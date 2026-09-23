package dev.gtnh.intellij.patch.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import dev.gtnh.intellij.patch.GtnhPatch
import dev.gtnh.intellij.patch.GtnhPatchConfidence
import dev.gtnh.intellij.patch.GtnhPatchKind
import dev.gtnh.intellij.patch.GtnhPatchProvider
import dev.gtnh.intellij.patch.GtnhPatchTarget

class AsmTransformerPatchProvider(private val project: Project) : GtnhPatchProvider {
    override fun findPatches(target: GtnhPatchTarget): List<GtnhPatch> {
        val result = LinkedHashSet<GtnhPatch>()
        val names = listOfNotNull(target.targetClass.qualifiedName, target.targetClass.name)
        names.forEach { name ->
            PsiSearchHelper.getInstance(project).processElementsWithWord(
                { element, _ ->
                    ProgressManager.checkCanceled()
                    val literal = element.parent as? PsiLiteralExpression
                    val owner = literal?.let { PsiTreeUtil.getParentOfType(it, PsiClass::class.java, false) }
                    if (literal?.value == name && owner != null && isTransformer(owner)) {
                        val source = (literal as? Navigatable) ?: return@processElementsWithWord true
                        result += GtnhPatch(target, source, GtnhPatchKind.ASM_TRANSFORMER,
                            GtnhPatchConfidence.HEURISTIC, name)
                    }
                    true
                },
                GlobalSearchScope.projectScope(project),
                name,
                UsageSearchContext.IN_STRINGS,
                true
            )
        }
        return result.toList()
    }

    private fun isTransformer(owner: PsiClass): Boolean {
        val transformerType = owner.implementsListTypes.any { it.canonicalText.endsWith("IClassTransformer") }
        val transformMethod = owner.findMethodsByName("transform", true).isNotEmpty()
        val asmReference = owner.containingFile.text.contains("org.objectweb.asm")
        return transformerType || (transformMethod && asmReference)
    }
}
