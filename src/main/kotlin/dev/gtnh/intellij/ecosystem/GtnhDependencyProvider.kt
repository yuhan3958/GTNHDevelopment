package dev.gtnh.intellij.ecosystem

fun interface GtnhDependencyProvider {
    fun classify(candidate: GtnhDependencyCandidate): GtnhDependencyKind?
}
