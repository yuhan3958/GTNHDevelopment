package dev.gtnh.intellij.project

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore

object GtnhProjectDetector {

    private val markers = listOf(
        "com.gtnewhorizons.gtnhconvention",
        "com.gtnewhorizons.retrofuturagradle",
        "GTNHGradle"
    )

    fun isGtnhProject(project: Project): Boolean {
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