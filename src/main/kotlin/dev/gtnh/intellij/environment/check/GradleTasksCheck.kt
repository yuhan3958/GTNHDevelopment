package dev.gtnh.intellij.environment.check

import com.intellij.openapi.project.Project
import dev.gtnh.intellij.environment.GtnhCheckStatus
import dev.gtnh.intellij.environment.GtnhEnvironmentCheck
import dev.gtnh.intellij.environment.GtnhEnvironmentResult
import dev.gtnh.intellij.gradle.GtnhGradleTaskGroup
import dev.gtnh.intellij.gradle.GtnhGradleTaskService

class GradleTasksCheck : GtnhEnvironmentCheck {
    override fun run(project: Project): GtnhEnvironmentResult {
        val tasks = GtnhGradleTaskService.getInstance(project).discoverTasks()
        if (tasks.isEmpty()) return GtnhEnvironmentResult(
            "gradle.tasks", "Gradle tasks", GtnhCheckStatus.UNKNOWN, "No imported Gradle task model")
        val useful = tasks.filter { it.group != GtnhGradleTaskGroup.OTHER }.joinToString { it.path }
        return GtnhEnvironmentResult("gradle.tasks", "Gradle tasks", GtnhCheckStatus.PASS,
            useful.ifBlank { "Imported tasks available" })
    }
}
