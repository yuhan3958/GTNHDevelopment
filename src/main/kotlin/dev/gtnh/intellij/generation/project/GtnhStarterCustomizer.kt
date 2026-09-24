package dev.gtnh.intellij.generation.project

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object GtnhStarterCustomizer {
    private const val EXAMPLE_PACKAGE = "com.myname.mymodid"
    private val EXAMPLE_PACKAGE_PATH: Path = Path.of("src", "main", "java", "com", "myname", "mymodid")

    fun customize(projectRoot: Path, model: GtnhModProjectModel) {
        model.validationError()?.let { throw IllegalArgumentException(it) }

        val propertiesPath = projectRoot.resolve("gradle.properties")
        require(Files.isRegularFile(propertiesPath)) { "Starter is missing gradle.properties" }
        var properties = Files.readString(propertiesPath)
        properties = replaceProperty(properties, "modName", model.modName)
        properties = replaceProperty(properties, "modId", model.modId)
        properties = replaceProperty(properties, "modGroup", model.modGroup)
        Files.writeString(propertiesPath, properties)

        val sourcePackage = projectRoot.resolve(EXAMPLE_PACKAGE_PATH)
        require(Files.isDirectory(sourcePackage)) { "Starter is missing the example Java package" }
        val destinationPackage = projectRoot
            .resolve(Path.of("src", "main", "java"))
            .resolve(model.modGroup.replace('.', '/'))

        val packageRoot = if (sourcePackage.normalize() == destinationPackage.normalize()) {
            sourcePackage
        } else {
            require(!Files.exists(destinationPackage)) {
                "Destination package already exists: ${model.modGroup}"
            }
            Files.createDirectories(destinationPackage.parent)
            Files.move(sourcePackage, destinationPackage, StandardCopyOption.ATOMIC_MOVE)
            destinationPackage
        }

        val className = model.mainClassName()
        Files.walk(packageRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".java") }
                .forEach { source ->
                    var content = Files.readString(source)
                    content = content.replace(EXAMPLE_PACKAGE, model.modGroup)
                    content = content.replace("name = \"MyMod\"", "name = \"${model.modName}\"")
                    content = content.replace(Regex("\\bMyMod\\b"), className)
                    if (source.fileName.toString() == "MyMod.java") {
                        content = content.replace("\"mymodid\"", "\"${model.modId}\"")
                    }
                    Files.writeString(source, content)
                }
        }

        val oldMainClass = packageRoot.resolve("MyMod.java")
        require(Files.isRegularFile(oldMainClass)) { "Starter is missing MyMod.java" }
        val newMainClass = packageRoot.resolve("$className.java")
        if (oldMainClass != newMainClass) {
            require(!Files.exists(newMainClass)) { "Main class already exists: $className.java" }
            Files.move(oldMainClass, newMainClass, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun replaceProperty(content: String, key: String, value: String): String {
        val property = Regex("(?m)^(\\s*${Regex.escape(key)}\\s*=\\s*).*$")
        val matches = property.findAll(content).toList()
        require(matches.size == 1) { "Starter must contain exactly one $key property" }
        return property.replace(content) { match -> match.groupValues[1] + value }
    }
}
