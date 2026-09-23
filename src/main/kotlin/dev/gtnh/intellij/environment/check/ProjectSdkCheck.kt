package dev.gtnh.intellij.environment.check

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import dev.gtnh.intellij.environment.GtnhCheckStatus
import dev.gtnh.intellij.environment.GtnhEnvironmentCheck
import dev.gtnh.intellij.environment.GtnhEnvironmentResult

class ProjectSdkCheck : GtnhEnvironmentCheck {
    override fun run(project: Project): GtnhEnvironmentResult {
        val sdk = ProjectRootManager.getInstance(project).projectSdk
            ?: return GtnhEnvironmentResult("java.projectSdk", "Project SDK", GtnhCheckStatus.ERROR, "No project SDK")
        return GtnhEnvironmentResult("java.projectSdk", "Project SDK", GtnhCheckStatus.PASS,
            "${sdk.name} (${sdk.versionString ?: "version unknown"})")
    }
}
