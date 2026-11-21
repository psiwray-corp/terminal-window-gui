# ADR-0003: JDK 25, Kotlin 2.4.21, and Gradle 9.9.1

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

The host `terminal-window` 2.0.0 targets JDK 25 and Kotlin 2.4.21. The GUI
library consumes host value types in public signatures, so its JVM baseline
cannot be lower than the host's. Gradle 9.9.1 is the host's pinned build tool
and supports JDK 25. The GUI repository should mirror that toolchain instead of
introducing a second Java/Kotlin baseline.

The library needs deterministic JUnit tests, Dokka output for the Central
Javadoc jar, ktlint/Spotless formatting, and nmcp for publication. It does not
need LWJGL, GLFW, JOML, or native libraries directly: the host owns the window
and renderer, and the GUI writes cells into a `TerminalMatrix`.

## Decision

We will use this toolchain:

| Item | Version / scope |
|---|---|
| JDK | 25, JVM target 25 |
| Kotlin | 2.4.21 |
| Gradle | 9.9.1, pinned wrapper |
| JUnit | 6.1.3 |
| Dokka | 2.3.0 |
| ktlint | 1.8.0 |
| Spotless | 8.11.0 |
| nmcp | 1.6.2 |
| Host library | `com.psiwray.organization:terminal-window:2.0.0` |

Rules:

1. `:library` compiles with `explicitApi()` and JVM target 25.
2. `terminal-window` is an `api` dependency; Kotlin stdlib is a runtime
   dependency.
3. The GUI POM does not depend on LWJGL, GLFW, JOML, platform native
   classifiers, or `net.java.dev.jna`.
4. `:examples` may use the host's LWJGL natives because it runs a window; those
   natives are not published with `:library`.
5. The Gradle wrapper checksum is pinned. Toolchain changes require a new ADR.
6. Java interop is best-effort; Kotlin metadata is authoritative.
7. No coroutines, no annotation processors, and no native build steps in
   `:library`.

The build profiles, wrapper checksum, CI scripts, and exact plugin IDs are
configured in section 02. This ADR fixes the versions and dependency boundary.

## Consequences

### Positive

- One JVM/Kotlin baseline across the host and GUI projects.
- JDK 25 gives the project a current LTS runtime and matches the host.
- The library POM stays free of native classifiers and GL dependencies.
- Tests and documentation use the same versions as the host, reducing drift.

### Negative

- Consumers must run JDK 25; JDK 21 and older are unsupported.
- Kotlin 2.4 metadata and JVM 25 descriptors limit the tooling that can
  consume the artifact.
- Dokka, ktlint, Spotless, and nmcp are additional build plugins to maintain.

### Neutral

- The host already brings LWJGL transitively to applications that open a
  window; the GUI library does not duplicate it.
- Platform launcher flags belong to the caller and examples, not to the
  library POM.
- Java callers can use the public API, but Kotlin nullability and default
  arguments remain Kotlin-first.

## Alternatives

| Alternative | Why rejected |
|---|---|
| JDK 21 | Lower baseline, but cannot consume the host's JDK 25 value types without a split. |
| Java source | The host and the rest of the project are Kotlin; Java-first is not a goal. |
| Gradle 8.x | Not the host baseline and would require a second wrapper/toolchain matrix. |
| Add LWJGL directly | The GUI library never opens a window or calls GL; it would enlarge the POM for no use. |
| Add JOML | No public GUI signature needs vector/matrix math. |
| Kotlin 2.3 | Host baseline is 2.4.21; a second Kotlin version breaks shared metadata expectations. |

## References

- `GUIDE.md` fixed facts and Kotlin conventions.
- `terminal-window` `build.gradle.kts` and `gradle.properties`.
- Kotlin 2.4.21 release notes: <https://kotlinlang.org/docs/releases.html>.
- Gradle 9.9.1 release notes: <https://gradle.org/releases/>.
- JUnit 6.1.3: <https://junit.org/>.
- Dokka 2.3.0: <https://kotlinlang.org/docs/dokka-introduction.html>.
