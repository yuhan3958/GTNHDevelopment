package dev.gtnh.intellij.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import dev.gtnh.intellij.settings.GtnhSettingsState
import java.awt.GridLayout

class GtnhCodePanel : JBPanel<GtnhCodePanel>(GridLayout(0, 1, 4, 4)) {
    init {
        accessibleContext.accessibleName = "GTNH code tools"
        val settings = GtnhSettingsState.getInstance().state
        if (settings.mixinNavigation) {
            add(JBLabel("Config/Mixin Links: use the editor context menu or gutter icons"))
        }
        if (settings.patchImpact) {
            add(JBLabel("Patch Impact: use Show GTNH Patch Impact in the editor"))
        }
        if (componentCount == 0) add(JBLabel("Code tools are disabled in settings"))
    }
}
