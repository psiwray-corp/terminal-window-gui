# ADR-0001: Artifact identity and coordinates

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

`terminal-window-gui` is a new library built on
`com.psiwray.organization:terminal-window:2.0.0`. It has no earlier release,
so its first published version is `1.0.0`. Maven Central versions are
immutable: once `1.0.0` is published, it cannot be edited or redirected, and a
broken release must be followed by `1.0.1` or a later version.

The library is a GUI toolkit, not a terminal emulator or application runtime.
It needs a stable coordinate, package root, automatic module name, and source
repository that are distinct from the host renderer. The host keeps its own
coordinates and release history; this library never changes them.

## Decision

We will publish the library under these coordinates:

| Item | Value |
|---|---|
| Group | `com.psiwray.organization` |
| Artifact | `terminal-window-gui` |
| Release version | `1.0.0` |
| Development version | `1.0.0-SNAPSHOT` |
| Repository | `psiwray-corp/terminal-window-gui` |
| Package root | `com.psiwray.organization.terminal_window_gui` |
| Automatic module name | `com.psiwray.organization.terminal_window_gui` |
| Published module | `:library` only |
| Proof module | `:examples`; not published |
| Host dependency | `com.psiwray.organization:terminal-window:2.0.0` |

The package root is unique to this artifact. Consumers must not mix multiple
GUI major versions on one classpath or module path. The artifact is new, so
there is no migration from a previous GUI coordinate.

`terminal-window` is an `api` dependency because public signatures use its
value types at the caller boundary: `TerminalMatrix`, `BitmapFont`, input
events, `Key`, `KeyAction`, `Modifiers`, `MouseButton`, and `CursorShape`.
No window, service, GLFW, OpenGL, or LWJGL type appears in a public GUI
signature.

The GitHub repository is the source of truth for the build, CI, and release
workflow. Maven Central is the consumer distribution channel.

## Consequences

### Positive

- A dedicated coordinate makes the GUI library independently versioned from
  the terminal renderer.
- Consumers can depend on `terminal-window-gui` without learning a second
  package root or a moved artifact.
- `:examples` can evolve without becoming a published artifact.
- The automatic module name matches the package root and can be audited.

### Negative

- The new coordinate starts at `1.0.0`; there is no earlier GUI version to
  upgrade from and no compatibility shim.
- Consumers must add both `terminal-window-gui` and the host
  `terminal-window:2.0.0` (transitively supplied by the `api` dependency).
- The library cannot publish a fix by rewriting `1.0.0` after release.

### Neutral

- Maven Central publication is configured in section 15; this record fixes only
  identity and dependency scope.
- The `:examples` module may use host natives, but the published POM must not
  contain platform-specific native classifiers.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Publish as part of `terminal-window` | Couples GUI release cadence to the renderer and makes the host artifact own a much larger API. |
| Use a different group | Splits ownership and signing for no consumer benefit. |
| Start at `0.1.0` | This is a first stable library release; `1.0.0` matches the published contract and avoids a second compatibility line. |
| Publish `:examples` | Examples are proof and documentation, not a supported dependency. |
| Use the host package root | Invites silent classpath conflicts and violates the separate-artifact boundary. |

## References

- `GUIDE.md` package map and section table.
- `docs/guide/01-charter-scope-and-roadmap.md`, steps 1 and 4.
- Maven Central: `com.psiwray.organization:terminal-window`.
- Semantic Versioning 2.0.0: <https://semver.org/spec/v2.0.0.html>.
