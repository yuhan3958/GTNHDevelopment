package dev.gtnh.intellij.generation

enum class GtnhGenerationConfidence { HIGH, SAFE_STANDALONE }

data class GtnhGenerationPlan(
    val title: String,
    val changes: List<GtnhFileChange>,
    val confidence: GtnhGenerationConfidence
) {
    init { require(changes.isNotEmpty()) { "A generation plan must contain at least one change" } }
}

sealed interface GtnhFileChange {
    val displayPath: String

    data class CreateFile(override val displayPath: String, val content: String) : GtnhFileChange
}
