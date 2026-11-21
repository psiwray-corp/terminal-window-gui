# Input

Input is a caller-owned stream of immutable [`GuiEvent`](../library/src/main/kotlin/com/psiwray/organization/terminal_window_gui/input/GuiEvent.kt)
values. `GuiSurface.dispatch` makes layout current, then routes the event
through overlays and the root tree. Dispatch never paints and never changes
paint dirty flags.

## Event types

| Event | Fields | Typical source |
|---|---|---|
| `GuiKeyEvent` | `key`, `action`, `modifiers`, `repeat` | Host `KeyEvent` |
| `GuiCharEvent` | Unicode `codePoint` | Host `CharEvent` |
| `GuiPointerMoveEvent` | local `x`/`y`, screen `x`/`y`, modifiers | Host `MouseMoveEvent` |
| `GuiPointerButtonEvent` | button, press/release, modifiers, local/screen cells, `clickCount` | Host `MouseButtonEvent` |
| `GuiScrollEvent` | `dx`, `dy`, local `x`/`y` | Host `ScrollEvent` plus caller pointer cell |
| `GuiFocusEvent` | `focused` | Host `FocusEvent` |
| `GuiResizeEvent` | old and new `GridSize` | Caller resize handling |

`GuiEvent` is a closed sealed hierarchy. New event kinds require a major
release or a new non-sealed interface.

## Translation

`GuiInput` is pure translation and owns no listener, window, or device:

```kotlin
val guiEvent = GuiInput.key(hostKeyEvent)
surface.dispatch(guiEvent)
```

The host has no pointer position on scroll events. The caller supplies the last
pointer cell to `GuiInput.scroll(event, cellX, cellY)`. Host mouse coordinates
are cells; `GuiInput` copies them to both local and screen coordinates, and the
router recomputes local coordinates at each widget hop. Click counts are derived
by the surface from the press stream; the host value is always `1`.

## Routing

1. **Overlays first.** Entries are visited topmost to bottom. A modal entry
   consumes input below it; the root tree is not dispatched.
2. **Capture, target, bubble.** A pointer event captures from the root path to
   the hit widget, invokes the target, then bubbles to ancestors. Key events go
   to the focused widget and bubble from there. A handler returns `true` to
   consume the event; later handlers are skipped.
3. **Hit testing.** Screen cells are converted to local coordinates by
   subtracting accumulated origins and applying scroll offsets. Clips are
   honored. A captured pointer continues to receive moves outside the widget
   until release.
4. **Focus.** Tab and Shift+Tab use `FocusTraversal` over eligible, visible,
   enabled widgets whose `tabStop` is true. A focused widget sees key events
   first; a container can handle arrows, Enter, Escape, mnemonics, or omit
   handling so the parent gets the event. Structural `ScrollView`s, `MenuBar`,
   and passive banners keep `focusable = true` but default `tabStop = false`;
   `FocusManager.focus(widget)` and pointer focusing still reach them.
   Focusing a widget scrolls every ancestor `ScrollView` just enough to make
   its bounds visible. `FocusScope.TRAP` confines traversal inside a modal.
5. **Pointer capture.** `PointerCapture` keeps press/move/release ownership
   during a drag, including outside bounds. Release always ends capture.
6. **Hover.** `HoverState` records the current path; `GuiSurface.pointerShape`
   is advisory and the caller applies it to the host window.
7. **Commands.** After focused-widget handling, `GuiSurface.keymap` checks
   active screen scopes and global `Command` bindings. Commands never own a
   timer or thread.

Double/triple clicks are recognized from consecutive presses at the same cell
within the caller clock's threshold. Wheel events use the last pointer cell and
are consumed by the hit widget when it can scroll.

## Focus and overlays

`FocusManager` is single-thread-affine and lives on the surface. It exposes
`focused`, `focusPath`, `focus(widget)`, `clearFocus`, first/last/next/previous
traversal, traps, restore, and listener registration. Overlay entries add their
widget as an extra focus root; modal entries push a trap and restore the saved
focus when removed.

## Caller ownership

The caller installs the host `InputListener`, translates events on the host
owner thread, and calls `surface.dispatch` on the surface thread. The library
does not poll devices, start a listener thread, queue events, or call
`TerminalWindow`. A caller using a separate UI thread must marshal events; the
GUI has no queue and no synchronization.

## Related

[Architecture](architecture.md) · [Layout](layout.md) · [Testing](testing.md) ·
[Public API](public-api.md) · [ADR-0005](adr/0005-passive-gui-surface.md)
