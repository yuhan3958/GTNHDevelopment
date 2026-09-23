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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.patch.GtnhPatchService
import dev.gtnh.intellij.patch.GtnhPatchTarget
import dev.gtnh.intellij.project.GtnhProjectDetector
import dev.gtnh.intellij.ui.GtnhPatchImpactPopup
import dev.gtnh.intellij.settings.GtnhSettingsState
import java.util.concurrent.Callable

class ShowGtnhPatchImpactAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (!GtnhSettingsState.getInstance().state.patchImpact) {
            event.presentation.isEnabledAndVisible = false
            return
        }
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.PSI_FILE)
        val target = if (editor != null && file != null) file.findElementAt(editor.caretModel.offset) else null
        event.presentation.isEnabledAndVisible = project != null && target != null &&
            PsiTreeUtil.getParentOfType(target, PsiClass::class.java, false) != null &&
            !DumbService.isDumb(project) && GtnhProjectDetector.isGtnhProject(project)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val targetClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) ?: return
        val targetMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)
        val pointers = SmartPointerManager.getInstance(project)
        val classPointer = pointers.createSmartPsiElementPointer(targetClass)
        val methodPointer = targetMethod?.let(pointers::createSmartPsiElementPointer)

        ReadAction.nonBlocking(Callable {
            val currentClass = classPointer.element ?: return@Callable emptyList()
            val target = GtnhPatchTarget(currentClass, methodPointer?.element)
            GtnhPatchService.getInstance(project).findPatches(target)
        }).inSmartMode(project).expireWith(project)
            .finishOnUiThread(ModalityState.nonModal()) { patches ->
                if (patches.isEmpty()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("GTNH Development")
                        .createNotification("No GTNH patch impact found", NotificationType.INFORMATION).notify(project)
                } else {
                    GtnhPatchImpactPopup.show(patches)
                }
            }.submit(AppExecutorUtil.getAppExecutorService())
    }
}
