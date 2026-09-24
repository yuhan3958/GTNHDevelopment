package dev.gtnh.intellij.generation.project

data class GtnhModProjectModel(
    val modName: String,
    val modId: String,
    val modGroup: String
) {
    fun validationError(): String? = when {
        modName.isBlank() -> "Mod name is required"
        !MOD_ID.matches(modId) ->
            "Mod ID must start with a lowercase letter and contain only lowercase letters, digits, or underscores"
        !isValidPackage(modGroup) -> "Mod group must be a valid Java package"
        deriveMainClassName(modName) == null -> "Mod name must produce a valid Java class name"
        else -> null
    }

    fun mainClassName(): String = deriveMainClassName(modName)
        ?: throw IllegalArgumentException("Mod name must produce a valid Java class name")

    companion object {
        private val MOD_ID = Regex("[a-z][a-z0-9_]*")
        private val PACKAGE = Regex("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)+")
        private val JAVA_KEYWORDS = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "_"
        )

        private fun isValidPackage(value: String): Boolean =
            PACKAGE.matches(value) && value.split('.').none(JAVA_KEYWORDS::contains)

        private fun deriveMainClassName(value: String): String? {
            val result = value
                .split(Regex("[^\\p{L}\\p{N}_$]+"))
                .filter(String::isNotEmpty)
                .joinToString("") { word -> word.replaceFirstChar { it.uppercaseChar() } }
            if (result.isEmpty() || !Character.isJavaIdentifierStart(result.first())) return null
            if (result.drop(1).any { !Character.isJavaIdentifierPart(it) }) return null
            if (result in JAVA_KEYWORDS) return null
            return result
        }
    }
}
