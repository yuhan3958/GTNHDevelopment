package dev.gtnh.intellij.gradle

data class GtnhGradleTask(
    val path: String,
    val displayName: String,
    val externalProjectPath: String,
    val group: GtnhGradleTaskGroup
)

enum class GtnhGradleTaskGroup { CLIENT, SERVER, BUILD, CHECK, FORMAT, OTHER }
