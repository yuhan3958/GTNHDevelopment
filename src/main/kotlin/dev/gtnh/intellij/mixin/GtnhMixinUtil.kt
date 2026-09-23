package dev.gtnh.intellij.mixin

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

object GtnhMixinUtil {
    const val MIXIN_ANNOTATION = "org.spongepowered.asm.mixin.Mixin"

    fun isMixinClass(psiClass: PsiClass): Boolean =
        psiClass.modifierList?.annotations?.any(::isMixinAnnotation) == true

    fun findMixinClassAtCaret(editor: Editor, file: PsiFile): PsiClass? {
        val offset = editor.caretModel.offset.coerceAtMost(file.textLength)
        val element = file.findElementAt(offset)
            ?: file.findElementAt((offset - 1).coerceAtLeast(0))
            ?: return null
        return PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false)?.takeIf(::isMixinClass)
    }

    private fun isMixinAnnotation(annotation: PsiAnnotation): Boolean =
        annotation.resolveAnnotationType()?.qualifiedName == MIXIN_ANNOTATION

    fun containsReferenceTo(root: PsiElement?, field: PsiField): Boolean {
        if (root == null) return false
        val references = sequenceOf(root as? PsiReferenceExpression) +
            PsiTreeUtil.findChildrenOfType(root, PsiReferenceExpression::class.java).asSequence()
        return references.filterNotNull().any {
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
