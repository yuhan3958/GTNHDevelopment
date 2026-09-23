package dev.gtnh.intellij.ecosystem

import dev.gtnh.intellij.ecosystem.provider.KnownGtnhDependencyProvider
import junit.framework.TestCase

class KnownGtnhDependencyProviderTest : TestCase() {
    private val provider = KnownGtnhDependencyProvider()

    fun testClassifiesKnownArtifactsAndGtnhGroup() {
        assertEquals(GtnhDependencyKind.GTNH_LIB, classify("com.github.gtnewhorizons", "gtnhlib"))
        assertEquals(GtnhDependencyKind.STRUCTURE_LIB, classify("com.github.gtnewhorizons", "structurelib"))
        assertEquals(GtnhDependencyKind.MODULAR_UI, classify("com.github.gtnewhorizons", "modularui2"))
        assertEquals(GtnhDependencyKind.GT5_API, classify("com.github.gtnewhorizons", "gt5-unofficial"))
        assertEquals(GtnhDependencyKind.CORE_MOD, classify("com.github.gtnewhorizons", "newhorizonscoremod"))
        assertEquals(GtnhDependencyKind.UNKNOWN_GTNH, classify("com.github.gtnewhorizons", "another-lib"))
    }

    fun testDoesNotClassifySimilarThirdPartyArtifact() {
        assertNull(classify("org.example", "my-gtnhlib-wrapper"))
    }

    private fun classify(group: String, artifact: String) = provider.classify(
        GtnhDependencyCandidate(group, artifact, "1.0", "main")
    )
}
