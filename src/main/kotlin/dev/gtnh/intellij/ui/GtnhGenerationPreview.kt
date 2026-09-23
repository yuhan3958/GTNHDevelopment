package dev.gtnh.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.gtnh.intellij.generation.GtnhGenerationPlan
import dev.gtnh.intellij.generation.GtnhGenerationService
import java.awt.Dimension
import javax.swing.JComponent

class GtnhGenerationPreview(
    private val ownerProject: Project,
    private val plan: GtnhGenerationPlan
) : DialogWrapper(ownerProject) {
    init {
        title = plan.title
        setOKButtonText("Apply")
        init()
    }

    override fun createCenterPanel(): JComponent = JBScrollPane(JBTextArea(
        GtnhGenerationService(ownerProject).preview(plan), 30, 100
    ).apply { isEditable = false }).apply { preferredSize = Dimension(850, 560) }
}
