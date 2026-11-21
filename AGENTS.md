# AGENTS.md

Instructions for coding agents in this repository. The user owns Git history.

## Git: never commit

This rule overrides every other file. If `GUIDE.md`, a section file, a task
description, or an automation prompt says to commit, it means "leave the work
for the user"; it never authorizes an agent commit.

- Do **not** run `git commit`, `git add`, `git stash`, `git reset`,
  `git checkout --`, or `git restore`.
- Do **not** use the agent `commit` namespace.
- Do **not** create a todo item, plan step, or acceptance criterion for
  committing.
- Do **not** amend, rebase, squash, tag, or rename branches.
- Leave all work in the working tree. At the end, report the changed paths and
  a suggested commit subject/body; the user commits.
- Read-only Git commands (`status`, `diff`, `log`, `show`) are allowed.
- The branch is `master`. Do not rename it to `main`.
- Never add build output, `.gradle/`, `.kotlin/`, or IDE metadata to the tree.

## Context budget

- Read `GUIDE.md`, then exactly one `docs/guide/NN-*.md` for the section being
  implemented. Do not read sibling section files.
- Implement only that section's deliverables. Do not pre-implement later
  sections; add a private seam only when the current section needs a hook.
- A section is done only when every `## Acceptance` box is demonstrated, not
  inferred. Update the section checkboxes and the `GUIDE.md` progress entry in
  the same pass.
- If a user instruction conflicts with the guide, the user wins. Remove the
  stale requirement from the guide and the docs it affects.
- The library, examples, docs, API audit, and release scaffolding are present.
  There is no v1 migration section.

## Project facts

- Coordinates: `com.psiwray.organization:terminal-window-gui`; development
  version `1.0.0-SNAPSHOT`, release `1.0.0`.
- Package root: `com.psiwray.organization.terminal_window_gui`.
- Automatic module name: `com.psiwray.organization.terminal_window_gui`.
- Modules: `:library` (published) and `:examples` (proof only). No
  `:benchmarks`.
- Toolchain: JDK 25, Kotlin 2.4.21, Gradle 9.9.1, JUnit 6.1.3, Dokka 2.3.0,
  ktlint 1.8.0, Spotless 8.11.0, nmcp 1.6.2.
- Host dependency: `com.psiwray.organization:terminal-window:2.0.0`.
- License: MIT. Bundled font: CC0-1.0 with attribution. No GPL v1 source.
- No `terminal-window` changes in 1.x. The host owns GLFW, GL, the window, and
  the renderer.
- Maven Central versions are immutable: a broken release is a new version,
  never a re-upload.

## Architecture invariants

- `GuiSurface` is passive. The caller owns the window, service, owner thread,
  event delivery, paint scheduling, timers, close, and GL. The library never
  calls `TerminalWindow` or `TerminalWindowService`.
- No library thread, timer thread, coroutine, background repaint, or hidden
  event loop. `tick(nowNanos)` runs only due timers.
- `GuiSurface` and attached widgets are single-thread-affine. No internal
  synchronization.
- Root and child bounds are local to the parent content box. `PaintContext`
  translates to absolute screen cells with an origin/clip stack. Overlays and
  pointer hit testing are the screen-coordinate boundary.
- One measure path: `measure(Constraints) -> IntSize`. Preferred size is
  computed. No mutable `preferredSize`, `minimumSize`, or `maximumSize`.
- Widgets request semantic `Shape` roles. Resolution is primary, per-shape
  fallback, global last resort, then `MissingShapeException`. No Private Use
  Area code points, emoji presentation, or hardcoded glyphs.
- The bundled font is `terminal-window-gui-8x16.twf`, CC0-1.0, ASCII plus
  every v1 primary, the reserved heavy set, and ASCII fallbacks.
- One code point per cell. No attributes, wide characters, grapheme clusters,
  IME, images, OS clipboard, runtime font switching, RTL, or accessibility in
  v1.
- Public signatures never expose `org.lwjgl.*`, GLFW, OpenGL, window handles,
  or service types. Host value types are allowed only at the caller boundary.

## Kotlin and API conventions

- `explicitApi()` is enabled; every public declaration has explicit visibility
  and types.
- No `!!` in library code. No coroutines. Use `internal` for implementation.
- Immutable value types are `data class`; stateless helpers are `object`.
  Public data classes and sealed hierarchies are frozen shapes.
- Public declarations need KDoc. No public inline functions. Default arguments
  and nullability are ABI. `explicitApi()` is the source gate; do not weaken
  `ApiSurfaceTest` to hide an internal/LWJGL leak.
- Colors are Int ARGB theme tokens. Do not store per-cell attributes.
- Layout, paint, and text measurement use indexed loops and primitives on hot
  paths; avoid per-cell allocation, boxing, and capturing lambdas.
- Use `use {}` only for `Closeable`; the GUI has no native handles to close.
- Style: UTF-8, LF, 4 spaces, max line length 120, `ktlint_official`.

## Build and verification

- Use the wrapper only: `./gradlew <task>`.
- `./gradlew build` is the baseline.
- `./gradlew minimumAcceptanceTest` is the default acceptance run. Long tests
  are tagged `long`; `./gradlew fullTest` runs them. Do not put long tests in
  the default suite.
- `ApiSurfaceTest` freezes the public type inventory; `WidgetConformanceTest`
  freezes the public concrete widget registry. Update the inventory document,
  test resource, and registry together when the user approves an API change.
- `bash ci/build.sh` runs Spotless, tests, examples, Dokka, local publication,
  and `ci/check-module.sh`; `ci/check-docs.sh` runs before Gradle.
- `./gradlew -q projects` must list `:`, `:library`, `:examples` and no
  benchmark module.
- `./gradlew -q javaToolchains` must show JDK 25.
- GL tests are tagged `gl`, run under Xvfb via `ci/integration-test.sh`, and
  assert `drawCalls > 0`; no screenshot diffing.
- Golden tests compare structural rows and theme token names.
  `-PupdateGoldens` is local only; CI never updates goldens.
- No timing assertions, no benchmark module, and no performance table.
- Verify the surface that changed. If a display surface cannot be run, report
  that explicitly.
- Do not rerun a check to confirm a failure the user already reported. Treat
  the report as ground truth.

## Documentation

- ADRs in `docs/adr/` are decision records. Do not rewrite an accepted decision
  unless the user changes it; note a pre-release revision in the ADR.
- Update the owning page under `docs/`; do not duplicate a contract in several
  files.
- Keep `docs/compatibility.md`, `README.md`, and `CHANGELOG.md` in sync with
  user-visible changes.
- `README.md` is at most 150 lines and links only to existing files. Code
  blocks must compile or be marked `text`; the quickstart is compile-checked.
- Do not document window ownership, an application loop, background repaint,
  threads, text attributes, wide characters, IME, images, or OS clipboard as
  v1 features.
- Do not describe v1.1 items as "MVP" or "follow-up" in a way that implies
  stubs.
- Do not add a performance table or benchmark claims.

## Release

- Releases are local: release branch, signed commit/tag, `publishToMavenLocal`,
  signature verification, Central Portal upload, manual publish, clean
  consumer check.
- CI never receives a signing key or Central token. Do not add publishing
  secrets to workflows.
- The release branch is `release/1.0` for 1.0.x; `master` advances to
  `1.1.0-SNAPSHOT` after `v1.0.0`.
- Do not tag, sign, upload, or publish unless the user explicitly asks.
