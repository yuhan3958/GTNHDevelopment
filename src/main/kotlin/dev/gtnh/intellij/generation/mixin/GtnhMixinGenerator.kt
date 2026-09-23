package dev.gtnh.intellij.generation.mixin

import dev.gtnh.intellij.generation.GtnhFileChange
import dev.gtnh.intellij.generation.GtnhGenerationConfidence
import dev.gtnh.intellij.generation.GtnhGenerationPlan
import dev.gtnh.intellij.generation.GtnhJavaNames

object GtnhMixinGenerator {
    fun plan(packageName: String, className: String, targetFqn: String): GtnhGenerationPlan {
        val pkg = GtnhJavaNames.requirePackage(packageName)
        val name = GtnhJavaNames.requireIdentifier(className, "Mixin class name")
        require(targetFqn.contains('.')) { "Target class must be fully qualified" }
        val path = "src/main/java/${pkg.replace('.', '/')}/$name.java"
        val content = """
            package $pkg;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(targets = "$targetFqn")
            public abstract class $name {
            }
        """.trimIndent() + "\n"
        return GtnhGenerationPlan("Create GTNH Mixin", listOf(GtnhFileChange.CreateFile(path, content)),
            GtnhGenerationConfidence.SAFE_STANDALONE)
    }
}
