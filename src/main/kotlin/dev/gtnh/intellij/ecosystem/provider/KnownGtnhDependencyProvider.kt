package dev.gtnh.intellij.ecosystem.provider

import dev.gtnh.intellij.ecosystem.GtnhDependencyCandidate
import dev.gtnh.intellij.ecosystem.GtnhDependencyKind
import dev.gtnh.intellij.ecosystem.GtnhDependencyProvider

class KnownGtnhDependencyProvider : GtnhDependencyProvider {
    override fun classify(candidate: GtnhDependencyCandidate): GtnhDependencyKind? {
        val artifact = candidate.artifact.lowercase()
        return when {
            artifact == "gtnhlib" -> GtnhDependencyKind.GTNH_LIB
            artifact == "structurelib" -> GtnhDependencyKind.STRUCTURE_LIB
            artifact in setOf("modularui", "modularui2") -> GtnhDependencyKind.MODULAR_UI
            artifact in setOf("gregtech", "gregtech5", "gt5-unofficial") -> GtnhDependencyKind.GT5_API
            artifact == "newhorizonscoremod" -> GtnhDependencyKind.CORE_MOD
            candidate.group?.lowercase()?.let { it == "com.github.gtnewhorizons" || it == "com.gtnewhorizons" } == true ->
                GtnhDependencyKind.UNKNOWN_GTNH
            else -> null
        }
    }
}
