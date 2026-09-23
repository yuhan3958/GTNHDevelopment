package dev.gtnh.intellij.mixin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField

data class GtnhMixinLink(
    val configField: PsiField,
    val controlExpression: PsiElement?,
    val registrationElement: PsiElement?,
    val mixinClass: PsiClass,
    val kind: GtnhMixinLinkKind
)

enum class GtnhMixinLinkKind(val displayName: String) {
    DIRECT("Direct"),
    CONDITIONAL_REGISTRATION("Conditional"),
    BUILDER_REGISTRATION("Builder"),
    UNKNOWN("Unknown")
}
