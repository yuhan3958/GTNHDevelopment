package dev.gtnh.intellij.project

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import dev.gtnh.intellij.settings.GtnhDetectionOverride
import dev.gtnh.intellij.settings.GtnhProjectSettingsState
import dev.gtnh.intellij.settings.GtnhSettingsState

object GtnhProjectDetector {

    private val markers = listOf(
        "com.gtnewhorizons.gtnhconvention",
        "com.gtnewhorizons.retrofuturagradle",
        "GTNHGradle"
    )

    fun isGtnhProject(project: Project): Boolean {
        when (project.getService(GtnhProjectSettingsState::class.java).detectionOverride) {
            GtnhDetectionOverride.FORCE_ON -> return true
            GtnhDetectionOverride.FORCE_OFF -> return false
            GtnhDetectionOverride.AUTO -> Unit
        }
        if (!GtnhSettingsState.getInstance().state.automaticDetection) return false
        val basePath = project.basePath ?: return false

        val candidates = listOf(
            "$basePath/build.gradle.kts",
            "$basePath/build.gradle"
        )

        return candidates.any { path ->
            val file = LocalFileSystem.getInstance()
                .findFileByPath(path)
                ?: return@any false

            ReadAction.compute<Boolean, RuntimeException> {
                val text = VfsUtilCore.loadText(file)

                markers.any { marker ->
                    text.contains(marker)
                }
            }
        }
    }
}
