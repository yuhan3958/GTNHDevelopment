package dev.gtnh.intellij.gradle

import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

@Service(Service.Level.PROJECT)
class GtnhGradleTaskService(private val project: Project) {
    fun discoverTasks(): List<GtnhGradleTask> = ProjectDataManager.getInstance()
        .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
        .flatMap { projectInfo ->
            val structure = projectInfo.externalProjectStructure ?: return@flatMap emptyList()
            ExternalSystemApiUtil.findAll(structure, ProjectKeys.TASK).map { node ->
                val task = node.data
                GtnhGradleTask(
                    task.name,
                    task.name.substringAfterLast(':').replaceFirstChar(Char::uppercase),
                    task.linkedExternalProjectPath,
                    classify(task.name)
                )
            }
        }
        .distinctBy { it.externalProjectPath to it.path }
        .sortedWith(compareBy<GtnhGradleTask> { it.group }.thenBy { it.path })

    private fun classify(path: String): GtnhGradleTaskGroup = when (path.substringAfterLast(':').lowercase()) {
        "runclient" -> GtnhGradleTaskGroup.CLIENT
        "runserver", "runserver17", "runserver21", "runserver25" -> GtnhGradleTaskGroup.SERVER
        "build", "assemble" -> GtnhGradleTaskGroup.BUILD
        "test", "check" -> GtnhGradleTaskGroup.CHECK
        "spotlessapply", "spotlesscheck" -> GtnhGradleTaskGroup.FORMAT
        else -> GtnhGradleTaskGroup.OTHER
    }

    companion object {
        fun getInstance(project: Project): GtnhGradleTaskService = project.getService(GtnhGradleTaskService::class.java)
    }
}
