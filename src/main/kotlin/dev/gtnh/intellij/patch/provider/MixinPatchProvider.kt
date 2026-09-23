package dev.gtnh.intellij.patch.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import dev.gtnh.intellij.mixin.GtnhMixinLinkService
import dev.gtnh.intellij.mixin.GtnhMixinUtil
import dev.gtnh.intellij.patch.GtnhPatch
import dev.gtnh.intellij.patch.GtnhPatchConfidence
import dev.gtnh.intellij.patch.GtnhPatchKind
import dev.gtnh.intellij.patch.GtnhPatchProvider
import dev.gtnh.intellij.patch.GtnhPatchTarget

class MixinPatchProvider(private val project: Project) : GtnhPatchProvider {
    override fun findPatches(target: GtnhPatchTarget): List<GtnhPatch> {
        val mixins = LinkedHashSet<PsiClass>()
        ReferencesSearch.search(target.targetClass, GlobalSearchScope.projectScope(project)).forEach { reference ->
            ProgressManager.checkCanceled()
            val annotation = PsiTreeUtil.getParentOfType(reference.element, PsiAnnotation::class.java, false)
            if (annotation?.qualifiedName == GtnhMixinUtil.MIXIN_ANNOTATION) {
                PsiTreeUtil.getParentOfType(annotation, PsiClass::class.java, true)?.let(mixins::add)
            }
            true
        }
        return mixins.flatMap { mixin -> patchesIn(mixin, target) }
    }

    private fun patchesIn(mixin: PsiClass, target: GtnhPatchTarget): List<GtnhPatch> {
        val configs = GtnhMixinLinkService.getInstance(project).findConfigsControllingMixin(mixin)
            .flatMap { it.configFields }
            .map { field -> "${field.containingClass?.qualifiedName ?: field.containingClass?.name}.${field.name}" }
            .distinct()
        return mixin.methods.flatMap { method ->
            method.modifierList.annotations.mapNotNull { annotation ->
                val kind = kind(annotation.qualifiedName, method) ?: return@mapNotNull null
                val selector = selector(annotation, method)
                if (!matchesTarget(selector, method, target)) return@mapNotNull null
                val source = (annotation as? Navigatable) ?: (method as? Navigatable) ?: return@mapNotNull null
                GtnhPatch(target, source, kind, GtnhPatchConfidence.EXACT, selector, configs)
            }
        }
    }

    private fun matchesTarget(selector: String?, method: PsiMethod, target: GtnhPatchTarget): Boolean {
        val targetMethod = target.targetMethod ?: return true
        if (method.hasAnnotation(OVERWRITE) || method.hasAnnotation(SHADOW)) return method.name == targetMethod.name
        return selector?.substringBefore('(')?.substringBefore(';')?.substringAfterLast('/') == targetMethod.name
    }

    private fun selector(annotation: PsiAnnotation, method: PsiMethod): String? {
        if (annotation.qualifiedName == OVERWRITE || annotation.qualifiedName == SHADOW) return method.name
        val value = annotation.findAttributeValue("method") ?: return null
        return (value as? PsiLiteralExpression)?.value as? String ?: value.text.trim('"')
    }

    private fun kind(name: String?, method: PsiMethod): GtnhPatchKind? = when (name) {
        INJECT -> GtnhPatchKind.MIXIN_INJECT
        REDIRECT -> GtnhPatchKind.MIXIN_REDIRECT
        MODIFY_CONSTANT -> GtnhPatchKind.MIXIN_MODIFY_CONSTANT
        MODIFY_VARIABLE -> GtnhPatchKind.MIXIN_MODIFY_VARIABLE
        OVERWRITE -> GtnhPatchKind.MIXIN_OVERWRITE
        SHADOW -> GtnhPatchKind.MIXIN_SHADOW
        else -> name?.takeIf { it.startsWith("com.llamalad7.mixinextras.injector.") }
            ?.let { GtnhPatchKind.MIXIN_EXTRAS }
    }

    private fun PsiMethod.hasAnnotation(fqn: String): Boolean = modifierList.findAnnotation(fqn) != null

    companion object {
        private const val INJECT = "org.spongepowered.asm.mixin.injection.Inject"
        private const val REDIRECT = "org.spongepowered.asm.mixin.injection.Redirect"
        private const val MODIFY_CONSTANT = "org.spongepowered.asm.mixin.injection.ModifyConstant"
        private const val MODIFY_VARIABLE = "org.spongepowered.asm.mixin.injection.ModifyVariable"
        private const val OVERWRITE = "org.spongepowered.asm.mixin.Overwrite"
        private const val SHADOW = "org.spongepowered.asm.mixin.Shadow"
    }
}
