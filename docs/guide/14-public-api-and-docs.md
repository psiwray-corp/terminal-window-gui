# 14 — Public API, documentation, compatibility

**Phase:** delivery · **Depends on:** 13 · **Blocks:** 15

## Goal

Freeze the public surface, prove it with an API audit, complete the docs set,
and enforce the widget conformance registry. No new widgets.

## Context budget

Read: `GUIDE.md`, this file, section 01's ADRs/compatibility policy, and
`terminal-window` `docs/guide/21-documentation.md`, `docs/public-api.md`,
`docs/compatibility.md`, `docs/architecture.md`, `README.md`,
`CONTRIBUTING.md`, and `CHANGELOG.md`. Do not read sibling sections. Expected
output: API audit, docs, README, changelog, conformance registry.

## Deliverables

- `docs/architecture.md` updated with the final package/object/thread/paint
  maps.
- `docs/public-api.md` with the thread-affinity table.
- `docs/compatibility.md` with the 1.0 policy and v1 gaps.
- `docs/layout.md`, `docs/input.md`, `docs/theming.md`, `docs/widgets.md`,
  `docs/testing.md`, `docs/shapes.md`, `docs/roadmap.md`.
- `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `AGENTS.md` updated.
- `library/src/test/kotlin/.../ApiSurfaceTest.kt`.
- `library/src/test/kotlin/.../WidgetConformanceTest.kt` and its registry.
- `library/src/test/kotlin/.../ModuleNameTest.kt` or a shell check in CI.
- Dokka output and the `-javadoc.jar` source set.

## Steps

1. **Public inventory.** Freeze the public types:
   - root: `GuiSurface`, `GuiInput`, `GridSize`, `GuiException`;
   - `.widget`: `Widget`, containers, all built-in widgets;
   - `.layout`: geometry, `Constraints`, `Length`, `Alignment`, `Positioned`,
     `Flexible`, `Expanded`;
   - `.paint`: `CellCanvas`, `PaintContext`, `TextMetrics`, `TextLayout`,
     `TextWrap`, `TextAlignment`;
   - `.shape`: `Shape`, `ShapeSpec`, `ShapeSet`, `ShapeSets`,
     `ResolvedShapeSet`, `MissingShapePolicy`, `MissingShapeException`,
     `GuiFonts`;
   - `.style`: `Theme`, `Style`, `WidgetState`, `Themes`;
   - `.input`: `GuiEvent` and subtypes, `FocusManager`, `FocusScope`,
     `FocusTraversal`, `OverlayHost`, `OverlayEntry`, `OverlayAnchor`,
     `OverlayPlacement`, `DismissPolicy`, `Scheduler`, `Clipboard`,
     `InMemoryClipboard`, `PointerCapture`, `HoverState`;
   - `.command`: `Command`, `KeyChord`, `Keymap`.
   `GuiSurface` exposes `size`, `needsLayout`, `needsPaint`, `pointerShape`,
   `focus`, `scheduler`, `clipboard`, `overlays`, and `hover`; no other
   runtime ownership.
2. **Visibility.** `explicitApi()` is enabled. No public declaration exposes
   `.internal` types, `org.lwjgl.*`, GLFW/OpenGL names, or
   `TerminalWindowService`/`TerminalWindow` as the owner. The allowed host
   boundary types are `BitmapFont`, `TerminalMatrix`, `Key`, `KeyAction`,
   `Modifiers`, `MouseButton`, `CursorShape`, and `terminal-window` input event
   types in `GuiInput`.
3. **API audit.** `ApiSurfaceTest` reflects the compiled test classpath:
   - exact public type inventory;
   - every public declaration has explicit visibility;
   - no internal type in a public signature;
   - no `org.lwjgl` type;
   - no public inline function;
   - sealed event hierarchy is closed;
   - no public mutable size fields (`preferredSize`, `minimumSize`,
     `maximumSize`).
   If reflection cannot see Kotlin metadata cleanly, pair it with a
   source/descriptor check; do not weaken the inventory.
4. **Module identity.** The JAR manifest carries
   `Automatic-Module-Name: com.psiwray.organization.terminal_window_gui`. No
   `module-info.class`. Verify with `jar --describe-module` in CI.
5. **Thread affinity.** Document: `GuiSurface`, widgets, controllers, themes,
   and shape resolution are single-thread-affine; `BitmapFont` and immutable
   value types are safe to share. The library has no synchronization, threads,
   coroutines, or timers. Clipboard implementations are caller-owned and must
   document their own thread rules.
6. **Compatibility.** SemVer 2.0.0:
   - public Kotlin declarations, JVM descriptors, module name, local-coordinate
     contract, sizing contract, shape resolution, event ordering, and focus
     behavior are covered;
   - rendered pixels, timing, exception text, font coverage, and platform
     terminal behavior are excluded;
   - data classes are frozen; adding a property is breaking;
   - default arguments are ABI;
   - nullability is ABI;
   - public sealed hierarchies are closed;
   - no public inline functions;
   - deprecation precedes removal, only in a major.
   v1 gaps: no attributes, wide chars, grapheme clusters, IME, images, OS
   clipboard, freeform drag/resize, animations, charts, or pickers.
7. **Docs.**
   - `README.md`: ≤150 lines; fresh-visitor install, requirements, quickstart
     with the manual surface loop, shape/font summary, examples, docs links.
   - `architecture.md`: packages, objects, thread map, paint cycle, local
     coordinates, sizing, shape resolution, ownership boundary.
   - `public-api.md`: inventory + thread-affinity table.
   - `compatibility.md`: policy and v1 gaps.
   - `layout.md`: constraints, flex, Stack/Positioned/Length, Grid/Wrap,
     clipping, rounding.
   - `input.md`: `GuiEvent` types, local coords, capture/bubble, focus,
     pointer capture, input translation, caller ownership.
   - `theming.md`: tokens, states, monochrome rules, built-in themes.
   - `widgets.md`: component catalog with states, keys, mouse, and minimums.
   - `testing.md`: harness, golden policy, conformance matrix, GL integration.
   - `shapes.md`: full code-point table, fallback rules, font attribution.
   - `roadmap.md`: v1.1 table and `terminal-window` gaps.
8. **Conformance registry.** `WidgetConformanceTest` contains one case per
   public widget. Each case renders and interacts with the widget through
   `GuiTestHarness` and covers the `GUIDE.md` matrix. A registry completeness
   check asserts every public widget type has a case. This is a guard; the
   cases themselves must catch behavior, not wiring.
9. **README quickstart.** Show the caller-owned loop:
   ```kotlin
   val surface = GuiSurface(root, Themes.dark(), ShapeSets.unicode().resolve(font))
   surface.resize(window.matrix.columns, window.matrix.rows)
   window.addInputListener { surface.dispatch(GuiInput.key(it)) } // typed listener
   while (!window.shouldClose) {
       window.frame()
       surface.resize(window.matrix.columns, window.matrix.rows)
       surface.tick(System.nanoTime())
       if (surface.needsPaint) surface.paint(CellCanvas(window.matrix))
       Thread.sleep(15)
   }
   ```
   Make clear that the library does not own the window or loop.
10. **Docs checks.** Every internal link resolves; no links to future files;
    README at most 150 lines; no benchmark claims; no window-ownership claims.
11. **CHANGELOG.** Add the `1.0.0` section with the feature inventory and the
    explicit limitations. Do not claim v1.1 items.
12. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] `ApiSurfaceTest` passes with the exact public inventory and no internal/
      LWJGL types in public signatures.
- [x] `jar --describe-module` shows
      `com.psiwray.organization.terminal_window_gui`.
- [x] Dokka builds HTML and the `-javadoc.jar` for every exported package.
- [x] `WidgetConformanceTest` has a case for every public widget and the
      registry completeness check passes.
- [x] Every doc link resolves and README is at most 150 lines.
- [x] `docs/public-api.md` has the thread-affinity table and ownership boundary.
- [x] `docs/compatibility.md` states the 1.0 policy and v1 gaps.
- [x] `docs/widgets.md` covers every v1 widget and its minimum size.
- [x] `docs/testing.md` matches the harness and matrix actually implemented.
- [x] README quickstart compiles against the published API and shows the caller
      loop.
- [x] CHANGELOG does not claim v1.1 work.
- [x] No widget behavior or build change is introduced in this section.

## Pitfalls

- Do not expose internal helper types just because they are convenient.
- Do not let the API audit pass by narrowing it; fix the signature.
- Do not claim `TerminalWindow` ownership anywhere in the docs.
- Do not document a `TerminalGui` loop or background repaint.
- Do not include benchmark/performance claims.
- Do not let a public data class gain a property without a major-version note.
- Do not publish Dokka warnings; `failOnWarning` and `reportUndocumented` stay
  on.
- Do not let the README become the full guide.

## Plan seed

> Plan section 14 only. Freeze and audit the public API, verify the automatic
> module name, complete the architecture/public-api/compatibility/layout/input/
> theming/widgets/testing/shapes/roadmap docs, write the README quickstart for
> the caller-owned loop, add the widget conformance registry, and update the
> changelog. No new widgets and no release signing.
