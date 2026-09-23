package dev.gtnh.intellij.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPointerManager
import dev.gtnh.intellij.config.GtnhConfigUtil
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkService
import dev.gtnh.intellij.project.GtnhProjectDetector
import dev.gtnh.intellij.ui.GtnhMixinNavigationPopup
import dev.gtnh.intellij.settings.GtnhSettingsState
import java.util.concurrent.Callable

class FindGtnhMixinsForConfigAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        if (!GtnhSettingsState.getInstance().state.mixinNavigation) {
            event.presentation.isEnabledAndVisible = false
            return
        }
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.PSI_FILE)
        val field = if (editor != null && file != null) GtnhConfigUtil.findFieldAtCaret(editor, file) else null
        val available = project != null && field != null && !DumbService.isDumb(project) &&
            GtnhProjectDetector.isGtnhProject(project)
        event.presentation.isEnabledAndVisible = available
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (DumbService.isDumb(project)) return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val field = GtnhConfigUtil.findFieldAtCaret(editor, file) ?: return
        val pointer = SmartPointerManager.createPointer(field)
        val fieldName = field.name

        ReadAction.nonBlocking(Callable {
            val currentField = pointer.element ?: return@Callable emptyList<GtnhMixinLink>()
            GtnhMixinLinkService.getInstance(project).findMixinsControlledBy(currentField)
        })
            .inSmartMode(project)
            .expireWith(project)
            .coalesceBy(this, pointer)
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.nonModal()) { links ->
                showResult(project, fieldName, links)
            }
            .submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService())
    }

    private fun showResult(project: com.intellij.openapi.project.Project, fieldName: String, links: List<GtnhMixinLink>) {
        val unique = links.distinctBy { it.mixinClass.qualifiedName ?: it.mixinClass }
        when (unique.size) {
            0 -> notify(project, "No GTNH Mixin registrations found for $fieldName")
            1 -> unique.single().mixinClass.navigate(true)
            else -> GtnhMixinNavigationPopup.show(links)
        }
    }

    private fun notify(project: com.intellij.openapi.project.Project, @NlsContexts.NotificationContent text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("GTNH Development")
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }
}
