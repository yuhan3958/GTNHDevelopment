# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.2.0] - 2026-09-23

### Added

- Automatic GTNH project detection using GTNH Convention, RetroFuturaGradle, and `GTNHGradle` markers
- A GTNH tool window for running `runClient`, `runServer`, version-specific server tasks, `build`, and `spotlessApply`
- An editor action that finds Mixin classes associated with a configuration field declaration or reference
- The <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>G</kbd> shortcut and an editor context-menu entry for Mixin navigation
- Analysis of conditional statements, conditional expressions, builder call chains, and lambda-based Mixin registration patterns
- `@Mixin` class resolution from class literals, PSI references, and string class names
- A navigation popup that displays registration types and packages when multiple Mixins are found
- An IDE notification when no matching Mixin registration is found

### Changed

- Mixin usage searches now run as background read actions to keep the IDE responsive
- Mixin resolution now prefers classes from the current module when several candidates share a name
- The project now targets IntelliJ IDEA 2026.1 (`261.*`) and Java 21

### Fixed

- Fixed Kotlin compilation against the IntelliJ 2026.1 API when resolving class types
- Replaced the PSI-only list renderer used with non-PSI navigation targets, fixing generic-bound and invalid-override errors

[Unreleased]: https://github.com/GTNewHorizons/GTNHDevelopment/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/GTNewHorizons/GTNHDevelopment/releases/tag/v0.2.0
