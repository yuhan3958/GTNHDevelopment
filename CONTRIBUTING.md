# Contributing to GTNH Development

## Development setup

Use Java 21 and the included Gradle Wrapper. Minecraft Development is a required dependency and is resolved by the IntelliJ Platform Gradle Plugin.

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
.\gradlew.bat runIde
```

Do not commit `.idea`, `.gradle`, `.intellijPlatform`, `build`, or `.superpowers` output.

## Architecture

- Services aggregate results and coordinate background work.
- Providers understand syntax, repository conventions, or classification rules.
- UI classes render immutable results and do not contain project-wide search logic.
- Long-lived state stores plain values or smart PSI pointers, never raw invalidatable PSI.
- Version-sensitive Minecraft Development, Gradle, Git, and IntelliJ calls belong behind small adapters in `compat/`.

Production code must not contain Hodgepodge-specific packages, config classes, enum names, or APIs. Repository-specific fixtures may be used only in tests.

## PSI and performance

Prefer PSI and symbol resolution over source text. Text inspection is acceptable only for explicitly heuristic evidence such as ASM context or resource formats. Never search the raw filesystem for Java relationships.

Project-wide searches must run outside the EDT, call cancellation checks in large loops, respect dumb mode, and use project or module scopes. `AnAction.update()` and line-marker collection must remain cheap.

## Providers

Mixin links use the shared `GtnhMixinLink` model in both directions. Patch providers must label resolved evidence `EXACT` and uncertain string evidence `HEURISTIC`. New providers should fail gracefully on unresolved or incomplete PSI and must not guess among ambiguous symbols.

## Compatibility

The supported range is IntelliJ build `251` through `262.*`. Avoid build-number checks in feature code. Add or update a focused compatibility adapter when an API boundary changes.

The compatibility workflow builds representative IntelliJ 2025.1, 2025.3, and 2026.2 combinations. Override local baselines with:

```powershell
.\gradlew.bat test -PplatformVersion=2025.3.4 -PminecraftDevVersion=2025.3-1.8.17
```

## Tests

Add focused regression tests for every behavior change. Important fixture categories include:

- forward and reverse Config/Mixin links
- ambiguous string registrations and broken PSI
- Mixin, ASM, and Access Transformer patch evidence
- Environment Doctor failure isolation
- dependency classification without substring false positives
- truthful PR drafts
- generation validation and collision handling

Run `test` and `buildPlugin` after each milestone. Run `verifyPlugin` before release. Treat critical verifier findings as release blockers.

## Safe changes

Environment diagnostics must not change SDKs, Gradle JVMs, remotes, or build scripts. Multi-file generation requires a preview and explicit confirmation. Human claims such as DevEnv or Fullpack testing must never be inferred.

## Git history

Keep commits atomic and reviewable. Separate tests, domain changes, UI integration, and release metadata when practical. Do not rewrite or discard unrelated contributor work.

## Pull requests

Describe observed testing accurately. Do not mark human-only checklist items unless you performed them. Link or close an issue only when the relationship is explicit.
