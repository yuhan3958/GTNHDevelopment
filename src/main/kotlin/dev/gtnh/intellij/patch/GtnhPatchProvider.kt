package dev.gtnh.intellij.patch

fun interface GtnhPatchProvider {
    fun findPatches(target: GtnhPatchTarget): List<GtnhPatch>
}
