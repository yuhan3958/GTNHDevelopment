package dev.gtnh.intellij.mixin.provider

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gtnh.intellij.mixin.GtnhMixinClassResolver
import dev.gtnh.intellij.mixin.GtnhConfigFieldResolver
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkKind
import dev.gtnh.intellij.mixin.GtnhMixinLinkProvider
import dev.gtnh.intellij.mixin.GtnhMixinUtil

class DirectMixinLinkProvider(
    private val resolver: GtnhMixinClassResolver
) : GtnhMixinLinkProvider {
    override fun findByConfig(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink> = references.flatMap { reference ->
        ProgressManager.checkCanceled()
        val call = GtnhMixinUtil.ancestors(reference, PsiMethodCallExpression::class.java).firstOrNull()
            ?: return@flatMap emptyList()
        val lambda = GtnhMixinUtil.ancestors(reference, PsiLambdaExpression::class.java).firstOrNull()
        if (lambda == null || !call.argumentList.textRange.contains(lambda.textRange)) return@flatMap emptyList()

        resolver.findMixinClasses(call.argumentList)
            .map { mixin ->
                val fields = GtnhConfigFieldResolver.findFields(lambda).ifEmpty { listOf(field) }
                GtnhMixinLink(fields, lambda, call, mixin, GtnhMixinLinkKind.DIRECT)
            }
    }

    override fun findByMixin(
        mixinClass: PsiClass,
        registrations: Collection<PsiElement>
    ): List<GtnhMixinLink> = registrations.flatMap { registration ->
        ProgressManager.checkCanceled()
        val call = GtnhMixinUtil.ancestors(registration, PsiMethodCallExpression::class.java).firstOrNull()
            ?: return@flatMap emptyList()
        GtnhConfigFieldResolver.findFields(call.argumentList).takeIf { it.isNotEmpty() }
            ?.let { fields ->
                listOf(GtnhMixinLink(fields, call.argumentList, call, mixinClass, GtnhMixinLinkKind.DIRECT))
            }.orEmpty()
    }
}
