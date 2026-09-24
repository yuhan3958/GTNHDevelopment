package dev.gtnh.intellij.generation.project

import junit.framework.TestCase
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

class GtnhStarterCustomizerTest : TestCase() {
    private lateinit var root: Path

    override fun setUp() {
        root = Files.createTempDirectory("gtnh-starter-customizer-test")
    }

    override fun tearDown() {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    fun testAcceptsValidMetadataAndDerivesMainClassName() {
        val model = GtnhModProjectModel("Useful Machines", "usefulmachines", "dev.example.usefulmachines")

        assertNull(model.validationError())
        assertEquals("UsefulMachines", model.mainClassName())
    }

    fun testRejectsInvalidMetadata() {
        assertEquals("Mod name is required", model(modName = " ").validationError())
        assertEquals("Mod ID must start with a lowercase letter and contain only lowercase letters, digits, or underscores", model(modId = "Bad-ID").validationError())
        assertEquals("Mod group must be a valid Java package", model(modGroup = "dev.class.example").validationError())
        assertEquals("Mod name must produce a valid Java class name", model(modName = "123 machines").validationError())
    }

    fun testCustomizesMetadataPackageAndMainClass() {
        writeStarter()
        val model = GtnhModProjectModel("Useful Machines", "usefulmachines", "dev.example.usefulmachines")

        GtnhStarterCustomizer.customize(root, model)

        val packageRoot = root.resolve("src/main/java/dev/example/usefulmachines")
        val mainSource = Files.readString(packageRoot.resolve("UsefulMachines.java"))
        assertFalse(Files.exists(root.resolve("src/main/java/com/myname/mymodid")))
        assertTrue(mainSource.contains("package dev.example.usefulmachines;"))
        assertTrue(mainSource.contains("public class UsefulMachines"))
        assertTrue(mainSource.contains("UsefulMachines.MODID"))
        assertTrue(mainSource.contains("MODID = \"usefulmachines\""))
        assertTrue(mainSource.contains("name = \"Useful Machines\""))
        assertTrue(mainSource.contains("clientSide = \"dev.example.usefulmachines.ClientProxy\""))
        assertEquals(
            "package dev.example.usefulmachines;\n\npublic class Config {\n    private final String owner = UsefulMachines.MODID;\n}\n",
            Files.readString(packageRoot.resolve("Config.java"))
        )

        val properties = Files.readString(root.resolve("gradle.properties"))
        assertTrue(properties.contains("modName = Useful Machines"))
        assertTrue(properties.contains("modId = usefulmachines"))
        assertTrue(properties.contains("modGroup = dev.example.usefulmachines"))
        assertTrue(properties.contains("minecraftVersion = 1.7.10"))
    }

    fun testRejectsMissingOrDuplicateProperties() {
        writeStarter()
        Files.writeString(
            root.resolve("gradle.properties"),
            "modName = MyMod\nmodId = mymodid\nmodId = duplicate\nmodGroup = com.myname.mymodid\n"
        )

        val error = assertFails { GtnhStarterCustomizer.customize(root, model()) }

        assertTrue(error.message.orEmpty().contains("modId"))
    }

    fun testRejectsExistingDestinationPackage() {
        writeStarter()
        Files.createDirectories(root.resolve("src/main/java/dev/example/usefulmachines"))

        val error = assertFails {
            GtnhStarterCustomizer.customize(
                root,
                GtnhModProjectModel("Useful Machines", "usefulmachines", "dev.example.usefulmachines")
            )
        }

        assertTrue(error.message.orEmpty().contains("already exists"))
    }

    private fun model(
        modName: String = "Example Mod",
        modId: String = "examplemod",
        modGroup: String = "dev.example.mod"
    ) = GtnhModProjectModel(modName, modId, modGroup)

    private fun writeStarter() {
        Files.writeString(
            root.resolve("gradle.properties"),
            "modName = MyMod\nmodId = mymodid\nmodGroup = com.myname.mymodid\nminecraftVersion = 1.7.10\n"
        )
        val packageRoot = root.resolve("src/main/java/com/myname/mymodid")
        Files.createDirectories(packageRoot)
        Files.writeString(
            packageRoot.resolve("MyMod.java"),
            """
                package com.myname.mymodid;

                @Mod(modid = MyMod.MODID, name = "MyMod")
                public class MyMod {
                    public static final String MODID = "mymodid";
                    @SidedProxy(clientSide = "com.myname.mymodid.ClientProxy", serverSide = "com.myname.mymodid.CommonProxy")
                    public static CommonProxy proxy;
                }
            """.trimIndent() + "\n"
        )
        Files.writeString(packageRoot.resolve("ClientProxy.java"), "package com.myname.mymodid;\n\npublic class ClientProxy extends CommonProxy {}\n")
        Files.writeString(packageRoot.resolve("CommonProxy.java"), "package com.myname.mymodid;\n\npublic class CommonProxy {}\n")
        Files.writeString(
            packageRoot.resolve("Config.java"),
            "package com.myname.mymodid;\n\npublic class Config {\n    private final String owner = MyMod.MODID;\n}\n"
        )
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
}
