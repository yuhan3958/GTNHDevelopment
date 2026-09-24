# GTNH Development

GTNH Development is an IntelliJ IDEA plugin for GregTech: New Horizons contributors. It extends the required Minecraft Development plugin with GTNH-specific project detection, Gradle workflows, bidirectional configuration/Mixin navigation, patch analysis, environment diagnostics, ecosystem information, contribution assistance, and safe code generation.

> [!IMPORTANT]
> GTNH Development is an unofficial community project. It is not developed, endorsed, or officially supported by the GT New Horizons team or JetBrains.

Minecraft Development remains responsible for generic Minecraft, Forge, and Sponge Mixin IDE support. GTNH Development adds only GTNH conventions and workflows.

## Requirements

- IntelliJ IDEA 2025.1 through 2026.2 (builds `251` through `262.*`)
- Java 21 for building the plugin; IntelliJ 2026.2 itself may use Java 25
- Minecraft Development (`com.demonwav.minecraft-dev`), which is a required plugin dependency

The build uses IntelliJ IDEA 2026.1.4 and Minecraft Development 2026.1-1.8.19 as its current compile baseline. CI also exercises representative 2025.1, 2025.3, and 2026.2 combinations.

## Installation

Build the plugin and install the ZIP from **Settings | Plugins | Install Plugin from Disk...**:

```powershell
.\gradlew.bat buildPlugin
```

The archive is written to `build/distributions/`. Minecraft Development must be installed and enabled; GTNH Development will not load without it.

## Project detection

Detection combines build markers for GTNH Convention, RetroFuturaGradle, and GTNHGradle. Settings can disable automatic detection, while project-local state supports explicit force-on or force-off overrides. Detection is intentionally conservative and is not tied to one repository.

When a GTNH project is first detected, the plugin can show one non-intrusive notification. This can be disabled under **Settings | Tools | GTNH Development**.

## GTNH tool window

The **GTNH** tool window groups available functionality into:

- **Overview**: detected GTNH ecosystem dependencies and resolved versions where available
- **Run**: relevant tasks discovered from IntelliJ's imported Gradle model
- **Code**: Config/Mixin and Patch Impact entry points
- **Environment**: read-only project, SDK, wrapper, and Gradle model diagnostics
- **Contribution**: Git facts, explicit human confirmations, and an editable PR draft

Unavailable optional sections and Gradle tasks are hidden instead of shown as unusable controls.

## Gradle and run integration

Tasks are discovered from IntelliJ's External System model. The plugin does not assume that `runClient`, `runServer`, `check`, or formatting tasks exist. Execution uses IntelliJ's Gradle runner with the exact task path and owning external project path, including multi-module builds.

## Config and Mixin navigation

Use the editor context menu on a configuration field:

- **Find GTNH Mixins Using This Config**

Use the context menu inside a class whose annotation resolves to `org.spongepowered.asm.mixin.Mixin`:

- **Find GTNH Config Controlling This Mixin**

The shared provider graph recognizes conditional branches, direct conditional calls, builder chains, class literals, and string registrations. Compound conditions return every referenced field without boolean simplification. Ambiguous string names return all valid Mixin candidates instead of guessing. Searches run outside the UI thread; conservative gutter markers only use previously cached smart-pointer snapshots.

## Patch Impact Explorer

**Show GTNH Patch Impact** finds project code affecting the selected class or method:

- resolved Sponge Mixin annotations are shown as **Exact**
- ASM transformer and Access Transformer evidence is shown as **Heuristic**
- controlling config fields are included when the shared Mixin link graph can resolve them

The explorer supplements rather than duplicates Minecraft Development inspections.

## Environment Doctor

The Environment panel reports read-only facts such as GTNH detection, project SDK, Gradle wrapper, and imported relevant tasks. Missing or unsynchronized models produce an **Unknown** result rather than an exception. The Doctor never changes SDKs, Gradle JVMs, remotes, or build files.

## Ecosystem navigation

The Overview detects GTNHLib, StructureLib, ModularUI variants, GT5-Unofficial APIs, NewHorizonsCoreMod, and strong GTNH-group dependencies from local module library data. No repository is cloned and core discovery requires no internet access.

## Pull Request Assistant

The Contribution panel reads Git branch, worktree, and remote facts through Git4Idea. It creates an editable local draft with Summary, Changes, Testing, Related Issues, and Checklist sections.

The plugin never invents testing or human claims. DevEnv testing, Fullpack testing, and AI policy acknowledgement remain unchecked until the user explicitly confirms them. It does not submit pull requests.

## Safe generation

**New GTNH Mixin** and **New GTNH Config Entry** create standalone source through an immutable generation plan. A native preview is shown before applying changes. Existing files are never overwritten. When repository conventions are not confidently recognized, existing registration or config files are not modified.

The project-template model can generate a small Gradle Kotlin DSL project with opt-in GTNHLib, ModularUI2, StructureLib, and Mixin support.

## Settings

Open **Settings | Tools | GTNH Development** to control automatic detection, navigation, gutter icons, Patch Impact and heuristics, Environment Doctor, PR Assistant, and first-run notifications.

## Building and testing

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
```

For cached dependencies only, add `--offline`. Use `runIde` to start a sandbox IDE. Version properties are in `gradle.properties`; compatibility jobs are in `.github/workflows/compatibility.yml`.

## Troubleshooting

- If no task appears, refresh the Gradle project and confirm IntelliJ imported its task model.
- If navigation is unavailable, wait for indexing to finish and verify that the class uses the resolved Sponge `@Mixin` annotation.
- If a string registration is ambiguous, use a fully qualified class name or choose among the returned candidates.
- If a feature is hidden, check **Settings | Tools | GTNH Development** and project detection.
- If generated output collides with an existing file, rename the requested type or edit the existing file manually.

## Privacy and network behavior

Core analysis uses local PSI, indexes, Gradle models, Git state, and project files. The plugin does not clone repositories, submit pull requests, or silently mutate project configuration. Normal navigation and diagnostics do not require GitHub authentication.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for architecture, tests, compatibility rules, and contribution workflow. Release history is in [CHANGELOG.md](./CHANGELOG.md).

## License

This repository currently has no license file. Confirm applicable terms with the maintainers before redistribution.
