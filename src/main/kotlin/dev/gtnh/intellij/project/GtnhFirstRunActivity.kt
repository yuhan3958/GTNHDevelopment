package dev.gtnh.intellij.project

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import dev.gtnh.intellij.settings.GtnhSettingsState

class GtnhFirstRunActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        if (!GtnhSettingsState.getInstance().state.firstRunNotifications) return
        val state = project.getService(GtnhFirstRunState::class.java)
        if (state.shown || !GtnhProjectDetector.isGtnhProject(project)) return
        state.shown = true
        NotificationGroupManager.getInstance().getNotificationGroup("GTNH Development")
            .createNotification("GTNH project detected", NotificationType.INFORMATION)
            .addAction(NotificationAction.createSimple("Open GTNH Tool Window") {
                ToolWindowManager.getInstance(project).getToolWindow("GTNH")?.show()
            })
            .addAction(NotificationAction.createSimple("Run Environment Check") {
                ToolWindowManager.getInstance(project).getToolWindow("GTNH")?.show()
            })
            .notify(project)
    }
}
