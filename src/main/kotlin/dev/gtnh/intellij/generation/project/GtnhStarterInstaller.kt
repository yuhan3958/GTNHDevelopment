package dev.gtnh.intellij.generation.project

import com.intellij.util.io.HttpRequests
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.zip.ZipInputStream

class GtnhStarterInstaller(
    private val archiveProvider: (Path) -> Unit = ::downloadStarter
) {
    fun install(
        projectRoot: Path,
        model: GtnhModProjectModel,
        checkCanceled: () -> Unit = {}
    ) {
        model.validationError()?.let { throw IllegalArgumentException(it) }
        val destinationRoot = projectRoot.toAbsolutePath().normalize()
        require(Files.isDirectory(destinationRoot)) { "Project directory does not exist: $destinationRoot" }

        val archive = Files.createTempFile("gtnh-starter-", ".zip")
        val stagingRoot = Files.createTempDirectory("gtnh-starter-")
        try {
            checkCanceled()
            archiveProvider(archive)
            checkCanceled()
            extract(archive, stagingRoot, checkCanceled)
            validateStarter(stagingRoot)
            GtnhStarterCustomizer.customize(stagingRoot, model)
            setGradleWrapperExecutable(stagingRoot)
            publish(stagingRoot, destinationRoot, checkCanceled)
            setGradleWrapperExecutable(destinationRoot)
        } finally {
            runCatching { deleteTree(stagingRoot) }
            runCatching { Files.deleteIfExists(archive) }
        }
    }

    private fun extract(archive: Path, stagingRoot: Path, checkCanceled: () -> Unit) {
        val normalizedRoot = stagingRoot.toAbsolutePath().normalize()
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                checkCanceled()
                val entry = zip.nextEntry ?: break
                val entryPath = Path.of(entry.name)
                val destination = normalizedRoot.resolve(entryPath).normalize()
                require(entry.name.isNotEmpty() && !entryPath.isAbsolute && destination.startsWith(normalizedRoot)) {
                    "Unsafe ZIP entry: ${entry.name}"
                }

                if (entry.isDirectory) {
                    require(!Files.exists(destination) || Files.isDirectory(destination)) {
                        "Conflicting ZIP entry: ${entry.name}"
                    }
                    Files.createDirectories(destination)
                } else {
                    require(!Files.exists(destination)) { "Duplicate ZIP entry: ${entry.name}" }
                    Files.createDirectories(destination.parent)
                    Files.copy(zip, destination)
                }
                zip.closeEntry()
            }
        }
    }

    private fun validateStarter(stagingRoot: Path) {
        require(Files.isRegularFile(stagingRoot.resolve("gradle.properties"))) {
            "Starter archive is missing gradle.properties"
        }
        require(Files.isRegularFile(stagingRoot.resolve(EXAMPLE_MAIN_CLASS))) {
            "Starter archive is missing $EXAMPLE_MAIN_CLASS"
        }
    }

    private fun publish(stagingRoot: Path, projectRoot: Path, checkCanceled: () -> Unit) {
        val created = mutableListOf<Path>()
        try {
            Files.walk(stagingRoot).use { paths ->
                paths.filter { it != stagingRoot }
                    .sorted(compareBy<Path>({ it.nameCount }, { it.toString() }))
                    .forEach { source ->
                        checkCanceled()
                        val destination = projectRoot.resolve(stagingRoot.relativize(source)).normalize()
                        require(destination.startsWith(projectRoot)) { "Unsafe destination path: $destination" }
                        require(!Files.exists(destination)) { "Project path already exists: $destination" }
                        if (Files.isDirectory(source)) {
                            Files.createDirectory(destination)
                        } else {
                            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES)
                        }
                        created += destination
                    }
            }
        } catch (error: Throwable) {
            rollback(created, error)
            throw error
        }
    }

    private fun rollback(created: List<Path>, originalError: Throwable) {
        created.asReversed().forEach { path ->
            try {
                Files.deleteIfExists(path)
            } catch (cleanupError: Throwable) {
                originalError.addSuppressed(cleanupError)
            }
        }
    }

    private fun setGradleWrapperExecutable(root: Path) {
        val wrapper = root.resolve("gradlew")
        if (Files.isRegularFile(wrapper)) {
            wrapper.toFile().setExecutable(true, false)
        }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    companion object {
        const val STARTER_URL =
            "https://github.com/GTNewHorizons/ExampleMod1.7.10/releases/download/master-packages/starter.zip"
        private val EXAMPLE_MAIN_CLASS = Path.of("src", "main", "java", "com", "myname", "mymodid", "MyMod.java")
    }
}

private fun downloadStarter(target: Path) {
    HttpRequests.request(GtnhStarterInstaller.STARTER_URL)
        .productNameAsUserAgent()
        .connect { request ->
            Files.copy(request.inputStream, target, StandardCopyOption.REPLACE_EXISTING)
        }
}
