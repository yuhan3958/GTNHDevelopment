package dev.gtnh.intellij.patch

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.pom.Navigatable
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class GtnhPatchServiceTest : LightJavaCodeInsightFixtureTestCase() {
    fun testAggregatesDeduplicatesAndIsolatesProviderFailure() {
        val targetClass = myFixture.addClass("package example; public class Target {}")
        val target = GtnhPatchTarget(targetClass)
        val patch = GtnhPatch(target, targetClass as Navigatable, GtnhPatchKind.MIXIN_INJECT,
            GtnhPatchConfidence.EXACT, "tick")
        val service = GtnhPatchService(project, listOf(
            GtnhPatchProvider { listOf(patch, patch) },
            GtnhPatchProvider { error("broken provider") },
            GtnhPatchProvider { listOf(patch.copy(detail = "other")) }
        ))

        assertEquals(2, service.findPatches(target).size)
    }

    fun testCancellationIsNotSwallowed() {
        val targetClass = myFixture.addClass("package example; public class Target {}")
        val service = GtnhPatchService(project, listOf(GtnhPatchProvider { throw ProcessCanceledException() }))
        assertThrows(ProcessCanceledException::class.java) {
            service.findPatches(GtnhPatchTarget(targetClass))
        }
    }
}
