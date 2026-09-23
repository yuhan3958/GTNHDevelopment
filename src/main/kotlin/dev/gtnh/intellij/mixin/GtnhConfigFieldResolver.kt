package dev.gtnh.intellij.mixin

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

object GtnhConfigFieldResolver {
    fun findFields(root: PsiElement?): List<PsiField> {
        if (root == null) return emptyList()
        return (sequenceOf(root as? PsiReferenceExpression) +
            PsiTreeUtil.findChildrenOfType(root, PsiReferenceExpression::class.java).asSequence())
            .filterNotNull()
            .mapNotNull {
                ProgressManager.checkCanceled()
                it.resolve() as? PsiField
            }
            .distinctBy { it.containingClass?.qualifiedName to it.name }
            .toList()
    }
}
