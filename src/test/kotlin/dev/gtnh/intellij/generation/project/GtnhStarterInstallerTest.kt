package dev.gtnh.intellij.generation.project

import junit.framework.TestCase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GtnhStarterInstallerTest : TestCase() {
    private lateinit var root: Path
    private lateinit var fixtureZip: Path
    private val model = GtnhModProjectModel("Useful Machines", "usefulmachines", "dev.example.usefulmachines")

    override fun setUp() {
        root = Files.createTempDirectory("gtnh-starter-installer-test")
        fixtureZip = Files.createTempFile("gtnh-starter-fixture", ".zip")
    }

    override fun tearDown() {
        deleteTree(root)
        Files.deleteIfExists(fixtureZip)
    }

    fun testInstallsAndCustomizesCompleteStarter() {
        writeCompleteStarterZip(fixtureZip)

        installer(fixtureZip).install(root, model)

        assertTrue(Files.isRegularFile(root.resolve("build.gradle.kts")))
        assertTrue(Files.isRegularFile(root.resolve("src/main/java/dev/example/usefulmachines/UsefulMachines.java")))
        assertTrue(Files.readString(root.resolve("gradle.properties")).contains("modId = usefulmachines"))
    }

    fun testRejectsParentTraversalWithoutWritingOutsideRoot() {
        val outsideName = "${root.fileName}-outside.txt"
        writeZip(fixtureZip, mapOf("../$outsideName" to "unsafe"))
        val outside = root.parent.resolve(outsideName)

        val error = assertFails { installer(fixtureZip).install(root, model) }

        assertTrue(error.message.orEmpty().contains("Unsafe ZIP entry"))
        assertFalse(Files.exists(outside))
    }

    fun testRejectsAbsoluteZipEntry() {
        writeZip(fixtureZip, mapOf("/absolute.txt" to "unsafe"))

        val error = assertFails { installer(fixtureZip).install(root, model) }

        assertTrue(error.message.orEmpty().contains("Unsafe ZIP entry"))
        assertEquals(0L, Files.list(root).use { it.count() })
    }

    fun testRejectsIncompleteStarterBeforePublishing() {
        writeZip(fixtureZip, mapOf("gradle.properties" to "modName = MyMod\nmodId = mymodid\nmodGroup = com.myname.mymodid\n"))

        val error = assertFails { installer(fixtureZip).install(root, model) }

        assertTrue(error.message.orEmpty().contains("MyMod.java"))
        assertEquals(0L, Files.list(root).use { it.count() })
    }

    fun testPreservesExistingFileAndRollsBackPublishedPaths() {
        writeCompleteStarterZip(fixtureZip, mapOf("a-created.txt" to "created", "z-existing.txt" to "replacement"))
        val existing = root.resolve("z-existing.txt")
        Files.writeString(existing, "original")

        val error = assertFails { installer(fixtureZip).install(root, model) }

        assertTrue(error.message.orEmpty().contains("already exists"))
        assertEquals("original", Files.readString(existing))
        assertFalse(Files.exists(root.resolve("a-created.txt")))
        assertFalse(Files.exists(root.resolve("gradle.properties")))
    }

    fun testCancellationLeavesProjectDirectoryUntouched() {
        writeCompleteStarterZip(fixtureZip)
        var checks = 0

        assertFails {
            installer(fixtureZip).install(root, model) {
                checks++
                if (checks == 4) throw InterruptedException("cancelled")
            }
        }

        assertEquals(0L, Files.list(root).use { it.count() })
    }

    private fun installer(source: Path) = GtnhStarterInstaller { target ->
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun writeCompleteStarterZip(target: Path, extraEntries: Map<String, String> = emptyMap()) {
        val entries = linkedMapOf(
            "build.gradle.kts" to "plugins { id(\"com.gtnewhorizons.gtnhconvention\") }\n",
            "gradle.properties" to "modName = MyMod\nmodId = mymodid\nmodGroup = com.myname.mymodid\nminecraftVersion = 1.7.10\n",
            "src/main/java/com/myname/mymodid/MyMod.java" to
                "package com.myname.mymodid;\n@Mod(modid = MyMod.MODID, name = \"MyMod\")\npublic class MyMod { public static final String MODID = \"mymodid\"; }\n",
            "src/main/java/com/myname/mymodid/ClientProxy.java" to "package com.myname.mymodid;\npublic class ClientProxy extends CommonProxy {}\n",
            "src/main/java/com/myname/mymodid/CommonProxy.java" to "package com.myname.mymodid;\npublic class CommonProxy {}\n",
            "src/main/java/com/myname/mymodid/Config.java" to "package com.myname.mymodid;\npublic class Config {}\n"
        )
        entries.putAll(extraEntries)
        writeZip(target, entries)
    }

    private fun writeZip(target: Path, entries: Map<String, String>) {
        ZipOutputStream(Files.newOutputStream(target)).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun assertFails(block: () -> Unit): Throwable {
        var failure: Throwable? = null
        try {
            block()
        } catch (error: Throwable) {
            failure = error
        }
        if (failure == null) fail("Expected operation to fail")
        return failure!!
    }

    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
