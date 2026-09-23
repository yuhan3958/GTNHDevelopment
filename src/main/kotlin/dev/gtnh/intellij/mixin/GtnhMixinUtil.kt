package dev.gtnh.intellij.mixin

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

object GtnhMixinUtil {
    const val MIXIN_ANNOTATION = "org.spongepowered.asm.mixin.Mixin"

    fun isMixinClass(psiClass: PsiClass): Boolean =
        psiClass.modifierList?.annotations?.any(::isMixinAnnotation) == true

    private fun isMixinAnnotation(annotation: PsiAnnotation): Boolean =
        annotation.resolveAnnotationType()?.qualifiedName == MIXIN_ANNOTATION

    fun containsReferenceTo(root: PsiElement?, field: PsiField): Boolean {
        if (root == null) return false
        return PsiTreeUtil.findChildrenOfType(root, PsiReferenceExpression::class.java).any {
            ProgressManager.checkCanceled()
            it.resolve()?.isEquivalentTo(field) == true
        }
    }

    fun <T : PsiElement> ancestors(element: PsiElement, type: Class<T>): Sequence<T> = sequence {
        var current: PsiElement? = element
        while (current != null) {
            ProgressManager.checkCanceled()
            if (type.isInstance(current)) yield(type.cast(current))
            current = current.parent
        }
    }
}
