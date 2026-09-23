package dev.gtnh.intellij.environment.check

import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import dev.gtnh.intellij.environment.GtnhCheckStatus
import dev.gtnh.intellij.environment.GtnhEnvironmentCheck
import dev.gtnh.intellij.environment.GtnhEnvironmentResult

class GradleWrapperCheck : GtnhEnvironmentCheck {
    override fun run(project: Project): GtnhEnvironmentResult {
        val base = project.basePath ?: return GtnhEnvironmentResult(
            "gradle.wrapper", "Gradle wrapper", GtnhCheckStatus.UNKNOWN, "Project path unavailable")
        val present = Files.isRegularFile(Path.of(base, "gradle", "wrapper", "gradle-wrapper.properties"))
        return GtnhEnvironmentResult("gradle.wrapper", "Gradle wrapper",
            if (present) GtnhCheckStatus.PASS else GtnhCheckStatus.WARNING,
            if (present) "gradle-wrapper.properties found" else "Gradle wrapper not found")
    }
}
