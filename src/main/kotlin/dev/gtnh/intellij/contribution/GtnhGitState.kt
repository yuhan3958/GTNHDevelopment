package dev.gtnh.intellij.contribution

data class GtnhGitState(
    val available: Boolean,
    val branch: String?,
    val detached: Boolean,
    val changedPaths: List<String>,
    val remotes: List<String>,
    val upstream: String?,
    val ahead: Int?,
    val behind: Int?
)
