plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        // IntelliJ Java support
        bundledPlugin("com.intellij.java")

        // Gradle integration
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("Git4Idea")

        // Required dependency: Minecraft Development
        plugin(
            "com.demonwav.minecraft-dev",
            providers.gradleProperty("minecraftDevVersion").get()
        )

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Plugin.Java)
        pluginVerifier()
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "262.*"
        }
    }

    pluginVerification {
        ides {
            val localVerifierIde = providers.gradleProperty("verifierIdePath")
            if (localVerifierIde.isPresent) {
                local(localVerifierIde.get())
            } else {
                recommended()
            }
        }
    }
}
