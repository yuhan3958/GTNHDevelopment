package dev.gtnh.intellij.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.ecosystem.GtnhDependency
import dev.gtnh.intellij.ecosystem.GtnhDependencyService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.concurrent.Callable
import javax.swing.JPanel

class GtnhOverviewPanel(private val project: Project) : JBPanel<GtnhOverviewPanel>(BorderLayout()) {
    private val list = JPanel(GridLayout(0, 1, 4, 4))

    init {
        add(JBLabel("GTNH ecosystem dependencies"), BorderLayout.NORTH)
        add(list, BorderLayout.CENTER)
        ReadAction.nonBlocking(Callable { GtnhDependencyService.getInstance(project).dependencies() })
            .expireWith(project)
            .finishOnUiThread(ModalityState.nonModal(), ::showDependencies)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showDependencies(dependencies: List<GtnhDependency>) {
        list.removeAll()
        dependencies.groupBy(GtnhDependency::moduleName).forEach { (module, items) ->
            list.add(JBLabel(module))
            items.forEach { dependency ->
                list.add(JBLabel("  ${dependency.kind.displayName}: ${dependency.coordinates}:${dependency.version ?: "unknown"}"))
            }
        }
        if (dependencies.isEmpty()) list.add(JBLabel("No GTNH ecosystem dependencies detected"))
        revalidate()
        repaint()
    }
}
