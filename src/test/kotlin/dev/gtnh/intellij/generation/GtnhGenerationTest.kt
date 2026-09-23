package dev.gtnh.intellij.generation

import dev.gtnh.intellij.generation.mixin.GtnhMixinGenerator
import dev.gtnh.intellij.generation.project.GtnhProjectModel
import dev.gtnh.intellij.generation.project.GtnhProjectTemplate
import junit.framework.TestCase

class GtnhGenerationTest : TestCase() {
    fun testRejectsInvalidIdentifiers() {
        try {
            GtnhMixinGenerator.plan("example.mixin", "Not-Valid", "game.Target")
            fail("Expected invalid identifier rejection")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    fun testProjectTemplateIncludesOnlySelectedDependencies() {
        val plan = GtnhProjectTemplate.plan(GtnhProjectModel("Example", "example", "dev.example", useGtnhLib = true))
        val build = (plan.changes[1] as GtnhFileChange.CreateFile).content
        assertTrue(build.contains("GTNHLib"))
        assertFalse(build.contains("ModularUI2"))
        assertEquals(3, plan.changes.size)
    }

    fun testPreviewDoesNotApplyChanges() {
        val plan = GtnhMixinGenerator.plan("example.mixin", "TargetMixin", "game.Target")
        assertTrue(plan.changes.single().displayPath.endsWith("TargetMixin.java"))
        assertEquals(GtnhGenerationConfidence.SAFE_STANDALONE, plan.confidence)
    }

    fun testProjectTemplateAddsMixinConfigurationOnlyWhenSelected() {
        val plan = GtnhProjectTemplate.plan(
            GtnhProjectModel("Example", "example", "dev.example", useMixin = true)
        )

        val mixinConfig = plan.changes.single { it.displayPath == "src/main/resources/mixins.example.json" }
            as GtnhFileChange.CreateFile
        assertTrue(mixinConfig.content.contains("\"package\": \"dev.example.mixin\""))
        assertEquals(4, plan.changes.size)
    }
}
