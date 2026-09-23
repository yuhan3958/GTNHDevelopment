package dev.gtnh.intellij.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.concurrency.AppExecutorUtil
import dev.gtnh.intellij.contribution.GtnhContributionChecklistState
import dev.gtnh.intellij.contribution.GtnhContributionService
import dev.gtnh.intellij.contribution.GtnhGitState
import dev.gtnh.intellij.contribution.GtnhHumanConfirmation
import dev.gtnh.intellij.contribution.GtnhPrDraftInput
import dev.gtnh.intellij.contribution.GtnhPrDraftService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Callable
import javax.swing.JButton
import javax.swing.JPanel

class GtnhContributionPanel(private val project: Project) : JBPanel<GtnhContributionPanel>(BorderLayout()) {
    private val status = JBLabel("Loading Git state...")
    private val summary = JBTextField()
    private val issue = JBTextField()
    private val draft = JBTextArea(18, 60)
    private var gitState = GtnhGitState(false, null, false, emptyList(), emptyList(), null, null, null)
    private val checklist = project.getService(GtnhContributionChecklistState::class.java)

    init {
        val form = JPanel(GridLayout(0, 1, 4, 4))
        form.add(status)
        form.add(JBLabel("Summary"))
        form.add(summary)
        form.add(JBLabel("Related issue number (optional)"))
        form.add(issue)
        GtnhHumanConfirmation.entries.forEach { item ->
            form.add(JBCheckBox(label(item), checklist.isConfirmed(item)).apply {
                addActionListener { checklist.setConfirmed(item, isSelected) }
            })
        }
        form.add(JButton("Generate editable draft").apply { addActionListener { generate() } })
        form.add(JButton("Copy draft").apply {
            addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(draft.text)) }
        })
        add(form, BorderLayout.NORTH)
        add(JBScrollPane(draft), BorderLayout.CENTER)
        refresh()
    }

    private fun refresh() {
        ReadAction.nonBlocking(Callable { GtnhContributionService.getInstance(project).gitState() })
            .expireWith(project)
            .finishOnUiThread(ModalityState.nonModal()) { state ->
                gitState = state
                status.text = if (!state.available) "Git repository unavailable"
                else "Branch: ${state.branch ?: "detached HEAD"}; changed paths: ${state.changedPaths.size}"
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun generate() {
        val confirmed = GtnhHumanConfirmation.entries.filter(checklist::isConfirmed).toSet()
        draft.text = GtnhPrDraftService.generate(
            GtnhPrDraftInput(
                summary.text,
                gitState.changedPaths.map { "Update `$it`" },
                issueNumber = issue.text.trim().toIntOrNull(),
                confirmations = confirmed
            )
        )
    }

    private fun label(item: GtnhHumanConfirmation): String = when (item) {
        GtnhHumanConfirmation.DEV_ENV_TESTED -> "DevEnv tested"
        GtnhHumanConfirmation.FULLPACK_TESTED -> "Fullpack tested"
        GtnhHumanConfirmation.AI_POLICY_ACKNOWLEDGED -> "AI policy acknowledged"
    }
}
