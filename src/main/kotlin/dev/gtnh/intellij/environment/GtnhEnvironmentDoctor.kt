package dev.gtnh.intellij.environment

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import dev.gtnh.intellij.environment.check.GtnhProjectCheck
import dev.gtnh.intellij.environment.check.GradleTasksCheck
import dev.gtnh.intellij.environment.check.GradleWrapperCheck
import dev.gtnh.intellij.environment.check.ProjectSdkCheck

@Service(Service.Level.PROJECT)
class GtnhEnvironmentDoctor(
    private val project: Project,
    private val checks: List<GtnhEnvironmentCheck> = defaultChecks()
) {
    fun diagnose(): List<GtnhEnvironmentResult> = checks.map { check ->
        ProgressManager.checkCanceled()
        try {
            check.run(project)
        } catch (canceled: ProcessCanceledException) {
            throw canceled
        } catch (failure: RuntimeException) {
            LOG.debug("GTNH environment check failed", failure)
            GtnhEnvironmentResult(check.javaClass.name, "Environment check", GtnhCheckStatus.UNKNOWN,
                failure.message ?: "Unavailable")
        }
    }.toList()

    companion object {
        private val LOG = Logger.getInstance(GtnhEnvironmentDoctor::class.java)
        fun getInstance(project: Project): GtnhEnvironmentDoctor = project.getService(GtnhEnvironmentDoctor::class.java)
        private fun defaultChecks(): List<GtnhEnvironmentCheck> = listOf(
            GtnhProjectCheck(), ProjectSdkCheck(), GradleWrapperCheck(), GradleTasksCheck()
        )
    }
}
