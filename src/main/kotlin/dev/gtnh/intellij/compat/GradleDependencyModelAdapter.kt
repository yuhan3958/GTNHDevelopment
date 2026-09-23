package dev.gtnh.intellij.compat

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import dev.gtnh.intellij.ecosystem.GtnhDependencyCandidate

class GradleDependencyModelAdapter(private val project: Project) {
    fun dependencies(): List<GtnhDependencyCandidate> = ModuleManager.getInstance(project).modules.flatMap { module ->
        OrderEnumerator.orderEntries(module).librariesOnly().classes().roots.mapNotNull { root ->
            parse(root.nameWithoutExtension, module.name)
        }
    }.distinct()

    internal fun parse(name: String, moduleName: String): GtnhDependencyCandidate? {
        val normalized = name.removePrefix("Gradle: ").substringBefore(".jar")
        val parts = normalized.split(':')
        return when {
            parts.size >= 3 -> GtnhDependencyCandidate(parts[0], parts[1], parts[2], moduleName)
            parts.size == 2 -> GtnhDependencyCandidate(null, parts[0], parts[1], moduleName)
            normalized.isNotBlank() -> GtnhDependencyCandidate(null, normalized, null, moduleName)
            else -> null
        }
    }
}
