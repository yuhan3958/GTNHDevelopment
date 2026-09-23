package dev.gtnh.intellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import dev.gtnh.intellij.generation.GtnhGenerationService
import dev.gtnh.intellij.generation.mixin.GtnhMixinGenerator
import dev.gtnh.intellij.project.GtnhProjectDetector
import dev.gtnh.intellij.ui.GtnhGenerationPreview

class NewGtnhMixinAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabledAndVisible = project != null &&
            event.getData(CommonDataKeys.PSI_FILE) is PsiJavaFile && GtnhProjectDetector.isGtnhProject(project)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val name = Messages.showInputDialog(project, "Mixin class name", "New GTNH Mixin", null) ?: return
        val target = Messages.showInputDialog(project, "Target class FQN", "New GTNH Mixin", null) ?: return
        applyPlan(project, runCatching { GtnhMixinGenerator.plan(file.packageName, name, target) }.getOrElse {
            Messages.showErrorDialog(project, it.message ?: "Invalid input", "GTNH Generation")
            return
        })
    }

    private fun applyPlan(project: com.intellij.openapi.project.Project, plan: dev.gtnh.intellij.generation.GtnhGenerationPlan) {
        val service = GtnhGenerationService(project)
        val errors = service.validate(plan)
        if (errors.isNotEmpty()) {
            Messages.showErrorDialog(project, errors.joinToString("\n"), "GTNH Generation")
        } else if (GtnhGenerationPreview(project, plan).showAndGet()) {
            service.apply(plan)
        }
    }
}
