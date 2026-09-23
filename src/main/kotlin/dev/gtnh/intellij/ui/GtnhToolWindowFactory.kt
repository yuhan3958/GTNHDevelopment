package dev.gtnh.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import dev.gtnh.intellij.gradle.GtnhGradleRunner
import dev.gtnh.intellij.project.GtnhProjectDetector
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel

class GtnhToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val panel = createPanel(project)

        val content = ContentFactory.getInstance()
            .createContent(panel, "", false)

        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project): JPanel {
        val root = JBPanel<JBPanel<*>>(BorderLayout())

        val isGtnh = GtnhProjectDetector.isGtnhProject(project)

        val status = JBLabel(
            if (isGtnh) {
                "✓ GTNH project detected"
            } else {
                "Not a GTNH project"
            }
        )

        root.add(status, BorderLayout.NORTH)

        val buttons = JPanel(
            GridLayout(
                0,
                1,
                4,
                4
            )
        )

        addTaskButton(
            buttons,
            project,
            "Run Client",
            "runClient"
        )

        addTaskButton(
            buttons,
            project,
            "Run Server",
            "runServer"
        )

        addTaskButton(
            buttons,
            project,
            "Run Server 17",
            "runServer17"
        )

        addTaskButton(
            buttons,
            project,
            "Run Server 21",
            "runServer21"
        )

        addTaskButton(
            buttons,
            project,
            "Run Server 25",
            "runServer25"
        )

        addTaskButton(
            buttons,
            project,
            "Build",
            "build"
        )

        addTaskButton(
            buttons,
            project,
            "Spotless Apply",
            "spotlessApply"
        )

        buttons.components.forEach {
            it.isEnabled = isGtnh
        }

        root.add(
            buttons,
            BorderLayout.CENTER
        )

        return root
    }

    private fun addTaskButton(
        panel: JPanel,
        project: Project,
        label: String,
        task: String
    ) {
        val button = JButton(label)

        button.addActionListener {
            GtnhGradleRunner.run(
                project,
                task
            )
        }

        panel.add(button)
    }
}