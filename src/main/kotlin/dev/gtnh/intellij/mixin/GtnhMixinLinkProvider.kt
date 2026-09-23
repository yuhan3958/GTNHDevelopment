package dev.gtnh.intellij.mixin

import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression

interface GtnhMixinLinkProvider {
    fun findMixinsControlledBy(
        field: PsiField,
        references: Collection<PsiReferenceExpression>
    ): List<GtnhMixinLink>
}
