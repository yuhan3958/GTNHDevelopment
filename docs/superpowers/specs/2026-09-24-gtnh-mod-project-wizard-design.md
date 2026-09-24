# GTNH Mod Project Wizard Design

## Goal

Add a **GTNH Mod** entry to IntelliJ IDEA's New Project window. The wizard creates a ready-to-edit Minecraft 1.7.10 mod project from GT New Horizons' official project starter and applies the mod metadata entered by the user.

The generated project must preserve the official starter structure while replacing its example identity consistently. Project creation requires network access because the plugin downloads the current official starter at creation time.

## User Experience

The New Project window offers **GTNH Mod** as a project generator. It uses IntelliJ's standard project name and location controls and adds three required fields:

- **Mod name**: the human-readable name written to `modName`.
- **Mod ID**: the lowercase identifier written to `modId`.
- **Mod group**: the Java package written to `modGroup`.

The wizard validates all fields before enabling project creation. Mod name must be nonblank, Mod ID must be a valid lowercase mod identifier, and Mod group must be a valid Java package. Validation errors appear beside the relevant wizard field.

When the user creates the project, IntelliJ shows background progress while the plugin downloads and prepares the starter. On success, the generated directory is opened and linked as a Gradle project through the normal IntelliJ project-import flow.

## Template Source

The generator downloads the official moving starter archive:

`https://github.com/GTNewHorizons/ExampleMod1.7.10/releases/download/master-packages/starter.zip`

Using the moving official archive keeps newly generated projects aligned with GTNH's maintained build setup. The plugin does not bundle a fallback copy because a bundled archive would silently become stale and would increase the plugin artifact size.

## Components

### Wizard registration

A New Project Wizard generator registered in `plugin.xml` provides the GTNH Mod entry, metadata fields, field validation, and generation handoff. It follows the IntelliJ Platform New Project Wizard API available to the plugin's supported IDE range.

### Starter downloader

A focused downloader retrieves the archive into a temporary file using IntelliJ's platform HTTP facilities. It follows redirects and reports HTTP, connectivity, and cancellation failures as project-creation errors.

### Starter extractor

The extractor expands regular files and directories into the requested project directory. Every ZIP entry is normalized and checked to remain below the destination directory to prevent path traversal. The archive must contain `gradle.properties` and the example Java package before customization begins.

### Project customizer

The customizer receives a model containing Mod name, Mod ID, and Mod group. It:

1. Updates only the `modName`, `modId`, and `modGroup` assignments in `gradle.properties`, preserving all other starter settings.
2. Derives a Java class name from Mod name by retaining identifier characters and converting word boundaries to upper camel case. The result must be a valid Java identifier; otherwise wizard validation rejects the input.
3. Moves `src/main/java/com/myname/mymodid` to the directory represented by Mod group.
4. Updates Java package declarations and fully qualified example package references throughout the moved Java sources.
5. Renames `MyMod.java` and the `MyMod` class/references to the derived class name.
6. Replaces the example display name and `mymodid` constant in the main mod class.

Text replacement is deliberately limited to known starter placeholders and Java source files. Binary files and unrelated text are not modified.

## Generation Flow

1. IntelliJ validates the standard project path and the three GTNH metadata fields.
2. Project creation starts a cancellable background task.
3. The starter archive is downloaded to a temporary file.
4. The archive is validated and extracted into the new project directory.
5. Metadata and Java sources are customized.
6. The temporary archive is deleted.
7. The generated directory is opened and its Gradle build is linked.

No Git repository is initialized automatically. The starter's generated project files remain otherwise unchanged.

## Failure Handling

Generation stops with a clear error when the archive cannot be downloaded, is malformed, lacks required starter files, contains an unsafe entry, or cannot be customized. Cancellation also stops generation promptly.

The generator writes only inside the selected new-project directory. If failure occurs after files have been created, it removes only files and directories created by the current generation attempt. It does not delete a directory that existed before creation or any pre-existing contents.

Errors include enough context to distinguish download, extraction, and customization failures. The plugin never continues to Gradle import after a partial generation.

## Verification

Automated tests should cover the pure validation and customization behavior with a small fixture archive:

- metadata fields accept and reject the intended values;
- `gradle.properties` retains unrelated settings while changing the three requested keys;
- package directories, package declarations, proxy class names, Mod ID, display name, and main class are renamed consistently;
- unsafe ZIP paths and incomplete starter archives are rejected;
- cleanup touches only paths created by the generator.

The New Project Wizard registration and Gradle-link call should be reviewed against the supported IntelliJ Platform API. Per repository instructions, implementation work will not automatically run build, test, or application commands unless the user explicitly requests them.

## Out of Scope

- Selecting a fixed starter version or branch.
- Offline project creation.
- License authoring or renaming `LICENSE-template`.
- Optional dependency, Mixin, or access-transformer configuration.
- Git initialization or remote repository creation.
- Running Gradle setup or build tasks after generation.
