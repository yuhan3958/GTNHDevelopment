package dev.gtnh.intellij.generation.config

import dev.gtnh.intellij.generation.GtnhFileChange
import dev.gtnh.intellij.generation.GtnhGenerationConfidence
import dev.gtnh.intellij.generation.GtnhGenerationPlan
import dev.gtnh.intellij.generation.GtnhJavaNames

object GtnhConfigEntryGenerator {
    fun plan(packageName: String, fieldName: String): GtnhGenerationPlan {
        val pkg = GtnhJavaNames.requirePackage(packageName)
        val field = GtnhJavaNames.requireIdentifier(fieldName, "Config field")
        val path = "src/main/java/${pkg.replace('.', '/')}/GeneratedGtnhConfig.java"
        val content = """
            package $pkg;

            public final class GeneratedGtnhConfig {
                private GeneratedGtnhConfig() {}

                public static boolean $field = true;
            }
        """.trimIndent() + "\n"
        return GtnhGenerationPlan("Create standalone GTNH config entry",
            listOf(GtnhFileChange.CreateFile(path, content)), GtnhGenerationConfidence.SAFE_STANDALONE)
    }
}
