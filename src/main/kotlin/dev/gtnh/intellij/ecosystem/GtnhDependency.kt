package dev.gtnh.intellij.ecosystem

enum class GtnhDependencyKind(val displayName: String) {
    GTNH_LIB("GTNHLib"), STRUCTURE_LIB("StructureLib"), MODULAR_UI("ModularUI"),
    GT5_API("GT5-Unofficial API"), CORE_MOD("NewHorizonsCoreMod"), UNKNOWN_GTNH("GTNH library")
}

data class GtnhDependency(
    val coordinates: String,
    val version: String?,
    val moduleName: String,
    val kind: GtnhDependencyKind
)

data class GtnhDependencyCandidate(
    val group: String?,
    val artifact: String,
    val version: String?,
    val moduleName: String
) {
    val coordinates: String = listOfNotNull(group, artifact).joinToString(":")
}
