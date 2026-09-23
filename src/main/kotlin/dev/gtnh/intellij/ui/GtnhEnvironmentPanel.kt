package dev.gtnh.intellij.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.environment.GtnhCheckStatus
import dev.gtnh.intellij.environment.GtnhEnvironmentDoctor
import dev.gtnh.intellij.environment.GtnhEnvironmentResult
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.concurrent.Callable
import javax.swing.JButton
import javax.swing.JPanel

class GtnhEnvironmentPanel(private val project: Project) : JBPanel<GtnhEnvironmentPanel>(BorderLayout()) {
    private val results = JPanel(GridLayout(0, 1, 4, 4))

    init {
        add(JButton("Refresh").apply { addActionListener { refresh() } }, BorderLayout.NORTH)
        add(results, BorderLayout.CENTER)
        refresh()
    }

    private fun refresh() {
        ReadAction.nonBlocking(Callable { GtnhEnvironmentDoctor.getInstance(project).diagnose() })
            .expireWith(project)
            .finishOnUiThread(ModalityState.nonModal(), ::showResults)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showResults(items: List<GtnhEnvironmentResult>) {
        results.removeAll()
        items.forEach { item ->
            val marker = when (item.status) {
                GtnhCheckStatus.PASS -> "OK"
                GtnhCheckStatus.WARNING -> "Warning"
                GtnhCheckStatus.ERROR -> "Error"
                GtnhCheckStatus.UNKNOWN -> "Unknown"
            }
            results.add(JBLabel("[$marker] ${item.label}: ${item.evidence}"))
        }
        revalidate()
        repaint()
    }
}
