# Compatibility

This document is the consumer-facing compatibility policy for
`com.psiwray.organization:terminal-window-gui:1.0.0`. It covers the public API,
JVM shape, module identity, coordinate and sizing contracts, event ordering,
and shape resolution. The exact public inventory is listed in
[`public-api.md`](public-api.md) and checked by `ApiSurfaceTest`; the policy in
this file does not promise rendered pixels or timing.

## Version 1.0 policy

The artifact follows Semantic Versioning 2.0.0:

- A breaking public API or contract change requires a major version.
- Additions are minor versions.
- Documentation, performance, and internal changes are patch versions.

`1.0.0` is the first release. There is no earlier GUI version, no migration
from a GUI `0.x`, and no compatibility shim for a different package root.

The following are covered:

| Contract | Covered behavior |
|---|---|
| Public Kotlin declarations | Exact type inventory, names, visibility, types, nullability, default arguments, sealed/data shapes, KDoc-visible semantics |
| Kotlin metadata | Public metadata and suspend/free function shape (there are no suspend APIs) |
| JVM descriptors | Public methods, fields, constructors, `copy`, `componentN`, and `$default` methods |
| Automatic module name | `com.psiwray.organization.terminal_window_gui` |
| Coordinate system | Root and child bounds are local; `PaintContext` translates to screen cells; clips are half-open |
| Sizing contract | One `measure(Constraints)` path; no mutable preferred/min/max size fields |
| Event ordering | Overlays first, root second; focus and dispatch order; layout before dispatch/paint |
| Shape resolution | Primary, per-shape fallback, global last resort, strict failure |
| Thread affinity | Surface and widgets are single-thread-affine; no internal synchronization |
| Required host | `terminal-window:2.0.0` or a compatible 2.x version |

The following are **excluded** from the compatibility promise:

- rendered pixels, antialiasing, glyph rasterization, and platform font
  coverage;
- timing, frame cadence, cursor blink rate, animation duration, and scheduling
  latency;
- exception message text (the exception type and category are covered);
- default theme color values and visual contrast;
- exact cell styles when a theme token is remapped by the caller;
- internal types, internal names, and private implementation details;
- examples, screenshots, and `:examples` APIs;
- build profiles, launcher flags, Gradle plugin versions, and CI runner
  versions;
- behavior of a `terminal-window` version newer than 2.0.0 that changes host
  semantics; the caller must verify that upgrade.

## API rules

1. Public data classes are frozen: adding a property changes the constructor,
   `copy`, and `componentN` descriptors. Add a new type or wait for a major.
2. Default arguments are ABI. Adding a parameter changes the synthetic
   `$default` method.
3. Nullability is ABI. Do not change `T` to `T?` or back in a minor release.
4. Public sealed hierarchies are closed shapes. New event kinds require a new
   non-sealed interface or a major release.
5. There are no public inline functions. An inline body is copied into
   consumer bytecode and cannot be fixed in a patch.
6. Deprecation precedes removal; removal happens only in a major release.
7. `internal` is a Kotlin source boundary, not a security boundary. Internal
   declarations are public bytecode and are not compatibility promises.
8. Public signatures never expose `org.lwjgl.*`, GLFW handles, GL names, or
   `TerminalWindow`/`TerminalWindowService`.
9. `terminal-window` value types are allowed only at the caller boundary:
   `TerminalMatrix`, `BitmapFont`, input events, `Key`, `KeyAction`,
   `Modifiers`, `MouseButton`, and `CursorShape`. No `org.lwjgl.*`,
   `TerminalWindow`, or `TerminalWindowService` type may appear in a public
   signature.
10. The exact public type set, including nested public classes and aliases,
    is frozen by `ApiSurfaceTest` and `library/src/test/resources/public-api.txt`.
    A deliberate addition is still a source-compatible minor change, but it
    must update the inventory in the same review.
10. The library has no public mutable size fields and no public setter that
    changes a widget's preferred size.

## Threading and lifecycle

`GuiSurface` and every attached widget are confined to one caller thread.
There is no internal locking and no background work. Calling `resize`,
`dispatch`, `tick`, or `paint` from different threads is a caller bug; the
library does not attempt to detect every race.

The caller owns the `TerminalWindow`, its owner thread, input delivery,
repaint, timers, close, and GL. The library never calls the host window or
service. A `GuiSurface` is not `Closeable`; it owns no native resource and can
be discarded after the caller stops using it.

A surface must be resized before dispatch or paint. The caller must not paint
with a `CellCanvas` whose dimensions differ from `surface.size`.

## Module and classpath

The JAR manifest carries:

```text
Automatic-Module-Name: com.psiwray.organization.terminal_window_gui
```

No `module-info.class` is shipped in 1.0. On the module path this is an
automatic module: it exports every package, including `.internal`, and reads
every module. Consumers must declare `terminal-window`, LWJGL, and JOML modules
or use the classpath.

Do not mix multiple GUI major versions, or GUI versions with incompatible
`terminal-window` versions, on one classpath or module path. The JVM may load
classes from whichever artifact appears first, with no error.

## Host dependency

The GUI depends on `terminal-window:2.0.0` as an `api` dependency. The GUI does
not add LWJGL, GLFW, JOML, or native classifiers to the consumer POM.
Applications that open a window add host natives as they already do.

`terminal-window` 2.0.0 is not modified in GUI 1.x. A host 2.x upgrade is a
consumer decision; the GUI covers the host types it names, not every host
behavior.

## v1 gaps

These are deliberate v1 limits, not silent failures:

- no bold, italic, underline, strikethrough, or dim;
- no CJK double-width cells, combining marks, or grapheme clusters;
- no IME preedit/composition;
- no OS clipboard (only the `Clipboard` interface with an in-memory default);
- no runtime font switching or fallback chain beyond the resolved shape set;
- no images, sixel, or kitty graphics;
- no RTL/bidi or accessibility bridge;
- no sub-cell click precision;
- no Markdown/ANSI rich text or syntax highlighting;
- no scrollback beyond a caller-supplied `LogView` model;
- no `DesktopWindow`, slider, pickers, charts, animations, drag-and-drop
  reorder, variable-height rows, lazy tree children, content-width table
  columns, in-cell table editing, command palette, or form validation engine.

The full candidate list with the exact `terminal-window` prerequisite is in
[`docs/roadmap.md`](roadmap.md). Nothing in the v1 gaps is a hidden stub.

## Related

[Architecture](architecture.md) · [Roadmap](roadmap.md) ·
[Testing](testing.md) · [ADR-0001](adr/0001-artifact-identity.md) ·
[ADR-0009](adr/0009-v1-scope.md)
