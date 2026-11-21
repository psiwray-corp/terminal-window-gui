# 02 — Repository, build, quality gates, CI, release scaffolding

**Phase:** foundations · **Depends on:** 01 · **Blocks:** 03

## Goal

Bootstrap the Gradle project, quality gates, CI wrappers, and release script
scaffolding. No widget implementation code.

## Context budget

Read: `GUIDE.md`, this file, and `terminal-window` `docs/guide/02-repository-bootstrap.md`,
`03-dependencies-and-natives.md`, `04-quality-gates.md`, `05-ci-pipeline.md`,
plus its actual `settings.gradle.kts`, root `build.gradle.kts`,
`gradle.properties`, `gradle/libs.versions.toml`, `library/build.gradle.kts`,
`ci/*.sh`, `.github/workflows/build.yml`, `.forgejo/workflows/build.yml`,
`scripts/release*.sh`, and `scripts/verify-signatures.sh`. Expected output:
build and CI files; no Kotlin widgets.

## Deliverables

- `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`.
- `gradle/libs.versions.toml`, `gradle/wrapper/`, `gradlew`, `gradlew.bat`.
- `.editorconfig`, `.gitattributes`, `.gitignore`.
- `library/build.gradle.kts`.
- `examples/build.gradle.kts` (module scaffold only; run tasks in section 13).
- `ci/build.sh`, `check-docs.sh`, `check-jdk.sh`, `full-test.sh`,
  `install-gl-deps.sh`, `integration-test.sh`.
- `.github/workflows/build.yml`, `.forgejo/workflows/build.yml`.
- `scripts/release-branch.sh`, `release-prepare.sh`, `release-tag.sh`,
  `release.sh`, `verify-signatures.sh`.
- Minimal publication configuration for `publishToMavenLocal`; section 15
  finalizes POM metadata, signing, and nmcp.

## Steps

1. **Wrapper.** Copy `terminal-window`'s Gradle 9.9.1 wrapper and pinned
   distribution checksum. Keep `gradlew.bat` CRLF via `.gitattributes`
   (`*.bat text eol=crlf`).
2. **Version catalog.** Pin Kotlin `2.4.21`, JUnit `6.1.3`, Dokka `2.3.0`,
   ktlint `1.8.0`, Spotless `8.11.0`, `terminal-window` `2.0.0`, LWJGL
   `3.4.3` for examples. No JOML direct dependency; it arrives transitively.
3. **Settings.**
   - `rootProject.name = "terminal-window-gui"`.
   - `include(":library", ":examples")`; no `:benchmarks`.
   - `dependencyResolutionManagement { repositories { mavenCentral() } }`.
   - Copy `nmcp` settings (`com.gradleup.nmcp.settings` 1.6.2) with
     `publicationName = "terminal-window-gui:${version}"`, `USER_MANAGED`,
     and credentials from `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD`.
     Section 15 finalizes the Portal flow.
4. **Root build.**
   - Apply Kotlin JVM and Spotless plugins (`apply false` where appropriate).
   - Compute `hostOs`, `hostArch`, and `lwjglNativesClassifier` exactly as
     `terminal-window` does; expose it as `extra["lwjglNativesClassifier"]`.
   - Expose `extra["runtimeJvmArgs"]`:
     `-Djava.awt.headless=true`, `--enable-native-access=ALL-UNNAMED`, and
     `-XstartOnFirstThread` on macOS.
   - Set `group` and `version` from `gradle.properties`.
   - Configure Spotless for Kotlin and Kotlin Gradle files with ktlint.
   - Register aggregators:
     - `integrationTest` -> `:library:integrationTest`;
     - `fullTest` -> `:library:fullUnitTest`, `:library:fullIntegrationTest`;
     - no benchmark task.
5. **Library build.**
   - Plugins: Kotlin JVM, `java-library`, `maven-publish`, Spotless, Dokka,
     Dokka Javadoc. Signing is added in section 15.
   - `base { archivesName = "terminal-window-gui" }`.
   - `java { withSourcesJar() }`.
   - `kotlin { jvmToolchain(25); explicitApi(); compilerOptions { jvmTarget =
     JVM_25; allWarningsAsErrors = true; extraWarnings = true } }`.
   - Dependencies: `api(libs.terminal.window)`,
     `implementation(kotlin("stdlib"))`, JUnit/Kotlin test.
   - No LWJGL or native classifier configuration in the library dependency
     graph. The published POM must never contain natives.
   - Test suite: default `test` excludes `long`; `integrationTest` includes
     `gl` and excludes `long`; `fullUnitTest` and `fullIntegrationTest` run
     everything. `integrationTest` runs with `maxParallelForks = 1`.
   - `minimumAcceptanceTest` (optional alias) depends on `test` and
     `integrationTest`; `fullTest` depends on the full suites.
   - Dokka: fail on warning, document public visibility, report undocumented.
   - Manifest: `Automatic-Module-Name:
     com.psiwray.organization.terminal_window_gui` (finalized in section 14).
   - Publication: `mavenJava` from `components["java"]`, artifactId
     `terminal-window-gui`, source jar, plus a local `build/repo` repository.
     POM license/developer/SCM metadata is completed in section 15.
6. **Examples build.**
   - Kotlin JVM module, depends on `:library`, `libs.terminal.window`, and
     host LWJGL natives as `runtimeOnly`.
   - Extend `runtimeClasspath` with a resolvable-only `natives`
     configuration.
   - Use `runtimeJvmArgs` for test and run tasks.
   - Do not add run tasks or main classes yet; section 13 owns them.
   - No native classifiers may leak into the library publication.
7. **Quality gates.** Copy `ci/check-docs.sh` (README at most 150 lines;
   internal links must resolve), `ci/check-jdk.sh` (major 25),
   `ci/install-gl-deps.sh`, and `ci/full-test.sh`.
8. **CI build script.** `ci/build.sh` runs, in order:
   - `bash ci/check-docs.sh`;
   - `./gradlew --no-daemon --stacktrace spotlessCheck test
     :examples:build dokkaGenerate publishToMavenLocal`.
   No benchmark task.
9. **CI integration script.** `ci/integration-test.sh` requires `xvfb-run`,
   exports `LIBGL_ALWAYS_SOFTWARE=1`, and runs
   `./gradlew --no-daemon --stacktrace integrationTest`.
10. **GitHub workflow.** Matrix `ubuntu-latest`, `windows-latest`,
    `macos-latest`; JDK 25 Temurin; Gradle setup; `bash ci/build.sh`; upload
    failure reports. Separate `gl` job on Ubuntu installs GL deps and runs
    `bash ci/integration-test.sh`. No publishing job.
11. **Forgejo workflow.** Same logic with the `jdk25` runner, Node 20 runtime
    bootstrap for checkout/upload actions, JDK check, GL deps, build,
    integration. Do not duplicate gates in YAML.
12. **Release scripts.** Copy `release-branch.sh`, `release-prepare.sh`,
    `release-tag.sh`, `release.sh`, and `verify-signatures.sh`. Change the tag
    message to `terminal-window-gui $version` and the default signature
    directory to `library/build/libs`. Section 15 owns the release docs and
    final signing configuration.
13. **Verify.** Run:
    ```sh
    ./gradlew -q projects
    ./gradlew -q javaToolchains
    ./gradlew build
    ./gradlew spotlessCheck
    ./gradlew publishToMavenLocal
    bash -n ci/*.sh scripts/*.sh
    ```
    `projects` must list `:`, `:library`, `:examples` only. `javaToolchains`
    must show JDK 25. `publishToMavenLocal` must produce a POM with no native
    classifiers.
14. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] `./gradlew -q projects` lists `:`, `:library`, `:examples`; no
      `:benchmarks`.
- [x] `./gradlew -q javaToolchains` shows JDK 25 in use.
- [x] `./gradlew build` passes with empty source sets.
- [x] `./gradlew spotlessCheck` passes.
- [x] `./gradlew publishToMavenLocal` succeeds and its POM has no LWJGL
      native classifier.
- [x] `ci/build.sh` runs the docs check, JDK check, Spotless, tests, examples
      build, Dokka, and `publishToMavenLocal`; no benchmark task.
- [x] `ci/integration-test.sh` runs under Xvfb with software GL.
- [x] GitHub and Forgejo workflows call the scripts and do not duplicate
      gates.
- [x] Release scripts pass `bash -n` and `--dry-run` where the version
      preconditions allow.
- [x] `gradlew.bat` is CRLF; wrapper checksum is pinned.
- [x] No widget implementation Kotlin files were created.

## Pitfalls

- Do not add `:benchmarks`, a performance table, or timing gates.
- Do not add LWJGL natives to the library publication; examples own their
  native configuration.
- Do not configure the library's `runtimeElements` to extend natives.
- Do not put Portal credentials in the repository or CI.
- Do not let CI publish; signing is local-only.
- Do not regenerate the wrapper or downgrade Gradle unless asked.
- Keep configuration-cache compatibility; avoid `Project` access at execution
  time.
- Keep the README under 150 lines or `ci/check-docs.sh` fails.

## Plan seed

> Plan section 02 only. Bootstrap a Kotlin 2.4.21 / JDK 25 / Gradle 9.9.1
> project for `terminal-window-gui`, modules `:library` and `:examples`, no
> benchmarks, with the `terminal-window` quality gates, shared `ci/*.sh`
> scripts, GitHub/Forgejo workflows, and release script scaffolding. Add a
> minimal publication for `publishToMavenLocal`; leave signing and Portal
> metadata to section 15. No widget code.
