package dev.gtnh.intellij.generation

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Path

class GtnhGenerationService(private val project: Project) {
    fun validate(plan: GtnhGenerationPlan): List<String> {
        val base = project.basePath?.let(Path::of)?.toAbsolutePath()?.normalize()
            ?: return listOf("Project path is unavailable")
        return plan.changes.mapNotNull { change ->
            when (change) {
                is GtnhFileChange.CreateFile -> {
                    val target = base.resolve(change.displayPath).normalize()
                    when {
                        !target.startsWith(base) -> "Path escapes the project: ${change.displayPath}"
                        target.toFile().exists() -> "File already exists: ${change.displayPath}"
                        else -> null
                    }
                }
            }
        }
    }

    fun apply(plan: GtnhGenerationPlan) {
        val errors = validate(plan)
        require(errors.isEmpty()) { errors.joinToString("; ") }
        val basePath = project.basePath ?: error("Project path is unavailable")
        WriteCommandAction.runWriteCommandAction(project, plan.title, null, {
            plan.changes.forEach { change ->
                when (change) {
                    is GtnhFileChange.CreateFile -> {
                        val target = Path.of(basePath).resolve(change.displayPath).normalize()
                        val parent = VfsUtil.createDirectories(target.parent.toString())
                        val file = parent.createChildData(this, target.fileName.toString())
                        VfsUtil.saveText(file, change.content)
                    }
                }
            }
        })
    }

    fun preview(plan: GtnhGenerationPlan): String = buildString {
        appendLine(plan.title)
        appendLine("Confidence: ${plan.confidence}")
        plan.changes.forEach { change ->
            appendLine()
            appendLine("--- /dev/null")
            appendLine("+++ ${change.displayPath}")
            when (change) {
                is GtnhFileChange.CreateFile -> change.content.lineSequence().forEach { appendLine("+$it") }
            }
        }
    }.trimEnd()
}
