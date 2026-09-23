package dev.gtnh.intellij.mixin

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.PsiTreeUtil

class GtnhMixinClassResolver(private val project: Project) {
    private val scope = GlobalSearchScope.projectScope(project)

    fun findMixinClasses(root: PsiElement): List<PsiClass> {
        val result = LinkedHashSet<PsiClass>()

        PsiTreeUtil.findChildrenOfType(root, PsiClassObjectAccessExpression::class.java).forEach { literal ->
            ProgressManager.checkCanceled()
            PsiUtil.resolveClassInClassTypeOnly(literal.operand.type)
                ?.takeIf(GtnhMixinUtil::isMixinClass)
                ?.let(result::add)
        }

        PsiTreeUtil.findChildrenOfType(root, PsiReferenceExpression::class.java).forEach { reference ->
            ProgressManager.checkCanceled()
            (reference.resolve() as? PsiClass)?.takeIf(GtnhMixinUtil::isMixinClass)?.let(result::add)
        }

        PsiTreeUtil.findChildrenOfType(root, PsiLiteralExpression::class.java).forEach { literal ->
            ProgressManager.checkCanceled()
            val value = literal.value as? String ?: return@forEach
            resolveString(value, literal).forEach(result::add)
        }

        return result.toList()
    }

    fun resolveString(value: String, context: PsiElement): List<PsiClass> {
        val hint = value.trim().removeSuffix(".class")
        if (!looksLikeClassName(hint)) return emptyList()

        val facade = JavaPsiFacade.getInstance(project)
        val exactNames = linkedSetOf(hint)
        val packageName = (context.containingFile as? PsiJavaFile)?.packageName.orEmpty()
        if (packageName.isNotEmpty()) {
            exactNames += "$packageName.$hint"
            var prefix = packageName
            while ('.' in prefix) {
                prefix = prefix.substringBeforeLast('.')
                exactNames += "$prefix.$hint"
            }
        }

        val exact = exactNames.asSequence()
            .mapNotNull { facade.findClass(it, scope) }
            .filter(GtnhMixinUtil::isMixinClass)
            .distinctBy { it.qualifiedName }
            .toList()
        if (exact.isNotEmpty()) return preferLocal(exact, context)

        val simpleName = hint.substringAfterLast('.')
        val candidates = PsiShortNamesCache.getInstance(project)
            .getClassesByName(simpleName, scope)
            .filter(GtnhMixinUtil::isMixinClass)
            .distinctBy { it.qualifiedName }
        return preferLocal(candidates, context)
    }

    private fun preferLocal(classes: List<PsiClass>, context: PsiElement): List<PsiClass> {
        if (classes.size < 2) return classes
        val module = ModuleUtilCore.findModuleForPsiElement(context)
        val sameModule = classes.filter { ModuleUtilCore.findModuleForPsiElement(it) == module }
        return sameModule.ifEmpty { classes }
    }

    private fun looksLikeClassName(value: String): Boolean {
        if (value.isBlank() || value.any(Char::isWhitespace)) return false
        val simpleName = value.substringAfterLast('.').substringAfterLast('$')
        return simpleName.firstOrNull()?.isUpperCase() == true
    }
}
