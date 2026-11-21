# 06 — GuiSurface, input translation, test harness

**Phase:** runtime · **Depends on:** 05 · **Blocks:** 07

## Goal

The passive surface API the caller drives: resize, dispatch, tick, and paint.
Pure terminal-window input translation. A headless test harness and one real
GL integration test. The library never owns a window, service, loop, thread, or
timer.

## Context budget

Read: `GUIDE.md`, this file, sections 04–05 public types, and
`terminal-window` `docs/frame.md`, `docs/input.md`, `docs/public-api.md`, plus
its `ReadmeQuickstart` and `terminal-window-cmdline` `AppLoop` for integration
shape (not to copy ownership). Do not read sibling sections. Expected output:
`GuiSurface`, `GuiInput`, test kit, integration test.

## Deliverables

- `.../GuiSurface.kt`, `.../GridSize.kt`, `.../GuiInput.kt`.
- `.../GuiException.kt` for invalid surface/canvas state.
- `library/src/test/kotlin/.../testkit/GuiTestHarness.kt`,
  `GoldenAssert.kt`, `MatrixDump.kt`.
- `library/src/test/resources/golden/` initial fixtures.
- `library/src/integrationTest/kotlin/.../GuiSurfaceGlTest.kt`.
- Tests: `GuiSurfaceTest`, `SurfaceFlagsTest`, `InputTranslationTest`,
  `ResizeTest`, `PaintFailureTest`.

## GuiSurface API

```kotlin
public class GuiSurface(
    root: Widget,
    theme: Theme,
    shapes: ResolvedShapeSet,
    clipboard: Clipboard = InMemoryClipboard(),
) {
    public val size: GridSize                 // columns, rows
    public val needsLayout: Boolean
    public val needsPaint: Boolean
    public val pointerShape: CursorShape?     // advisory only
    public val focus: FocusManager
    public val scheduler: Scheduler
    public val clipboard: Clipboard
    public val overlays: OverlayHost          // painted after the root
    public val hover: HoverState

    public fun resize(columns: Int, rows: Int)
    public fun dispatch(event: GuiEvent): Boolean
    public fun tick(nowNanos: Long): Boolean  // due timers; true if state changed
    public fun paint(canvas: CellCanvas)
    public fun requestLayout()
    public fun requestPaint()
    public fun setTheme(theme: Theme)         // invalidates paint
}
```

`GuiSurface` is single-thread-affine. It contains no synchronization and no
background work. The caller constructs it, resizes it to the matrix, feeds
events, and paints when convenient.

## Steps

1. **Construction.** Store the root, theme, resolved shapes, and clipboard.
   Create the `FocusManager`, `Scheduler`, `OverlayHost` (sharing the focus
   manager), and `HoverState`. Set the root as the surface root (no parent).
   Initial size is `0×0`; the first `resize(columns, rows)` is required before
   `dispatch` or `paint`. Widgets receive these services through their attach
   context; they never construct their own.
2. **Resize.** Validate positive dimensions. Update `size`, set the root
   layout bounds to `(0, 0, columns, rows)` in the local root space, invalidate
   layout, clamp scroll models, ensure the focused widget and caret remain
   visible after the next layout, and mark paint. Do not paint from `resize`.
3. **Lazy layout.** `dispatch` and `paint` ensure layout is current before
   using bounds. Layout is performed with tight root constraints
   `(0..columns, 0..rows)`. `dispatch` never paints; `paint` never dispatches.
4. **Dispatch.** Route overlays first (topmost to bottom), then the root
   through `EventRouter` (section 05). A modal overlay consumes everything
   below it. Update `needsLayout`/`needsPaint` from widget invalidation. Return
   whether the event was consumed. Do not clear flags here.
5. **Tick.** Run `Scheduler.runDue(nowNanos)`; return whether a timer changed
   state and marked paint/layout. No sleeping, no threads, no automatic
   scheduling. The caller decides the cadence.
6. **Paint.** Validate `canvas` dimensions equal `size`. If layout is dirty,
   lay out first. Fill the whole canvas with space/`theme.background`; paint the
   root through a `PaintContext(canvas, shapes)`, then paint `overlays` on top.
   Clear `needsLayout` only after successful layout and `needsPaint` only after
   successful paint. If a widget throws, preserve both flags and rethrow. No
   GL, upload, swap, or `TerminalWindow` calls.
7. **Theme.** `setTheme` replaces the immutable theme and invalidates paint;
   it does not change layout.
8. **Pointer shape.** The router publishes the hovered widget's requested
   `CursorShape?`; `GuiSurface` exposes it. The caller may apply it with
   `TerminalWindow.setCursor`. The library never calls the window.
9. **Input translation.** `GuiInput` is a pure object:
   - `key(KeyEvent) -> GuiKeyEvent`;
   - `char(CharEvent) -> GuiCharEvent`;
   - `move(MouseMoveEvent) -> GuiPointerMoveEvent` using cell coordinates;
   - `button(MouseButtonEvent) -> GuiPointerButtonEvent` using cell coordinates;
   - `scroll(ScrollEvent, cellX, cellY) -> GuiScrollEvent` because the host
     scroll event has no position; the caller or surface supplies the last
     pointer cell;
   - `focus(FocusEvent) -> GuiFocusEvent`.
   Do not translate `ResizeEvent` into layout here; the caller compares matrix
   dimensions and calls `resize`. Do not translate close requests; the caller
   owns close/veto.
10. **Test harness.** In test source, not the published artifact:
    - `GuiTestHarness(root, theme, shapes, columns = 80, rows = 24)`.
    - `resize`, `paint`, `rows()`, `styles()`, `key`, `char`, `click`, `drag`,
      `scroll`, `tick`, `advance(millis)`, `focusPath`, `paintCount`,
      `layoutCount`.
    - `rows()` returns the cell grid as strings; `styles()` returns theme token
      names for known colors and `#AARRGGBB` otherwise.
    - Golden files under `src/test/resources/golden`; `-PupdateGoldens`
      rewrites them, CI never does.
    - Fake clock starts at 0; `advance` moves it and calls `tick`.
11. **Integration test.** Tag `gl`. Create `TerminalWindowService`, open a
    window with `GuiFonts.bundled()`, build a small `GuiSurface` tree, attach a
    caller-owned `InputListener` that calls `GuiInput`/`dispatch`, paint the
    matrix, call `window.frame()`, assert `renderStats().drawCalls > 0`, then
    close. The test owns the service/window/loop; the library does not.
12. **Tests.** Flags after dispatch/paint; dispatch does not paint; paint does
    not dispatch; layout before dispatch; resize 1×1, 80×24, 240×67; theme
    change; paint failure preserves flags; canvas size mismatch throws;
    translation of every host event; harness goldens and fake clock; one real
    GL frame under Xvfb.
13. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] `GuiSurface` source contains no `TerminalWindow`, `TerminalWindowService`,
      `frame`, `requestRepaint`, GLFW, or GL calls.
- [x] The caller can delay, skip, or repeat `paint` without the library
      scheduling anything.
- [x] `dispatch` performs lazy layout but never paints; `paint` never
      dispatches.
- [x] `resize` updates the root bounds, invalidates layout, and does not paint.
- [x] `paint` fills the full canvas and produces deterministic cells for a
      fixed tree; a failed paint preserves both dirty flags.
- [x] Canvas/surface size mismatch throws `IllegalArgumentException`.
- [x] `GuiInput` maps key/char/move/button/scroll/focus without owning the
      input listener.
- [x] `pointerShape` is advisory; the library never calls `setCursor`.
- [x] `GuiSurface` exposes `focus`, `scheduler`, `clipboard`, `overlays`, and
      `hover`; overlays dispatch before the root and paint after it.
- [x] `GuiTestHarness` renders, sends synthetic events, advances a fake clock,
      and compares golden rows.
- [x] The GL integration test runs under Xvfb, paints one frame, observes a
      draw call, and closes.
- [x] No application loop, worker thread, timer thread, or service ownership
      exists in `:library`.

## Pitfalls

- Do not add a `TerminalGui` convenience runner that owns the window. Examples
  show the manual loop; the library does not.
- Do not clear dirty flags before a successful paint; retry semantics matter.
- Do not paint before layout; bounds are stale.
- Do not hold a `CellCanvas` across resizes; create it per paint after the
  matrix size is known.
- Do not translate `ResizeEvent` into a layout trigger; the matrix resize
  happens inside `frame()` after input drain.
- Scroll events have no position in the host; use the last pointer cell and
  document it.
- Do not call `requestRepaint` from the library; painting changes cells and the
  matrix dirty bit is the caller's signal.
- Do not use wall-clock time in the harness; use the fake clock.
- Keep the test harness out of the published artifact.

## Plan seed

> Plan section 06 only. Implement the passive `GuiSurface` (resize, dispatch,
> tick, paint, flags, advisory pointer shape), pure `GuiInput` translation from
> terminal-window events, and the headless `GuiTestHarness` with golden rows,
> synthetic events, and a fake clock. Add one Xvfb GL integration test that
> owns its own window. No library-owned window, loop, thread, or timer.
