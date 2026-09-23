package dev.gtnh.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import dev.gtnh.intellij.settings.GtnhSettingsState

class GtnhToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane().apply {
            val settings = GtnhSettingsState.getInstance().state
            addTab("Overview", GtnhOverviewPanel(project))
            addTab("Run", GtnhRunPanel(project))
            addTab("Code", GtnhCodePanel())
            if (settings.environmentDoctor) addTab("Environment", GtnhEnvironmentPanel(project))
            if (settings.prAssistant) addTab("Contribution", GtnhContributionPanel(project))
        }
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(tabs, "", false))
    }
}
