package dev.gtnh.intellij.gradle

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

object GtnhGradleRunner {

    fun run(project: Project, task: GtnhGradleTask) {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = task.externalProjectPath
            taskNames = listOf(task.path)
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }

        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID
        )
    }

    fun run(project: Project, taskName: String) {
        val basePath = project.basePath ?: return
        run(project, GtnhGradleTask(taskName, taskName, basePath, GtnhGradleTaskGroup.OTHER))
    }
}
