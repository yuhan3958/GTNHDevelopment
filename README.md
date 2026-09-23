# GTNH Development

An IntelliJ IDEA plugin that reduces repetitive work when developing [GregTech: New Horizons](https://github.com/GTNewHorizons) mods.

Built on top of the Minecraft Development plugin, it detects GTNH projects, provides shortcuts for common Gradle tasks, and navigates from configuration fields to the Mixin classes they control.

## Features

### GTNH project detection

The plugin detects a GTNH project by looking for any of the following markers in the root `build.gradle` or `build.gradle.kts` file:

- `com.gtnewhorizons.gtnhconvention`
- `com.gtnewhorizons.retrofuturagradle`
- `GTNHGradle`

GTNH-specific Gradle task buttons are disabled when the current project is not recognized as a GTNH project.

### GTNH tool window

The **GTNH** tool window on the right side of the IDE provides shortcuts for common Gradle tasks.

| Button | Gradle task |
| --- | --- |
| Run Client | `runClient` |
| Run Server | `runServer` |
| Run Server 17 | `runServer17` |
| Run Server 21 | `runServer21` |
| Run Server 25 | `runServer25` |
| Build | `build` |
| Spotless Apply | `spotlessApply` |

Tasks run from the project root through IntelliJ IDEA's Gradle integration.

### Navigate from a configuration field to its Mixins

Place the caret on a Java configuration field declaration or reference in a GTNH project, then use either of the following:

- Keyboard shortcut: <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>G</kbd>
- Editor context menu: **Find GTNH Mixins Using This Config**

The plugin searches all references to the field and recognizes Mixin registrations in:

- `if` statements and conditional expressions
- Builder call chains controlled by the configuration value
- Direct registrations where a lambda contains both the configuration value and Mixin

If one Mixin is found, the editor navigates directly to it. If multiple Mixins are found, a chooser displays their registration types and packages. If nothing is found, the IDE shows a notification.

Mixin classes can be resolved from class literals, class references, fully qualified class-name strings, and simple class-name strings. When multiple classes share a name, classes in the current module are preferred.

## Requirements

- IntelliJ IDEA 2026.1 (`261.*`)
- Java 21
- [Minecraft Development](https://plugins.jetbrains.com/plugin/8327-minecraft-development) 2026.1-1.8.19

Minecraft Development is a required plugin dependency and must be installed.

## Building from source

The repository includes the Gradle Wrapper.

Windows:

```powershell
.\gradlew.bat buildPlugin
```

macOS/Linux:

```bash
./gradlew buildPlugin
```

The plugin distribution is written to `build/distributions/`. Install the generated ZIP from **Settings | Plugins | Install Plugin from Disk...** in IntelliJ IDEA.

To use only dependencies already available in the local Gradle cache, build in offline mode:

```powershell
.\gradlew.bat --offline buildPlugin
```

To check Kotlin compilation only:

```powershell
.\gradlew.bat --offline compileKotlin
```

## Development

Run a sandbox IDE:

```powershell
.\gradlew.bat runIde
```

Run all checks:

```powershell
.\gradlew.bat check
```

The main versions are configured in [`gradle.properties`](./gradle.properties).

| Component | Current version |
| --- | --- |
| Plugin | `0.2.0` |
| IntelliJ Platform | `2026.1.4` |
| Kotlin | `2.2.20` |
| IntelliJ Platform Gradle Plugin | `2.19.0` |

See [`CHANGELOG.md`](./CHANGELOG.md) for release notes.

## Project structure

```text
src/main/kotlin/dev/gtnh/intellij/
├── action/    # Editor actions
├── config/    # Configuration field detection
├── gradle/    # IntelliJ Gradle task execution
├── mixin/     # Usage search, Mixin resolution, and registration analysis
├── project/   # GTNH project detection
└── ui/        # Tool window and navigation popup
```

Plugin metadata and extension registrations are defined in [`plugin.xml`](./src/main/resources/META-INF/plugin.xml).

## License

This repository does not currently include a license file. Confirm the applicable terms with the project maintainers before redistribution or external contribution.
