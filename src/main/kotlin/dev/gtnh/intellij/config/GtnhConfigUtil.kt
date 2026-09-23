package dev.gtnh.intellij.config

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil

object GtnhConfigUtil {
    fun findFieldAtCaret(editor: Editor, file: PsiFile): PsiField? {
        val offset = editor.caretModel.offset.coerceAtMost(file.textLength)
        val element = file.findElementAt(offset)
            ?: file.findElementAt((offset - 1).coerceAtLeast(0))
            ?: return null

        PsiTreeUtil.getParentOfType(element, PsiField::class.java, false)?.let { field ->
            if (field.nameIdentifier?.textRange?.containsOffset(offset) == true) return field
        }

        return PsiTreeUtil.getParentOfType(element, PsiReferenceExpression::class.java, false)
            ?.resolve() as? PsiField
    }

    fun isLikelyConfigField(field: PsiField): Boolean {
        val className = field.containingClass?.name.orEmpty()
        val nameSuggestsConfig = listOf("Config", "Configuration", "Settings", "Options")
            .any { className.contains(it, ignoreCase = true) }
        val hasAnnotation = field.modifierList?.annotations?.isNotEmpty() == true
        return (field.hasModifierProperty("public") && field.hasModifierProperty("static")) ||
            hasAnnotation || nameSuggestsConfig
    }
}
