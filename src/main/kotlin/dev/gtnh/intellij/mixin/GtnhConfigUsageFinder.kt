package dev.gtnh.intellij.mixin

import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

object GtnhConfigUsageFinder {

    fun findUsages(
        project: Project,
        field: PsiField
    ): List<PsiReferenceExpression> {

        val scope = GlobalSearchScope.projectScope(project)

        val result = ArrayList<PsiReferenceExpression>()
        ReferencesSearch.search(field, scope).forEach { reference ->
            ProgressManager.checkCanceled()
            (reference.element as? PsiReferenceExpression)?.let(result::add)
            true
        }
        return result
    }
}
