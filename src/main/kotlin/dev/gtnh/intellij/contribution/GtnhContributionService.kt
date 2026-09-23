package dev.gtnh.intellij.contribution

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import dev.gtnh.intellij.compat.GitStateAdapter
import dev.gtnh.intellij.gradle.GtnhGradleTask
import dev.gtnh.intellij.gradle.GtnhGradleTaskGroup
import dev.gtnh.intellij.gradle.GtnhGradleTaskService

@Service(Service.Level.PROJECT)
class GtnhContributionService(private val project: Project) {
    fun gitState(): GtnhGitState = GitStateAdapter(project).read()

    fun availableChecks(): List<GtnhGradleTask> = GtnhGradleTaskService.getInstance(project).discoverTasks()
        .filter { it.group == GtnhGradleTaskGroup.BUILD || it.group == GtnhGradleTaskGroup.CHECK ||
            it.group == GtnhGradleTaskGroup.FORMAT }

    companion object {
        fun getInstance(project: Project): GtnhContributionService = project.getService(GtnhContributionService::class.java)
    }
}
