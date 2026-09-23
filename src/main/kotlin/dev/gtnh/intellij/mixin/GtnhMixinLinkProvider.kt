package dev.gtnh.intellij.mixin

import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceExpression

interface GtnhMixinLinkProvider {
    fun findByConfig(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink>

    fun findByMixin(
        mixinClass: PsiClass,
        registrations: Collection<PsiElement>
    ): List<GtnhMixinLink> = emptyList()
}
