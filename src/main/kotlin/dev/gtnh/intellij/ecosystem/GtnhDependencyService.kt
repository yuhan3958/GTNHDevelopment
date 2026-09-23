package dev.gtnh.intellij.ecosystem

import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import dev.gtnh.intellij.compat.GradleDependencyModelAdapter
import dev.gtnh.intellij.ecosystem.provider.KnownGtnhDependencyProvider

@Service(Service.Level.PROJECT)
class GtnhDependencyService(
    private val project: Project,
    private val providers: List<GtnhDependencyProvider> = listOf(KnownGtnhDependencyProvider())
) {
    fun dependencies(): List<GtnhDependency> = GradleDependencyModelAdapter(project).dependencies().mapNotNull { candidate ->
        ProgressManager.checkCanceled()
        providers.firstNotNullOfOrNull { it.classify(candidate) }?.let { kind ->
            GtnhDependency(candidate.coordinates, candidate.version, candidate.moduleName, kind)
        }
    }.distinctBy { listOf(it.moduleName, it.coordinates, it.version, it.kind) }

    companion object {
        fun getInstance(project: Project): GtnhDependencyService = project.getService(GtnhDependencyService::class.java)
    }
}
