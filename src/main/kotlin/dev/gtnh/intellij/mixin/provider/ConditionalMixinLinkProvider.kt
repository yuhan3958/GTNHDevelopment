package dev.gtnh.intellij.mixin.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiReferenceExpression
import dev.gtnh.intellij.mixin.GtnhMixinClassResolver
import dev.gtnh.intellij.mixin.GtnhConfigFieldResolver
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkKind
import dev.gtnh.intellij.mixin.GtnhMixinLinkProvider
import dev.gtnh.intellij.mixin.GtnhMixinUtil

class ConditionalMixinLinkProvider(
    private val resolver: GtnhMixinClassResolver
) : GtnhMixinLinkProvider {
    override fun findByConfig(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink> = references.flatMap { reference ->
        ProgressManager.checkCanceled()
        findIfLink(field, reference) ?: findTernaryLink(field, reference).orEmpty()
    }

    override fun findByMixin(
        mixinClass: PsiClass,
        registrations: Collection<PsiElement>
    ): List<GtnhMixinLink> = registrations.flatMap { registration ->
        ProgressManager.checkCanceled()
        findIfLinks(mixinClass, registration) ?: findTernaryLinks(mixinClass, registration).orEmpty()
    }

    private fun findIfLinks(mixinClass: PsiClass, registration: PsiElement): List<GtnhMixinLink>? {
        val statement = GtnhMixinUtil.ancestors(registration, PsiIfStatement::class.java).firstOrNull { candidate ->
            listOfNotNull(candidate.thenBranch, candidate.elseBranch).any { it.textRange.contains(registration.textRange) }
        } ?: return null
        return reverseLinks(mixinClass, statement.condition, registration, statement)
    }

    private fun findTernaryLinks(mixinClass: PsiClass, registration: PsiElement): List<GtnhMixinLink>? {
        val expression = GtnhMixinUtil.ancestors(registration, PsiConditionalExpression::class.java).firstOrNull { candidate ->
            listOfNotNull(candidate.thenExpression, candidate.elseExpression).any { it.textRange.contains(registration.textRange) }
        } ?: return null
        return reverseLinks(mixinClass, expression.condition, registration, expression)
    }

    private fun reverseLinks(
        mixinClass: PsiClass,
        control: PsiElement?,
        registration: PsiElement,
        owner: PsiElement
    ): List<GtnhMixinLink> = GtnhConfigFieldResolver.findFields(control).takeIf { it.isNotEmpty() }
        ?.let { fields ->
            listOf(GtnhMixinLink(fields, control, owner.takeIf { it.isValid } ?: registration, mixinClass,
                GtnhMixinLinkKind.CONDITIONAL_REGISTRATION))
        }.orEmpty()

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
    ): List<GtnhMixinLink> {
        val fields = GtnhConfigFieldResolver.findFields(control).ifEmpty { listOf(field) }
        return branches.flatMap(resolver::findMixinClasses).distinct().map { mixin ->
            GtnhMixinLink(fields, control, registration, mixin, GtnhMixinLinkKind.CONDITIONAL_REGISTRATION)
        }
    }
}
