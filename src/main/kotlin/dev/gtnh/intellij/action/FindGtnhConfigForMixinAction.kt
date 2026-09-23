package dev.gtnh.intellij.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPointerManager
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.mixin.GtnhMixinLink
import dev.gtnh.intellij.mixin.GtnhMixinLinkService
import dev.gtnh.intellij.mixin.GtnhMixinUtil
import dev.gtnh.intellij.project.GtnhProjectDetector
import dev.gtnh.intellij.ui.GtnhConfigNavigationPopup
import java.util.concurrent.Callable

class FindGtnhConfigForMixinAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.PSI_FILE)
        val mixin = if (editor != null && file != null) GtnhMixinUtil.findMixinClassAtCaret(editor, file) else null
        event.presentation.isEnabledAndVisible = project != null && mixin != null &&
            !DumbService.isDumb(project) && GtnhProjectDetector.isGtnhProject(project)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (DumbService.isDumb(project)) return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val mixin = GtnhMixinUtil.findMixinClassAtCaret(editor, file) ?: return
        val pointer = SmartPointerManager.createPointer(mixin)
        val name = mixin.name ?: "Mixin"

        ReadAction.nonBlocking(Callable {
            val current = pointer.element ?: return@Callable emptyList<GtnhMixinLink>()
            GtnhMixinLinkService.getInstance(project).findConfigsControllingMixin(current)
        })
            .inSmartMode(project)
            .expireWith(project)
            .coalesceBy(this, pointer)
            .finishOnUiThread(ModalityState.nonModal()) { links -> showResult(project, name, links) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showResult(project: com.intellij.openapi.project.Project, name: String, links: List<GtnhMixinLink>) {
        val unique = links.flatMap(GtnhMixinLink::configFields)
            .distinctBy { it.containingClass?.qualifiedName to it.name }
        when (unique.size) {
            0 -> notify(project, "No controlling GTNH config found for $name")
            1 -> unique.single().navigate(true)
            else -> GtnhConfigNavigationPopup.show(links)
        }
    }

    private fun notify(project: com.intellij.openapi.project.Project, @NlsContexts.NotificationContent text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("GTNH Development")
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }
}
