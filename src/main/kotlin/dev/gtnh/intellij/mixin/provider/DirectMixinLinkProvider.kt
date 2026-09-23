package dev.gtnh.intellij.mixin.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gtnh.intellij.mixin.GtnhMixinClassResolver
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkKind
import dev.gtnh.intellij.mixin.GtnhMixinLinkProvider
import dev.gtnh.intellij.mixin.GtnhMixinUtil

class DirectMixinLinkProvider(
    private val resolver: GtnhMixinClassResolver
) : GtnhMixinLinkProvider {
    override fun findMixinsControlledBy(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink> = references.flatMap { reference ->
        ProgressManager.checkCanceled()
        val call = GtnhMixinUtil.ancestors(reference, PsiMethodCallExpression::class.java).firstOrNull()
            ?: return@flatMap emptyList()
        val lambda = GtnhMixinUtil.ancestors(reference, PsiLambdaExpression::class.java).firstOrNull()
        if (lambda == null || !call.argumentList.textRange.contains(lambda.textRange)) return@flatMap emptyList()

        resolver.findMixinClasses(call.argumentList)
            .map { mixin -> GtnhMixinLink(field, lambda, call, mixin, GtnhMixinLinkKind.DIRECT) }
    }
}
