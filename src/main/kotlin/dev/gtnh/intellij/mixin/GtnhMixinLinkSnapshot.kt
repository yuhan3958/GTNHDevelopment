package dev.gtnh.intellij.mixin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.SmartPsiElementPointer

data class GtnhMixinLinkSnapshot(
    val configFields: List<SmartPsiElementPointer<PsiField>>,
    val mixinClass: SmartPsiElementPointer<PsiClass>,
    val kind: GtnhMixinLinkKind
)
