package dev.gtnh.intellij.mixin.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiReferenceExpression
import dev.gtnh.intellij.mixin.GtnhMixinClassResolver
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkKind
import dev.gtnh.intellij.mixin.GtnhMixinLinkProvider
import dev.gtnh.intellij.mixin.GtnhMixinUtil

class ConditionalMixinLinkProvider(
    private val resolver: GtnhMixinClassResolver
) : GtnhMixinLinkProvider {
    override fun findMixinsControlledBy(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink> = references.flatMap { reference ->
        ProgressManager.checkCanceled()
        findIfLink(field, reference) ?: findTernaryLink(field, reference).orEmpty()
    }

    private fun findIfLink(field: PsiField, reference: PsiReferenceExpression): List<GtnhMixinLink>? {
        val statement = GtnhMixinUtil.ancestors(reference, PsiIfStatement::class.java)
            .firstOrNull { GtnhMixinUtil.containsReferenceTo(it.condition, field) }
            ?: return null
        val branches = listOfNotNull(statement.thenBranch, statement.elseBranch)
        return links(field, statement.condition, statement, branches)
    }

    private fun findTernaryLink(field: PsiField, reference: PsiReferenceExpression): List<GtnhMixinLink>? {
        val expression = GtnhMixinUtil.ancestors(reference, PsiConditionalExpression::class.java)
            .firstOrNull { GtnhMixinUtil.containsReferenceTo(it.condition, field) }
            ?: return null
        val branches = listOfNotNull(expression.thenExpression, expression.elseExpression)
        return links(field, expression.condition, expression, branches)
    }

    private fun links(
        field: PsiField,
        control: PsiElement?,
        registration: PsiElement,
        branches: List<PsiElement>
    ): List<GtnhMixinLink> = branches.flatMap(resolver::findMixinClasses).distinct().map { mixin ->
        GtnhMixinLink(field, control, registration, mixin, GtnhMixinLinkKind.CONDITIONAL_REGISTRATION)
    }
}
