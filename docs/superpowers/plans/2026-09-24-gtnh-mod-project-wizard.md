# GTNH Mod Project Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a GTNH Mod generator to IntelliJ IDEA's New Project window that downloads the official starter and applies user-provided Mod name, Mod ID, and Mod group values.

**Architecture:** A pure metadata/customization layer validates inputs and rewrites the official starter's known placeholders. A starter installer downloads to a temporary archive, extracts with ZIP traversal protection, customizes the new files, and rolls back only files created by the attempt. A `GeneratorNewProjectWizard` binds the three metadata fields, runs installation with progress, and links the generated Gradle project.

**Tech Stack:** Kotlin 2.2, IntelliJ Platform 2026.1 New Project Wizard API, IntelliJ `HttpRequests`, Kotlin UI DSL, Java NIO ZIP APIs, Gradle external-system integration, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-24-gtnh-mod-project-wizard-design.md`

## Global Constraints

- Register a framework generator through `com.intellij.newProjectWizard.generator` and implement `GeneratorNewProjectWizard`.
- Download `https://github.com/GTNewHorizons/ExampleMod1.7.10/releases/download/master-packages/starter.zip` at project-creation time.
- Require separate Mod name, Mod ID, and Mod group values.
- Modify only the official starter's known metadata and Java placeholders.
- Do not initialize Git, write a license, configure optional features, or run Gradle tasks.
- Never allow a ZIP entry to escape the selected project directory.
- On failure, remove only paths created by the current generation attempt.
- Preserve the user's existing `gradle.properties` version change and all unrelated working-tree changes.
- Repository instruction: do not run build, test, or application execution commands unless the user explicitly asks. Test sources may be written, but their execution remains pending.

## Review Focus

- A Mod name containing spaces, punctuation, or a leading digit must either produce a valid upper-camel Java class name or be rejected before creation; Task 1 pins this with validator tests.
- A ZIP entry such as `../outside.txt` or an absolute path must never write outside the project root; Task 2 pins this with a traversal test.
- A destination containing an existing file must never have that file overwritten or removed during rollback; Task 2 pins this with a pre-existing-file test.
- A starter missing `gradle.properties` or `src/main/java/com/myname/mymodid/MyMod.java` must fail before customization; Task 2 pins this with an incomplete-archive test.
- Network or cancellation failure must skip Gradle linking and leave no downloaded temporary archive; Task 3 keeps linking after successful installation only and closes the temporary-file scope in `finally`.

---

### Task 1: Project metadata validation and starter customization

**Files:**
- Create: `src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhModProjectModel.kt`
- Create: `src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterCustomizer.kt`
- Create: `src/test/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterCustomizerTest.kt`
- Retain: `src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhProjectTemplate.kt`

**Interfaces:**
- Produces: `data class GtnhModProjectModel(val modName: String, val modId: String, val modGroup: String)`.
- Produces: `GtnhModProjectModel.validationError(): String?` and `GtnhModProjectModel.mainClassName(): String`.
- Produces: `GtnhStarterCustomizer.customize(projectRoot: Path, model: GtnhModProjectModel)`.
- Consumes later: Task 2 calls `customize`; Task 3 uses the model and validation methods.

- [ ] **Step 1: Add focused model and customizer tests**

Create tests that build a temporary starter tree with these files:

```text
gradle.properties
src/main/java/com/myname/mymodid/MyMod.java
src/main/java/com/myname/mymodid/ClientProxy.java
src/main/java/com/myname/mymodid/CommonProxy.java
src/main/java/com/myname/mymodid/Config.java
```

Cover valid metadata, blank names, uppercase/invalid Mod IDs, invalid package segments, names beginning with digits, preservation of unrelated properties, package relocation, Java package/proxy replacement, Mod ID/display-name replacement, and `MyMod.java`/class renaming. The central expected case is:

```kotlin
val model = GtnhModProjectModel("Useful Machines", "usefulmachines", "dev.example.usefulmachines")
GtnhStarterCustomizer.customize(root, model)

assertTrue(Files.exists(root.resolve("src/main/java/dev/example/usefulmachines/UsefulMachines.java")))
val source = Files.readString(root.resolve("src/main/java/dev/example/usefulmachines/UsefulMachines.java"))
assertTrue(source.contains("package dev.example.usefulmachines;"))
assertTrue(source.contains("public class UsefulMachines"))
assertTrue(source.contains("MODID = \"usefulmachines\""))
assertTrue(source.contains("name = \"Useful Machines\""))
```

- [ ] **Step 2: Implement the immutable model and exact validation rules**

Use these rules:

```kotlin
private val MOD_ID = Regex("[a-z][a-z0-9_]*")
private val PACKAGE = Regex("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)+")
```

Trim all three values at the wizard boundary. Convert Mod name words to an upper-camel class name by splitting on non-letter/digit characters, capitalizing each nonempty word, joining them, and validating the result with `Character.isJavaIdentifierStart/Part`. Return stable, user-facing validation messages rather than throwing from the wizard.

- [ ] **Step 3: Implement known-placeholder customization**

`GtnhStarterCustomizer.customize` must:

```kotlin
replaceProperty(properties, "modName", model.modName)
replaceProperty(properties, "modId", model.modId)
replaceProperty(properties, "modGroup", model.modGroup)
```

Require each property to occur exactly once. Move the known example package directory to `src/main/java/${model.modGroup.replace('.', '/')}`. For each `.java` file under the moved directory, replace `com.myname.mymodid` with `model.modGroup`; in the main class additionally replace `MyMod`, the literal `mymodid`, and the annotation's `name = "MyMod"`. Rename `MyMod.java` to `${model.mainClassName()}.java`. Reject an existing destination package directory instead of merging into it.

- [ ] **Step 4: Inspect Task 1 changes without executing tests**

Use `git diff --check` and inspect the test fixtures and replacement boundaries. Do not run Gradle or JUnit under the repository instruction.

- [ ] **Step 5: Commit Task 1 files only**

```powershell
git add -- src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhModProjectModel.kt src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterCustomizer.kt src/test/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterCustomizerTest.kt
git commit -m "feat: customize GTNH starter projects"
```

### Task 2: Safe official starter installation

**Files:**
- Create: `src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterInstaller.kt`
- Create: `src/test/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterInstallerTest.kt`

**Interfaces:**
- Consumes: `GtnhModProjectModel` and `GtnhStarterCustomizer.customize(Path, GtnhModProjectModel)` from Task 1.
- Produces: `class GtnhStarterInstaller(private val archiveProvider: (Path) -> Unit = ::downloadStarter)`.
- Produces: `fun install(projectRoot: Path, model: GtnhModProjectModel, checkCanceled: () -> Unit = {})`.
- Consumes later: Task 3 creates a default installer and calls `install`.

- [ ] **Step 1: Add installer tests with in-memory-created ZIP fixtures**

Write fixtures using `ZipOutputStream`. Cover successful extraction/customization, `../outside.txt`, `/absolute.txt`, missing required files, an existing destination file, cancellation after some entries, and rollback behavior. The pre-existing-file assertion must prove its original bytes remain unchanged after failure.

```kotlin
val existing = root.resolve("keep.txt")
Files.writeString(existing, "original")
val installer = GtnhStarterInstaller { target -> Files.copy(fixtureZip, target, StandardCopyOption.REPLACE_EXISTING) }

assertFails { installer.install(root, model) }
assertEquals("original", Files.readString(existing))
```

- [ ] **Step 2: Implement download to a scoped temporary archive**

The default provider uses IntelliJ's `HttpRequests` and the constant URL:

```kotlin
HttpRequests.request(STARTER_URL)
    .productNameAsUserAgent()
    .connect { request -> Files.copy(request.inputStream, target, StandardCopyOption.REPLACE_EXISTING) }
```

`install` creates the temporary archive with `Files.createTempFile`, invokes the provider, and deletes the archive in `finally`, including download and cancellation failures.

- [ ] **Step 3: Implement guarded extraction and rollback accounting**

Resolve every ZIP entry against a newly created staging directory and require the normalized result to start with that staging root. Reject absolute entry names. Validate and customize the complete starter in staging before publishing any file to the project directory.

After extraction, require:

```text
gradle.properties
src/main/java/com/myname/mymodid/MyMod.java
```

Then invoke `GtnhStarterCustomizer.customize` in staging. Publish the staged tree to the project directory without replacing any existing path, tracking each destination file and directory immediately after creation. On any publish failure, delete tracked destination paths in reverse depth order. Delete the staging directory in `finally`. Never register or delete a destination path that existed before this installation attempt.

- [ ] **Step 4: Inspect Task 2 changes without executing tests**

Use `git diff --check`. Manually trace the unsafe-entry, existing-file, cancellation, customization-failure, and successful paths. Confirm the temporary archive is deleted in every branch. Do not execute tests.

- [ ] **Step 5: Commit Task 2 files only**

```powershell
git add -- src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterInstaller.kt src/test/kotlin/dev/gtnh/intellij/generation/project/GtnhStarterInstallerTest.kt
git commit -m "feat: install the official GTNH starter safely"
```

### Task 3: New Project Wizard integration and Gradle linking

**Files:**
- Create: `src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhModNewProjectWizard.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/resources/messages/MyMessageBundle.properties`

**Interfaces:**
- Consumes: `GtnhModProjectModel`, its validation methods, and `GtnhStarterInstaller.install` from Tasks 1 and 2.
- Produces: `class GtnhModNewProjectWizard : GeneratorNewProjectWizard` with ID `GTNHMod`.
- Produces: the `GTNH Mod` entry in the New Project window.

- [ ] **Step 1: Register the supported New Project Wizard extension**

Add under the existing `com.intellij` extensions block:

```xml
<newProjectWizard.generator
        implementation="dev.gtnh.intellij.generation.project.GtnhModNewProjectWizard"/>
```

Keep the existing Java and Gradle plugin dependencies because the wizard uses Java project UI support and Gradle linking.

- [ ] **Step 2: Implement the generator and wizard chain**

Follow the supported JetBrains structure:

```kotlin
class GtnhModNewProjectWizard : GeneratorNewProjectWizard {
    override val id = "GTNHMod"
    override val name = "GTNH Mod"
    override val icon: Icon = /* existing GTNH plugin icon or stable platform icon */

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::newProjectWizardBaseStepWithoutGap)
            .nextStep(::MetadataStep)
}
```

Do not add `GitNewProjectWizardStep`, because automatic Git initialization is outside scope.

- [ ] **Step 3: Bind and validate the three metadata fields**

`MetadataStep` extends `AbstractNewProjectWizardStep`. Define three `GraphProperty<String>` values and bind them with Kotlin UI DSL text fields labeled `Mod name`, `Mod ID`, and `Mod group`. Use `validationOnInput` and `validationOnApply`; show the first relevant message returned by the model validator. Seed Mod name from the standard project name, while leaving Mod ID and Mod group explicit for the user.

- [ ] **Step 4: Install with progress and link Gradle only on success**

In `setupProject(project)`, resolve `context.projectFileDirectory` as the generation root, create the trimmed model, and run `GtnhStarterInstaller.install` through IntelliJ's cancellable progress API. Pass `ProgressManager.checkCanceled()` as the cancellation callback.

After `install` returns successfully, link the generated root with the public 2026.1 API:

```kotlin
GradleOpenProjectProvider().linkToExistingProject(context.projectFileDirectory, project)
```

Do not invoke a Gradle task. Convert download, ZIP, validation, and I/O failures into a `ConfigurationException` or wizard error with a concise phase-specific message; do not link on failure.

- [ ] **Step 5: Add user-facing message keys**

Add stable bundle entries for generator name, field labels, validation failures, progress title, and download/extraction/customization error prefixes. Reuse them from the wizard where the platform API accepts localized text.

- [ ] **Step 6: Inspect Task 3 changes without building or running the IDE**

Compare imports, extension-point spelling, wizard method signatures, and Gradle-link method signatures against the 2026.1 SDK classes already present in the project. Use `git diff --check` and XML inspection. Do not run build, tests, or `runIde`.

- [ ] **Step 7: Commit Task 3 files only**

```powershell
git add -- src/main/kotlin/dev/gtnh/intellij/generation/project/GtnhModNewProjectWizard.kt src/main/resources/META-INF/plugin.xml src/main/resources/messages/MyMessageBundle.properties
git commit -m "feat: add GTNH mod project wizard"
```

### Task 4: Final static review and handoff

**Files:**
- Review: all files created or modified by Tasks 1-3
- Preserve: `gradle.properties` user change to `pluginVersion=1.1.0`

**Interfaces:**
- Consumes: all completed tasks.
- Produces: a reviewable feature branch state with no unrelated files staged.

- [ ] **Step 1: Review the complete diff against the approved spec**

Confirm the final diff covers wizard visibility, all three inputs, live validation, official starter download, safe extraction, exact renames, rollback, cancellation, error reporting, and Gradle linking.

- [ ] **Step 2: Inspect repository state**

Run read-only/status checks:

```powershell
git diff --check
git status --short
git log -4 --oneline
```

Ensure `gradle.properties` remains an unstaged user-owned change unless the user separately asks to include it.

- [ ] **Step 3: Report verification limits**

State explicitly that tests, compilation, and IDE launch were not run because the repository's `AGENTS.md` instruction forbids those commands without an explicit user request. List the unexecuted targeted test classes and the manual IDE check that remains: open New Project, select GTNH Mod, create a sample, and observe Gradle linking.
