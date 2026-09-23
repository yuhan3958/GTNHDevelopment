package dev.gtnh.intellij.mixin.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gtnh.intellij.mixin.GtnhMixinClassResolver
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkKind
import dev.gtnh.intellij.mixin.GtnhMixinLinkProvider
import dev.gtnh.intellij.mixin.GtnhMixinUtil

class BuilderMixinLinkProvider(
    private val resolver: GtnhMixinClassResolver
) : GtnhMixinLinkProvider {
    override fun findMixinsControlledBy(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink> = references.flatMap { reference ->
        ProgressManager.checkCanceled()
        val controlCall = GtnhMixinUtil.ancestors(reference, PsiMethodCallExpression::class.java).firstOrNull()
            ?: return@flatMap emptyList()
        if (!isControlArgument(reference, controlCall)) return@flatMap emptyList()

        val chainRoot = outermostChainCall(controlCall)
        val calls = flattenChain(chainRoot)
        if (calls.size < 2) return@flatMap emptyList()

        calls.asSequence()
            .filter { it !== controlCall && !GtnhMixinUtil.containsReferenceTo(it.argumentList, field) }
            .flatMap { call -> resolver.findMixinClasses(call.argumentList).asSequence().map { call to it } }
            .distinctBy { it.second.qualifiedName }
            .map { (registration, mixin) ->
                GtnhMixinLink(field, controlCall, registration, mixin, GtnhMixinLinkKind.BUILDER_REGISTRATION)
            }
            .toList()
    }

    private fun isControlArgument(reference: PsiReferenceExpression, call: PsiMethodCallExpression): Boolean {
        val lambda = GtnhMixinUtil.ancestors(reference, PsiLambdaExpression::class.java).firstOrNull()
        return lambda?.let { call.argumentList.textRange.contains(it.textRange) } == true ||
            call.argumentList.textRange.contains(reference.textRange)
    }

    private fun outermostChainCall(start: PsiMethodCallExpression): PsiMethodCallExpression {
        var current = start
        while (true) {
            val parent = skipParentheses(current.parent)
            val next = when {
                parent is PsiReferenceExpression && parent.qualifierExpression === current ->
                    skipParentheses(parent.parent) as? PsiMethodCallExpression
                else -> null
            } ?: return current
            current = next
        }
    }

    private fun flattenChain(root: PsiMethodCallExpression): List<PsiMethodCallExpression> {
        val calls = ArrayList<PsiMethodCallExpression>()
        var current: PsiMethodCallExpression? = root
        while (current != null) {
            calls += current
            current = current.methodExpression.qualifierExpression
                ?.let(::skipParentheses) as? PsiMethodCallExpression
        }
        return calls
    }

    private fun skipParentheses(element: PsiElement?): PsiElement? {
        var current = element
        while (current is PsiParenthesizedExpression) current = current.expression
        return current
    }
}
