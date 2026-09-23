package dev.gtnh.intellij.generation

object GtnhJavaNames {
    private val identifier = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
    private val keywords = setOf("class", "interface", "enum", "package", "public", "private", "protected", "static")

    fun requireIdentifier(value: String, label: String): String = value.also {
        require(identifier.matches(it) && it !in keywords) { "$label is not a valid Java identifier" }
    }

    fun requirePackage(value: String): String = value.also {
        require(it.split('.').all { part -> identifier.matches(part) && part !in keywords }) {
            "Package is not valid"
        }
    }
}
