package dev.gtnh.intellij.mixin

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch

class GtnhMixinRegistrationFinder(private val project: Project) {
    private val scope = GlobalSearchScope.projectScope(project)
    private val resolver = GtnhMixinClassResolver(project)

    fun findRegistrations(mixinClass: PsiClass): List<PsiElement> {
        val result = LinkedHashSet<PsiElement>()
        ReferencesSearch.search(mixinClass, scope).forEach { reference ->
            ProgressManager.checkCanceled()
            result += reference.element
            true
        }

        val names = listOfNotNull(mixinClass.name, mixinClass.qualifiedName).toCollection(LinkedHashSet())
        names.forEach { name ->
            PsiSearchHelper.getInstance(project).processElementsWithWord(
                { element, _ ->
                    ProgressManager.checkCanceled()
                    val literal = element.parent as? PsiLiteralExpression
                    if (literal != null && literal.value == name && resolvesTo(literal, mixinClass)) result += literal
                    true
                },
                scope,
                name,
                UsageSearchContext.IN_STRINGS,
                true
            )
        }
        return result.toList()
    }

    private fun resolvesTo(literal: PsiLiteralExpression, mixinClass: PsiClass): Boolean {
        val resolved = resolver.resolveString(literal.value as String, literal)
        return resolved.any { it.isEquivalentTo(mixinClass) }
    }
}
