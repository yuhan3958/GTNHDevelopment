package dev.gtnh.intellij.environment

enum class GtnhCheckStatus { PASS, WARNING, ERROR, UNKNOWN }

data class GtnhEnvironmentResult(
    val id: String,
    val label: String,
    val status: GtnhCheckStatus,
    val evidence: String
)
