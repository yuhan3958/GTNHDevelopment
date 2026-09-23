package dev.gtnh.intellij.contribution

data class GtnhPrDraftInput(
    val summary: String,
    val changes: List<String>,
    val observedChecks: List<Pair<String, Boolean>> = emptyList(),
    val issueNumber: Int? = null,
    val closeIssue: Boolean = false,
    val confirmations: Set<GtnhHumanConfirmation> = emptySet()
)

object GtnhPrDraftService {
    fun generate(input: GtnhPrDraftInput): String = buildString {
        appendLine("## Summary")
        appendLine()
        appendLine(input.summary.ifBlank { "<!-- Add a concise summary. -->" })
        appendLine()
        appendLine("## Changes")
        appendLine()
        if (input.changes.isEmpty()) appendLine("- No change summary available.")
        else input.changes.forEach { appendLine("- $it") }
        appendLine()
        appendLine("## Testing")
        appendLine()
        if (input.observedChecks.isEmpty()) appendLine("- Not run.")
        else input.observedChecks.forEach { (name, passed) -> appendLine("- [${if (passed) "x" else " "}] $name") }
        appendLine()
        appendLine("## Related Issues")
        appendLine()
        appendLine(input.issueNumber?.let { if (input.closeIssue) "Fixes #$it" else "Related to #$it" }
            ?: "- None specified.")
        appendLine()
        appendLine("## Checklist")
        appendLine()
        checklist("DevEnv tested", GtnhHumanConfirmation.DEV_ENV_TESTED, input.confirmations)
        checklist("Fullpack tested", GtnhHumanConfirmation.FULLPACK_TESTED, input.confirmations)
        checklist("AI policy acknowledged", GtnhHumanConfirmation.AI_POLICY_ACKNOWLEDGED, input.confirmations)
    }.trimEnd()

    private fun StringBuilder.checklist(
        label: String,
        item: GtnhHumanConfirmation,
        confirmations: Set<GtnhHumanConfirmation>
    ) = appendLine("- [${if (item in confirmations) "x" else " "}] $label")
}
