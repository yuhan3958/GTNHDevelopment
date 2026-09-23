package dev.gtnh.intellij.patch

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

data class GtnhPatchTarget(
    val targetClass: PsiClass,
    val targetMethod: PsiMethod? = null
)
