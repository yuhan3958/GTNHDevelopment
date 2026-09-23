package dev.gtnh.intellij.patch.provider

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import dev.gtnh.intellij.patch.GtnhPatch
import dev.gtnh.intellij.patch.GtnhPatchConfidence
import dev.gtnh.intellij.patch.GtnhPatchKind
import dev.gtnh.intellij.patch.GtnhPatchProvider
import dev.gtnh.intellij.patch.GtnhPatchTarget

class AccessTransformerPatchProvider(private val project: Project) : GtnhPatchProvider {
    override fun findPatches(target: GtnhPatchTarget): List<GtnhPatch> {
        val targetName = target.targetClass.qualifiedName ?: return emptyList()
        val internalName = targetName.replace('.', '/')
        val scope = GlobalSearchScope.projectScope(project)
        val files = sequenceOf("cfg", "at")
            .flatMap { extension -> FilenameIndex.getAllFilesByExt(project, extension, scope).asSequence() }
            .filter { file -> file.name.contains("at", ignoreCase = true) || file.name.contains("access", ignoreCase = true) }
            .distinctBy { it.path }
        return files.flatMap { file ->
            ProgressManager.checkCanceled()
            val text = runCatching { String(file.contentsToByteArray(), file.charset) }.getOrNull()
                ?: return@flatMap emptySequence()
            var offset = 0
            text.lineSequence().mapNotNull { rawLine ->
                ProgressManager.checkCanceled()
                val lineOffset = offset
                offset += rawLine.length + 1
                val line = rawLine.substringBefore('#').trim()
                val tokens = line.split(Regex("\\s+")).filter(String::isNotBlank)
                if (tokens.size < 2 || tokens[1] != targetName && tokens[1] != internalName) return@mapNotNull null
                val member = tokens.getOrNull(2)
                val targetMethod = target.targetMethod
                if (targetMethod != null && member?.substringBefore('(') != targetMethod.name) return@mapNotNull null
                GtnhPatch(target, OpenFileDescriptor(project, file, lineOffset),
                    GtnhPatchKind.ACCESS_TRANSFORMER, GtnhPatchConfidence.HEURISTIC, member ?: tokens[1])
            }
        }.toList()
    }
}
