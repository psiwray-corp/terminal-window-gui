# 05 — Interaction runtime

**Phase:** core · **Depends on:** 04 · **Blocks:** 06

## Goal

Focus traversal, local-coordinate event routing, pointer capture, overlays,
themes, commands/keymaps, clipboard, and caller-driven timers. No
`TerminalWindow` integration yet.

## Context budget

Read: `GUIDE.md`, this file, section 04's `Widget`/container tree, and
`terminal-window` `docs/input.md`, `docs/public-api.md` (input and cursor
types). Do not read sibling sections. Expected output: event/focus/overlay/
theme/command/clipboard/scheduler code and tests. Split seam if needed: 05a
focus/events/overlays, 05b theme/commands/clipboard/scheduler.

## Deliverables

- Extend `.../widget/Widget.kt` with focus, visibility, enabled, event, hover,
  and pointer-capture hooks.
- `.../input/GuiEvent.kt` and subtypes, `EventRouter.kt`, `PointerCapture.kt`,
  `HoverState.kt`, `FocusManager.kt`, `FocusScope.kt`, `FocusTraversal.kt`.
- `.../input/OverlayHost.kt`, `OverlayEntry.kt`, `OverlayAnchor.kt`,
  `OverlayPlacement.kt`, `DismissPolicy.kt`.
- `.../style/Theme.kt`, `Style.kt`, `WidgetState.kt`, `Themes.kt`.
- `.../command/Command.kt`, `KeyChord.kt`, `Keymap.kt`.
- `.../input/Clipboard.kt`, `InMemoryClipboard.kt`.
- `.../input/Scheduler.kt`, `TimerHandle.kt`.
- Tests: `FocusTraversalTest`, `EventRoutingTest`, `PointerCaptureTest`,
  `DoubleClickTest`, `OverlayTest`, `ThemeStateTest`, `CommandKeymapTest`,
  `ClipboardTest`, `SchedulerTest`.

## Event model

```kotlin
public sealed interface GuiEvent

public data class GuiKeyEvent(
    public val key: Key,
    public val action: KeyAction,
    public val modifiers: Modifiers,
    public val repeat: Boolean,
) : GuiEvent

public data class GuiCharEvent(public val codePoint: Int) : GuiEvent

public data class GuiPointerMoveEvent(
    public val localX: Int, public val localY: Int,
    public val screenX: Int, public val screenY: Int,
    public val modifiers: Modifiers,
) : GuiEvent

public data class GuiPointerButtonEvent(
    public val button: MouseButton,
    public val action: KeyAction,
    public val modifiers: Modifiers,
    public val localX: Int, public val localY: Int,
    public val screenX: Int, public val screenY: Int,
    public val clickCount: Int,
) : GuiEvent

public data class GuiScrollEvent(
    public val dx: Double, public val dy: Double,
    public val localX: Int, public val localY: Int,
) : GuiEvent

public data class GuiFocusEvent(public val focused: Boolean) : GuiEvent
public data class GuiResizeEvent(public val old: GridSize, public val new: GridSize) : GuiEvent
```

Pointer events are delivered with coordinates local to the receiving widget;
`screenX/screenY` are retained for overlays, tooltips, and drag ghosts.
Containers convert by subtracting the child origin (and scroll offset).

## Steps

1. **Widget hooks.** Add `enabled`, `visible`, `focusable`, `isFocused`,
   `onEvent(event): Boolean`, `onFocusChanged(focused)`,
   `onHoverChanged(hovered)`, and internal `wantsPointerCapture` to `Widget`.
   Default `onEvent` returns `false`. Disabled widgets do not receive keyboard
   or pointer events. Invisible or zero-size widgets are skipped by focus and
   hit testing.
2. **Event router.** Dispatch capture root→target, target, bubble
   target→root. A `true` return consumes and stops. Hit testing walks children
   in reverse paint order using local coordinates. Key events target the
   focused widget; scroll targets the widget under the pointer, else the
   focused widget, else the root. A click focuses the nearest focusable
   ancestor/descendant.
3. **Pointer capture.** On button press inside a widget, capture it. Moves and
   releases go to the capture even outside its bounds, with local coordinates
   that may be negative. Release clears capture. Window focus loss clears
   capture, pressed state, and hover, but keeps keyboard focus.
4. **Hover.** Track the hover path; send enter/leave via `onHoverChanged`.
   Moving over an overlay removes hover from the layer below. Tooltips and
   cursor-shape requests read the hovered widget.
5. **Click counts.** Detect double-click and triple-click using a 400 ms
   threshold, the same button, and the same cell (or an adjacent cell for
   double-click tolerance). `clickCount` is 1, 2, or 3. Reset on move beyond
   the tolerance, different button, or timeout.
6. **Focus traversal.** `FocusManager` builds a depth-first order of
   focusable, enabled, visible, non-zero-size widgets. `FocusScope` controls
   traversal: `ENTER` (default), `SKIP`, `TRAP`. Tab/Shift-Tab focus next/
   previous; arrows are widget-specific. `focus(widget)`, `focusFirst`,
   `focusLast`, `focusNext`, `focusPrevious`, `restore(previous)`. A modal
   overlay traps focus and restores it on close.
7. **Overlays.** `OverlayHost` is painted after the root and receives pointer
   events first. `OverlayEntry` has a widget, anchor (`Screen`, `Widget` with
   alignment, or `Rect`), placement (`ABOVE`, `BELOW`, `LEFT`, `RIGHT`,
   `AUTO`), dismiss policy (`CLICK_OUTSIDE`, `ESCAPE`, `BOTH`, `NONE`),
   `modal`, and `shadow`. Placement flips when it does not fit and clamps to
   the screen. Z-order is entry order; topmost wins hit testing. Clicking
   outside dismisses non-modal entries; Esc dismisses. Modal entries trap
   focus and block input to the layer below.
8. **Theme.** `Theme` is an immutable token set:
   `background`, `surface`, `surfaceAlt`, `text`, `textMuted`, `textDisabled`,
   `accent`, `success`, `warning`, `error`, `border`, `borderFocused`,
   `selectionBackground`, `selectionForeground`, `caret`, `shadow`.
   `WidgetState` covers normal/focused/hovered/pressed/disabled/selected/
   error. `Style(fg, bg)` resolves through the theme. No text attributes.
   Focus and selection must remain distinguishable without color: use border
   markers, `>` cursors, or glyph changes. Provide `Themes.dark()`,
   `Themes.light()`, `Themes.highContrast()`.
9. **Commands and keymaps.** `Command(id, label, description, shortcut,
   enabled, checked, action)`; `enabled`/`checked` are evaluated on demand.
   `KeyChord(key, modifiers)`. `Keymap` supports global and screen-scoped
   bindings, conflict detection, and a platform-aware label
   (`Ctrl`/`Alt`/`Super`). Order: modal overlay keys, focused widget, bubble,
   screen keymap, global commands, mnemonics. Do not execute a command and a
   focused-widget action for the same event.
10. **Clipboard.** `Clipboard { getText(): String?; setText(text) }` plus an
    in-memory default. The library never touches the OS clipboard. Text
    widgets use this interface.
11. **Scheduler.** `postDelayed(delayMillis, action): TimerHandle`,
    `cancel(handle)`, `runDue(nowNanos): Boolean`, `nextDeadlineNanos(): Long?`.
    Callbacks run on the caller thread inside `runDue`; no threads. Used by
    tooltips, toasts, spinners, and optional caret blink.
12. **Tests.** Tab order with disabled/invisible/zero-size widgets; focus
    scopes and trap/restore; capture/target/bubble order and consumption;
    mouse capture outside bounds; double/triple click; wheel targeting; key
    repeat; overlay flip/clamp/dismiss/z-order/focus trap; theme state
    resolution and monochrome focus; command enable/check/conflict; clipboard;
    deterministic scheduler with a fake clock.
13. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Pointer events are local to the receiving widget; screen coordinates are
      retained for overlays.
- [x] Capture runs root→target, target, then bubble; consumption stops the
      chain; disabled widgets are skipped.
- [x] Press captures the pointer; move/up outside bounds still arrive; release
      clears capture.
- [x] Double/triple click counts are deterministic and reset correctly.
- [x] Tab order skips disabled, invisible, and zero-size widgets; focus scopes
      enter/skip/trap correctly; modal restore works.
- [x] Overlays flip/clamp, dismiss by click-outside/Esc, stack by z-order, and
      trap focus when modal.
- [x] Theme states resolve and focus remains visible without color.
- [x] Commands run once per event; disabled commands do not run; keymap
      conflicts are rejected.
- [x] Clipboard is an interface; the in-memory default works.
- [x] Scheduler is deterministic with a fake clock and creates no threads.
- [x] No `TerminalWindow`/GL calls are present.

## Pitfalls

- Do not use screen coordinates inside widget handlers; convert at the
  container boundary.
- Do not focus invisible/zero-size widgets; the focus cycle must not trap.
- A modal overlay must not leak keys to the layer below.
- Click-outside dismissal must not fire for clicks inside a child popup.
- Hover must be cleared when an overlay covers the widget.
- Double-click timing must use a monotonic clock, not wall time.
- `enabled`/`checked` lambdas must not create per-paint allocations; evaluate
  only on command events/menu open.
- Do not execute a global shortcut that a text field consumed (`Ctrl+C`).
- Do not create background timers; `runDue` is caller-driven.
- Do not add text attributes; the host has only fg/bg.

## Plan seed

> Plan section 05 only. Add focus/visibility/event hooks to `Widget`, implement
> local-coordinate capture/target/bubble routing, pointer capture, hover,
> double/triple click, focus scopes/traversal/restore, overlay host with
> flip/clamp/dismiss/focus trap, theme tokens and state styles, commands and
> keymaps, clipboard interface, and a caller-driven scheduler. Add the listed
> tests. No `TerminalWindow` integration.
