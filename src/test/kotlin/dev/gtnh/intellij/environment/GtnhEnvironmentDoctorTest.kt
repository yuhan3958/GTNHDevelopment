package dev.gtnh.intellij.environment

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class GtnhEnvironmentDoctorTest : LightJavaCodeInsightFixtureTestCase() {
    fun testKeepsOrderAndIsolatesFailedChecks() {
        val doctor = GtnhEnvironmentDoctor(project, listOf(
            GtnhEnvironmentCheck { GtnhEnvironmentResult("first", "First", GtnhCheckStatus.PASS, "ok") },
            GtnhEnvironmentCheck { error("unavailable") },
            GtnhEnvironmentCheck { GtnhEnvironmentResult("last", "Last", GtnhCheckStatus.WARNING, "warn") }
        ))
        val results = doctor.diagnose()
        assertEquals(3, results.size)
        assertEquals(GtnhCheckStatus.UNKNOWN, results[1].status)
        assertEquals("last", results[2].id)
    }

    fun testCancellationPropagates() {
        val doctor = GtnhEnvironmentDoctor(project, listOf(GtnhEnvironmentCheck { throw ProcessCanceledException() }))
        assertThrows(ProcessCanceledException::class.java) { doctor.diagnose() }
    }
}
