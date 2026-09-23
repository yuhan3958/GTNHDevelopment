package dev.gtnh.intellij.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.gradle.GtnhGradleRunner
import dev.gtnh.intellij.gradle.GtnhGradleTask
import dev.gtnh.intellij.gradle.GtnhGradleTaskGroup
import dev.gtnh.intellij.gradle.GtnhGradleTaskService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.concurrent.Callable
import javax.swing.JButton
import javax.swing.JPanel

class GtnhRunPanel(private val project: Project) : JBPanel<GtnhRunPanel>(BorderLayout()) {
    private val tasksPanel = JPanel(GridLayout(0, 1, 4, 4))

    init {
        add(JBLabel("Discovered GTNH Gradle tasks"), BorderLayout.NORTH)
        add(tasksPanel, BorderLayout.CENTER)
        refresh()
    }

    private fun refresh() {
        ReadAction.nonBlocking(Callable { GtnhGradleTaskService.getInstance(project).discoverTasks() })
            .expireWith(project)
            .finishOnUiThread(ModalityState.nonModal(), ::showTasks)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showTasks(tasks: List<GtnhGradleTask>) {
        tasksPanel.removeAll()
        tasks.filter { it.group != GtnhGradleTaskGroup.OTHER }.forEach { task ->
            tasksPanel.add(JButton(task.displayName).apply {
                toolTipText = task.path
                addActionListener { GtnhGradleRunner.run(project, task) }
            })
        }
        if (tasksPanel.componentCount == 0) tasksPanel.add(JBLabel("No relevant imported tasks"))
        revalidate()
        repaint()
    }
}
