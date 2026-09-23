package dev.gtnh.intellij.generation.project

import dev.gtnh.intellij.generation.GtnhFileChange
import dev.gtnh.intellij.generation.GtnhGenerationConfidence
import dev.gtnh.intellij.generation.GtnhGenerationPlan
import dev.gtnh.intellij.generation.GtnhJavaNames

data class GtnhProjectModel(
    val modName: String,
    val modId: String,
    val group: String,
    val useGtnhLib: Boolean = false,
    val useModularUi2: Boolean = false,
    val useStructureLib: Boolean = false,
    val useMixin: Boolean = false
)

object GtnhProjectTemplate {
    fun plan(model: GtnhProjectModel): GtnhGenerationPlan {
        val modId = GtnhJavaNames.requireIdentifier(model.modId, "Mod ID").lowercase()
        val group = GtnhJavaNames.requirePackage(model.group)
        require(model.modName.isNotBlank()) { "Mod name is required" }
        val dependencies = buildList {
            if (model.useGtnhLib) add("    implementation(\"com.github.GTNewHorizons:GTNHLib:+\")")
            if (model.useModularUi2) add("    implementation(\"com.github.GTNewHorizons:ModularUI2:+\")")
            if (model.useStructureLib) add("    implementation(\"com.github.GTNewHorizons:StructureLib:+\")")
        }.joinToString("\n")
        val sourcePath = "src/main/java/${group.replace('.', '/')}/${modId.replaceFirstChar(Char::uppercase)}Mod.java"
        val changes = mutableListOf<GtnhFileChange>(
            GtnhFileChange.CreateFile("settings.gradle.kts", "rootProject.name = \"${model.modName}\"\n"),
            GtnhFileChange.CreateFile("build.gradle.kts", buildFile(group, dependencies)),
            GtnhFileChange.CreateFile(sourcePath, "package $group;\n\npublic final class ${modId.replaceFirstChar(Char::uppercase)}Mod {}\n")
        )
        if (model.useMixin) {
            changes += GtnhFileChange.CreateFile(
                "src/main/resources/mixins.$modId.json",
                mixinConfig(group)
            )
        }
        return GtnhGenerationPlan(
            "Create GTNH mod template",
            changes,
            GtnhGenerationConfidence.HIGH
        )
    }

    private fun buildFile(group: String, dependencies: String) = """
        plugins {
            id("com.gtnewhorizons.gtnhconvention")
        }

        group = "$group"

        dependencies {
        $dependencies
        }
    """.trimIndent() + "\n"

    private fun mixinConfig(group: String) = """
        {
          "required": true,
          "package": "$group.mixin",
          "compatibilityLevel": "JAVA_8",
          "mixins": [],
          "client": [],
          "server": []
        }
    """.trimIndent() + "\n"
}
