package dev.gtnh.intellij.settings

import com.intellij.openapi.options.Configurable
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class GtnhSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private val boxes = LinkedHashMap<String, JCheckBox>()

    override fun getDisplayName() = "GTNH Development"

    override fun createComponent(): JComponent = JPanel().also { root ->
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        panel = root
        add(root, "automaticDetection", "Automatic GTNH project detection")
        add(root, "gutterIcons", "Config/Mixin gutter icons")
        add(root, "mixinNavigation", "Config/Mixin navigation")
        add(root, "patchImpact", "Patch Impact Explorer")
        add(root, "patchHeuristics", "Heuristic ASM and Access Transformer findings")
        add(root, "environmentDoctor", "Environment Doctor")
        add(root, "prAssistant", "Pull Request Assistant")
        add(root, "firstRunNotifications", "First-run project notification")
        reset()
    }

    override fun isModified(): Boolean {
        val state = GtnhSettingsState.getInstance().state
        return boxes["automaticDetection"]?.isSelected != state.automaticDetection ||
            boxes["gutterIcons"]?.isSelected != state.gutterIcons || boxes["mixinNavigation"]?.isSelected != state.mixinNavigation ||
            boxes["patchImpact"]?.isSelected != state.patchImpact || boxes["patchHeuristics"]?.isSelected != state.patchHeuristics ||
            boxes["environmentDoctor"]?.isSelected != state.environmentDoctor || boxes["prAssistant"]?.isSelected != state.prAssistant ||
            boxes["firstRunNotifications"]?.isSelected != state.firstRunNotifications
    }

    override fun apply() {
        val state = GtnhSettingsState.getInstance().state
        state.automaticDetection = selected("automaticDetection")
        state.gutterIcons = selected("gutterIcons")
        state.mixinNavigation = selected("mixinNavigation")
        state.patchImpact = selected("patchImpact")
        state.patchHeuristics = selected("patchHeuristics")
        state.environmentDoctor = selected("environmentDoctor")
        state.prAssistant = selected("prAssistant")
        state.firstRunNotifications = selected("firstRunNotifications")
    }

    override fun reset() {
        val state = GtnhSettingsState.getInstance().state
        boxes["automaticDetection"]?.isSelected = state.automaticDetection
        boxes["gutterIcons"]?.isSelected = state.gutterIcons
        boxes["mixinNavigation"]?.isSelected = state.mixinNavigation
        boxes["patchImpact"]?.isSelected = state.patchImpact
        boxes["patchHeuristics"]?.isSelected = state.patchHeuristics
        boxes["environmentDoctor"]?.isSelected = state.environmentDoctor
        boxes["prAssistant"]?.isSelected = state.prAssistant
        boxes["firstRunNotifications"]?.isSelected = state.firstRunNotifications
    }

    override fun disposeUIResources() { panel = null; boxes.clear() }
    private fun add(panel: JPanel, key: String, label: String) { boxes[key] = JCheckBox(label).also(panel::add) }
    private fun selected(key: String) = boxes[key]?.isSelected == true
}
