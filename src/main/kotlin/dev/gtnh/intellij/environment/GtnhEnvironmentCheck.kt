package dev.gtnh.intellij.environment

import com.intellij.openapi.project.Project

fun interface GtnhEnvironmentCheck {
    fun run(project: Project): GtnhEnvironmentResult
}
