package dev.gtnh.intellij.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import dev.gtnh.intellij.patch.GtnhPatch
import dev.gtnh.intellij.patch.GtnhPatchConfidence
import javax.swing.JList

object GtnhPatchImpactPopup {
    fun show(patches: List<GtnhPatch>) {
        JBPopupFactory.getInstance().createPopupChooserBuilder(patches)
            .setTitle("GTNH Patch Impact")
            .setRenderer(object : ColoredListCellRenderer<GtnhPatch>() {
                override fun customizeCellRenderer(
                    list: JList<out GtnhPatch>, value: GtnhPatch, index: Int, selected: Boolean, hasFocus: Boolean
                ) {
                    val confidence = if (value.confidence == GtnhPatchConfidence.EXACT) "Exact" else "Heuristic"
                    append("[${confidence}] ${value.kind.displayName}")
                    value.detail?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
                    if (value.controllingConfigs.isNotEmpty()) {
                        append("  Config: ${value.controllingConfigs.joinToString()}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
            })
            .setItemChosenCallback { it.source.navigate(true) }
            .createPopup().showInFocusCenter()
    }
}
