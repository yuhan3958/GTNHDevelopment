package dev.gtnh.intellij.environment.check

import com.intellij.openapi.project.Project
import dev.gtnh.intellij.environment.GtnhCheckStatus
import dev.gtnh.intellij.environment.GtnhEnvironmentCheck
import dev.gtnh.intellij.environment.GtnhEnvironmentResult
import dev.gtnh.intellij.project.GtnhProjectDetector

class GtnhProjectCheck : GtnhEnvironmentCheck {
    override fun run(project: Project) = if (GtnhProjectDetector.isGtnhProject(project)) {
        GtnhEnvironmentResult("gtnh.project", "GTNH project", GtnhCheckStatus.PASS, "GTNH build markers detected")
    } else {
        GtnhEnvironmentResult("gtnh.project", "GTNH project", GtnhCheckStatus.WARNING, "No GTNH marker detected")
    }
}
