package dev.gtnh.intellij.settings

import junit.framework.TestCase

class GtnhSettingsStateTest : TestCase() {
    fun testDefaultsKeepFeaturesEnabled() {
        val state = GtnhSettingsState.Data()
        assertTrue(state.automaticDetection)
        assertTrue(state.mixinNavigation)
        assertTrue(state.patchImpact)
        assertTrue(state.firstRunNotifications)
    }

    fun testProjectOverrideDefaultsToAuto() {
        assertEquals(GtnhDetectionOverride.AUTO, GtnhProjectSettingsState.Data().detectionOverride)
    }
}
