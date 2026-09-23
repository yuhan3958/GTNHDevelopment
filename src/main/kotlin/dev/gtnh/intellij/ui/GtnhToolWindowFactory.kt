package dev.gtnh.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

class GtnhToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane().apply {
            addTab("Run", GtnhRunPanel(project))
            addTab("Environment", GtnhEnvironmentPanel(project))
        }
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(tabs, "", false))
    }
}
