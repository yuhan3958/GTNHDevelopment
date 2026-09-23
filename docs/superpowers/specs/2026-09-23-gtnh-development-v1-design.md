# GTNH Development v1.0 Design

## Purpose

GTNH Development is a GTNewHorizons-specific development plugin built on the required Minecraft Development IntelliJ plugin. Minecraft Development remains responsible for generic Minecraft, Forge, and Mixin semantics. GTNH Development provides GTNH project detection, build conventions, Config/Mixin relationships, patch-impact analysis, environment diagnostics, ecosystem navigation, contribution workflows, and safe generation.

Version 1.0 must support structurally different GTNH repositories. Hodgepodge may be an integration-test target, but no production identifier, package, or API may depend on it.

## Scope and Release Strategy

- Support IntelliJ IDEA 2025.1 through 2026.2 (`251` through `262.*`).
- Publish one plugin binary across that range whenever compatibility permits.
- Compile against the oldest stable APIs needed by the range.
- Verify representative IntelliJ and Minecraft Development combinations in CI and Plugin Verifier.
- Add small compatibility adapters, or separate artifacts only when a stable common API cannot be used.
- Preserve working v0.1 and v0.2 behavior and continue incrementally from v0.3.
- Do not begin a later milestone while the current milestone fails compilation or tests.

## Core Principles

1. Do not reimplement generic Minecraft, Forge, or Mixin functionality owned by Minecraft Development.
2. Do not hardcode Hodgepodge or any other repository identifier in production code.
3. Do not parse Java with regular expressions. Use PSI and symbol resolution.
4. Services aggregate providers, propagate cancellation, merge results, and remove duplicates.
5. Providers understand PSI syntax and repository patterns.
6. Isolate unavoidable repository-specific support in optional providers.
7. Never run project-wide searches, Gradle queries, or Git operations on the EDT.
8. Never silently choose one target for an ambiguous Mixin string.
9. Long-lived UI and cache state must not retain invalidatable PSI directly.
10. Isolate compatibility behavior behind small adapters instead of scattering version checks.
11. Prefer navigation and diagnosis over automatic mutation.
12. Degrade safely with incomplete PSI, indexing, or Gradle synchronization.

## Package Structure

```text
dev.gtnh.intellij
├─ action/          User entry-point actions
├─ compat/          IntelliJ and dependency compatibility adapters
├─ config/          Config-field detection and plugin configuration
├─ contribution/    Git state, checks, and PR drafts
├─ ecosystem/       GTNH dependencies and API navigation
├─ environment/     Independent environment checks
├─ generation/      Preview-based generation operations
├─ gradle/          Gradle models, task discovery, and execution
├─ mixin/           Bidirectional Config/Mixin model and service
├─ mixin/provider/  Registration syntax and structure providers
├─ patch/           Patch-impact model and service
├─ patch/provider/  Mixin, ASM, and Access Transformer providers
├─ project/         GTNH project detection
├─ settings/        Application and project settings
└─ ui/              Tool Window, choosers, and result trees
```

Each type and file has one clear responsibility. Services contain no syntax-recognition rules, and UI code contains no search algorithms.

## Service and Provider Boundaries

Services manage provider collections, execute them with cancellation, merge and deterministically deduplicate results, expose safe cached results, and present stable APIs. Providers recognize PSI syntax or repository structure, produce shared result models with evidence, report kind and confidence, and tolerate broken PSI.

Services must not contain method names, package names, enum names, or repository names. Generic structural heuristics belong inside providers. Unavoidable repository knowledge belongs in an optional provider isolated from the core.

## PSI Lifetime and Caching

- PSI may be used directly inside short-lived background read actions.
- UI models, long-lived services, and caches store `SmartPsiElementPointer` values or stable identifiers.
- Cache invalidation uses `PsiModificationTracker`, `CachedValue`, or equivalent IntelliJ mechanisms.
- Invalid pointers discard cached results and trigger recomputation.
- Gutter collection uses cached data or low-cost same-file analysis only.
- Line-marker collection never initiates project-wide searches.

## GTNH Project Detection

`GtnhProjectDetector` combines independent signals: GTNH convention plugins, RetroFuturaGradle, GTNHGradle infrastructure, GTNH ecosystem dependencies, known GTNH metadata, and a future project-level manual override.

The existing build-script text check may remain as an early signal but must be extended with Gradle project-model information. Detection must not depend on one filename or plugin ID. Signals carry enough confidence to avoid false positives.

## Gradle Integration

- Use IntelliJ External System and Gradle APIs.
- Discover available tasks from the real Gradle model.
- Treat client/server, Java-version-specific server, build, test, check, and formatting tasks as semantic candidates, not guaranteed names.
- Show only discovered tasks.
- Execute through IntelliJ's Gradle infrastructure.
- Perform discovery and execution preparation off the EDT.

## Shared Config/Mixin Link Graph

Forward and reverse navigation use the same model and providers.

```kotlin
data class GtnhMixinLink(
    val configFields: List<PsiField>,
    val controlExpression: PsiElement?,
    val registrationElement: PsiElement?,
    val mixinClass: PsiClass,
    val kind: GtnhMixinLinkKind,
)

interface GtnhMixinLinkProvider {
    fun findByConfig(field: PsiField): List<GtnhMixinLink>
    fun findByMixin(mixinClass: PsiClass): List<GtnhMixinLink>
}
```

Any representation retained beyond its read action converts PSI to smart pointers.

Supported structures include builder chains with condition lambdas; registrations within `if` and conditional expressions; conditions and class literals in one call; string FQNs, context-relative names, and simple names; structures involving `PsiEnumConstant`, `PsiNewExpression`, and `PsiExpressionList`; and conditions containing multiple configuration fields.

String resolution order:

1. Exact FQN.
2. Names derived from source-package context.
3. Simple-name lookup through `PsiShortNamesCache`.
4. Prefer classes whose Sponge `@Mixin` annotation resolves.
5. Prefer current-module and current-project candidates.
6. Return every valid candidate if ambiguity remains.

Mixin detection resolves the annotation type to `org.spongepowered.asm.mixin.Mixin` whenever possible. Annotation text alone is insufficient.

## Config/Mixin User Flows

```text
PsiField at caret
  -> background ReferencesSearch
  -> link providers
  -> service aggregation
  -> direct navigation or native chooser

@Mixin PsiClass around caret
  -> class references and indexed string usages
  -> reverse analysis by the same providers
  -> all controlling PsiFields
  -> direct navigation or native chooser
```

Zero results produce a non-modal notification. One result navigates directly. Multiple results use `JBPopupFactory.createPopupChooserBuilder`. `AnAction.update()` inspects only caret-local PSI and cheap project state.

## Patch Impact Explorer

The shared model represents target, evidence, kind, and confidence.

```kotlin
enum class GtnhPatchConfidence { EXACT, HEURISTIC }

interface GtnhPatchProvider {
    fun findPatches(target: GtnhPatchTarget): List<GtnhPatch>
}
```

- `MixinPatchProvider` resolves `@Mixin`, injection annotations, overwrite, shadow, and visible MixinExtras annotations. Symbol-backed findings are normally `EXACT`.
- `AsmTransformerPatchProvider` conservatively relates transformers, ASM visitors, and target-class strings. Findings are `HEURISTIC`.
- `AccessTransformerPatchProvider` discovers AT files and relates class/member entries. Incomplete parsing produces `HEURISTIC` findings.

Stable public Minecraft Development APIs are used through an adapter where useful. Ordinary IntelliJ PSI is the fallback when no stable API exists.

## Environment Doctor

Each check is independent.

```kotlin
enum class GtnhCheckStatus { PASS, WARNING, ERROR, UNKNOWN }
```

Checks cover GTNH detection, Minecraft Development, Project SDK, Gradle JVM, Java toolchains, Gradle wrapper, GTNH build infrastructure, discovered tasks, Git repository, upstream remote, run configurations, and obvious Java or Gradle incompatibilities.

Each result contains an explanation, evidence, and optional navigation or safe-fix proposal. A mutation runs only through an explicit user action.

## GTNH Ecosystem Navigation

- Detect ecosystem libraries through Gradle project and dependency models.
- Display resolved versions and module usage.
- Open sources and find project examples, implementations, and similar usages.
- Core navigation works without internet access or automatic cloning.
- Never assume every repository uses every GTNH library.

## Pull Request Assistant

- Read branch, working tree, remotes, ahead/behind state, and diff summaries.
- Let users run discovered build, test, check, and formatting tasks.
- Generate drafts only from diffs, commits, user-provided issue data, and observed results.
- Require user confirmation for DevEnv testing, Fullpack testing, and AI-policy acknowledgement.
- Never claim an unexecuted test passed.
- Never generate `Fixes #...` without explicit issue association.
- Core v1.0 requires no GitHub authentication and does not submit PRs.

## Project and Code Generation

- Supplement Minecraft Development's generic generators.
- Collect mod metadata, package, convention choices, and optional dependencies in the project wizard.
- Reuse stable Minecraft Development generation APIs where available, adding only GTNH glue.
- Modify existing registries only when repository conventions are recognized with high confidence.
- Require diff preview and confirmation for every multi-file change.
- With low confidence, generate only safe new files or snippets.

## Settings and Compatibility

Settings under Tools > GTNH Development control automatic detection, project override, gutter icons, Config/Mixin navigation, Patch Impact heuristics, Environment Doctor, PR Assistant, and first-detection notifications.

Compatibility differences belong behind small interfaces in `compat`. Feature code never compares IDE build numbers directly. Adapter selection occurs in one location.

## Tool Window UX

```text
GTNH
├─ Overview
├─ Run
├─ Code
├─ Environment
└─ Contribution
```

Overview summarizes project, Minecraft, Java, and dependencies. Run displays discovered tasks only. Code exposes Config/Mixin links and Patch Impact. Environment presents diagnostics and explicit actions. Contribution presents Git state, checks, and an editable PR draft.

Hide unavailable controls or show a short reason. Use native choosers and navigatable PSI. Long-running work provides progress and cancellation.

## Concurrency, Error Handling, and Diagnostics

- Run project-wide PSI searches in cancellable non-blocking read actions.
- Run Gradle and Git operations in background tasks.
- Disable features in dumb mode or defer safely until smart mode.
- Check cancellation in long iterations.
- Treat unresolved PSI and broken source as empty results or `UNKNOWN`.
- Failure to recognize one structure must not invalidate other provider results.
- Log unexpected failures with diagnostic context without exposing stack traces to users.
- User-facing errors remain short and actionable.

## Testing Strategy

Unit and fixture tests cover multiple project-detection signals and false positives; builder, conditional, and direct Config/Mixin links; class-literal and string registrations; duplicate simple names; ordinary-reference false positives; multiple config fields; missing registrations; broken PSI; service deduplication; invalid smart pointers; patch confidence; provider isolation; environment status; PR truthfulness; generation preview; and low-confidence refusal.

Integration and compatibility tests use Hodgepodge only as a generic integration target, exercise structurally different GTNH repositories where available, build on IntelliJ 2025.1, an intermediate release, and 2026.2, pair compatible Minecraft Development versions, and run Plugin Verifier across `251–262.*`.

## Documentation

README documents purpose, required dependencies, installation, supported IDEs, project detection, Gradle integration, bidirectional Config/Mixin navigation, Patch Impact, Environment Doctor, ecosystem navigation, PR Assistant, generation, and troubleshooting.

`CONTRIBUTING.md` documents development setup, test matrices, provider rules, compatibility adapters, and release verification. `CHANGELOG.md` records user-facing milestone changes.

## Milestones

### v0.3

Review incomplete reverse navigation. Migrate links to multiple config fields, make providers genuinely bidirectional, replace raw-PSI caches with smart-pointer-backed state, implement a conservative gutter, and add PSI fixture tests.

### v0.4

Implement Patch Impact models, aggregation, Mixin analysis, conservative ASM and Access Transformer providers, and result UI.

### v0.5

Implement Environment Doctor foundations and dynamic Gradle task discovery, including SDK, Gradle JVM, toolchain, wrapper, task, and basic Git checks.

### v0.6

Implement Gradle-model-based ecosystem dependency detection and project-local API/example navigation.

### v0.7

Implement Git and Gradle state collection, human-confirmed checklist items, and fact-based PR drafts. PR submission remains out of scope.

### v0.8

Implement GTNH project and Mixin/config generators with preview-first changes. Do not modify existing source when convention confidence is low.

### v0.9

Complete settings, compatibility adapters, PSI cache invalidation, multi-module scopes, diagnostics, first-run notification, performance hardening, and CI compatibility matrices.

### v1.0

Unify the Tool Window and finish documentation. Run regression tests, supported-version builds, Plugin Verifier, representative-repository checks, and a production-identifier audit.

Each milestone receives its own specification and implementation plan and ends with passing tests and builds in an atomic commit series.

## Definition of Done

- Minecraft Development is required in Gradle and `plugin.xml`.
- Automated verification covers IntelliJ 2025.1 through 2026.2.
- Config/Mixin navigation uses the same provider-based graph in both directions.
- Patch Impact provides reliable Mixin findings and labels heuristics.
- Environment Doctor never silently mutates user configuration.
- PR Assistant never invents test results or human-only claims.
- Generators preview multi-file changes before applying them.
- Project-wide work never blocks the EDT.
- Invalid PSI and indexing never crash the plugin.
- Production code contains no identifiers tied to one GTNH repository.
- README, CONTRIBUTING, CHANGELOG, installation, and troubleshooting documentation support external contributors.
