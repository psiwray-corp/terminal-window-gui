# ADR-0005: Passive `GuiSurface`; the caller owns the window

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

`terminal-window` already owns the process-global window service, the GLFW
event pump, the window context, the renderer, and the frame cadence. A GUI
library layered on it must not create a second event loop, a second window
service, or a background repaint thread. Callers also need to run the GUI on
their own thread, with their own input translation and their own close policy.

The host's `TerminalWindowService` binds GLFW to the process main thread.
The host's `TerminalWindow` owns a context on one owner thread. The GUI must
not call either type; it receives cells and produces cells.

## Decision

We will make `GuiSurface` a passive, single-thread-affine cell-grid surface.

The caller owns:

- the `TerminalWindowService` and `TerminalWindow`;
- the owner thread and context transfer;
- event delivery and input translation;
- the frame loop and paint scheduling;
- timers and clock readings;
- close, resize, visibility, and cursor application;
- all GL and swap operations.

`GuiSurface` exposes only:

```kotlin
public fun resize(columns: Int, rows: Int)
public fun dispatch(event: GuiEvent): Boolean
public fun tick(nowNanos: Long): Boolean
public fun paint(canvas: CellCanvas)
public fun requestLayout()
public fun requestPaint()
public fun setTheme(theme: Theme)
```

It also exposes its size, dirty flags, pointer shape, focus manager, scheduler,
clipboard, overlay host, and hover state. It never exposes a
`TerminalWindow`, `TerminalWindowService`, window handle, GL object, thread,
future, or blocking wait.

Rules:

1. The library never opens, closes, resizes, focuses, or paints a native
   window.
2. The library never starts a thread, timer thread, coroutine, or background
   repaint. `tick(nowNanos)` runs only timers that are already due.
3. The library never calls `TerminalWindow`, `TerminalWindowService`, GLFW, or
   OpenGL.
4. The caller translates host input into `GuiEvent` values with `GuiInput` and
   calls `dispatch` on the surface thread.
5. The caller compares `TerminalMatrix` dimensions with `surface.size` and
   calls `resize`; the surface never observes the window.
6. The caller paints only when `needsPaint` is true, into a `CellCanvas` over
   the host matrix, then asks the host for a repaint.
7. `paint` writes cells and returns. It does not upload, draw, swap, or sleep.
8. `GuiSurface` and all attached widgets are confined to one caller thread.
   There is no internal synchronization.
9. Host value types may cross the API boundary (`TerminalMatrix`,
   `BitmapFont`, input events, `CursorShape`); host window/service types may
   not.
10. The library owns no close request. `Esc` in an overlay dismisses that
    overlay; it does not close the window.

The architecture document contains the exact caller integration sequence.

## Consequences

### Positive

- The GUI composes with any host loop, including a single-threaded example or
  a multi-threaded application.
- Golden tests call `resize`, `dispatch`, `tick`, and `paint` with no window or
  display.
- The caller controls repaint cadence, close veto, and timer frequency.
- The library cannot deadlock the host's main thread or steal GLFW callbacks.

### Negative

- There is no convenience application runner; a first integration requires
  explicit event translation and frame-loop code.
- Callers can forget to paint or request a repaint; the library cannot fix
  that.
- The library cannot animate itself; animations are a v1.1 caller feature.
- All GUI state is thread-affine, so callers must not dispatch from arbitrary
  threads.

### Neutral

- Section 13 provides `:examples` helpers that own the loop; they are not part
  of the published library.
- The host may deliver several events between paints; the surface coalesces
  state changes.
- Cursor application is advisory: the caller decides whether to call
  `TerminalWindow.setCursor`.

## Alternatives

| Alternative | Why rejected |
|---|---|
| GUI owns a `TerminalWindow` | Duplicates host lifecycle and breaks embedding in existing applications. |
| Background repaint thread | Adds synchronization, races, and a second cadence the host does not need. |
| Own event loop | Fights `TerminalWindowService`'s main-thread GLFW contract. |
| Swing/AWT integration | Not the target; the GUI is a cell-grid toolkit. |
| Static render-only API | No focus, editing, timers, or hover; cannot implement the component catalog. |

## References

- `terminal-window` `docs/public-api.md` thread-affinity table.
- `terminal-window` `docs/frame.md` frame contract.
- `terminal-window` `docs/architecture.md` service/owner thread map.
- `docs/guide/06-gui-surface-and-test-harness.md` surface API.
