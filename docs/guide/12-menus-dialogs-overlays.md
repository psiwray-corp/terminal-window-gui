# 12 — Menus, dialogs, and overlays

**Phase:** widgets · **Depends on:** 11 · **Blocks:** 13

## Goal

MenuBar/Menu/ContextMenu, Dialog/ConfirmDialog/PromptDialog, Popover, Tooltip,
and Toast. Keyboard-first overlays with flip/clamp, dismissal, focus traps,
z-order, and caller-driven timers.

## Context budget

Read: `GUIDE.md`, this file, sections 05–11 public types, and the conformance
matrix. Do not read sibling sections. Expected output: menu/dialog/overlay
widgets plus tests. Split seam if needed: 12a menus, 12b dialogs/overlays.

## Deliverables

- `.../widget/Menu.kt`, `MenuBar.kt`, `MenuItem.kt`, `MenuPopup.kt`,
  `ContextMenu.kt`.
- `.../widget/Dialog.kt`, `DialogAction.kt`, `DialogResult.kt`,
  `ConfirmDialog.kt`, `PromptDialog.kt`.
- `.../widget/Popover.kt`, `Tooltip.kt`, `TooltipController.kt`,
  `Toast.kt`, `ToastManager.kt`.
- Tests: `MenuBarTest`, `MenuPopupTest`, `ContextMenuTest`, `DialogTest`,
  `ConfirmPromptTest`, `PopoverTest`, `TooltipTest`, `ToastTest`, plus golden
  fixtures.

`GuiSurface` exposes `overlays: OverlayHost`, `focus`, `scheduler`, `clipboard`,
and `hover` (section 06). Menus/dialogs/toasts are added to that host; the
library never creates a second overlay layer.

## Steps

### Menus

1. **Menu model.** `Menu(title, mnemonic: Char?, items)`.
   `MenuItem` is sealed:
   - `MenuItem.CommandItem(command: Command)`;
   - `MenuItem.Separator`;
   - `MenuItem.Submenu(title, items)`.
   Commands supply label, shortcut, enabled, checked, and action. Menu item
   height is 1.
2. **MenuBar.** One row in `AppShell`. Inactive state shows titles; active
   state marks the open menu with a non-color marker. Keyboard: `F10` toggles;
   `Alt+<mnemonic>` opens; Left/Right switch menus; Down opens and focuses the
   first item; Up opens and focuses the last; Esc closes and returns focus.
   Mouse: click opens; hovering another title while open switches; click
   outside closes. Disabled items are skipped by keyboard but visible.
3. **MenuPopup.** Anchored below the menu title, flipping above when needed and
   clamping to the screen. Width is max(label + shortcut + markers) plus
   padding. If the list is taller than the screen, scroll with visible
   `SCROLL_UP`/`SCROLL_DOWN` indicators; the focused item is always visible.
   Up/Down move, Home/End jump, Enter activates, Right opens a submenu, Left
   closes it, Esc closes the whole menu. Checked commands show `CHECK`; radio
   commands show `DOT`; submenus show `TREE_COLLAPSED`. Shortcuts are
   right-aligned. Activation closes the menu and restores focus before running
   the command.
4. **ContextMenu.** `ContextMenu(items)` opened by the caller at a screen cell
   or widget anchor through `OverlayHost`. Same popup rules; no menubar. It is
   non-modal, dismisses on click-outside/Esc, and restores focus.

### Dialogs

5. **Dialog.** `Dialog(title, body, actions, result)` is a modal overlay:
   - border/title, body content, action row bottom-right;
   - size = content preferred + border/padding, clamped to the screen;
   - if the body exceeds the available height, the body scrolls; actions stay
     visible;
   - focus trap: Tab/Shift-Tab cycle inside, Esc triggers cancel, Enter
     triggers the default action;
   - click outside does not dismiss a modal; a shadow may be painted;
   - closing restores the previous focus.
   `DialogAction(label, default, cancel, onActivate)`; `DialogResult`
   (`CONFIRMED`, `CANCELLED`, or a key/payload) is delivered once.
6. **ConfirmDialog.** Title, message, confirm/cancel labels; default confirm,
   cancel on Esc; returns `Boolean`/`DialogResult`.
7. **PromptDialog.** Title, label, `TextField`, confirm/cancel; initial value,
   password/readOnly options, submit on Enter; returns text or cancel. Validation
   is app-owned; the dialog only displays an error string.

### Overlays

8. **Popover.** Anchored arbitrary content; placement `ABOVE/BELOW/LEFT/RIGHT/
   AUTO`; flips/clamps; dismiss `CLICK_OUTSIDE`, `ESCAPE`, `BOTH`, `NONE`;
   optional modal focus trap. No automatic window ownership.
9. **Tooltip.** Content anchored to a widget/rect; shows after a hover/focus
   delay (default 600 ms), hides on move-away, focus loss, click, or Esc;
   max width with word wrap; flips above/below; one tooltip at a time. Uses
   `Scheduler`. The controller observes the hover/focus state; it never starts
   a thread.
10. **Toast.** `ToastManager` shows stacked messages at a screen corner
    (default bottom-right), max 3 visible, severity colors/icons, auto-dismiss
    after a duration (default 3 s), manual dismiss via `CLOSE`; no modal focus.
    Uses `Scheduler`; timers cancel on dismiss/detach.
11. **Focus and input order.** Overlays receive pointer/key events before the
    root. Modal entries trap focus; non-modal entries restore focus on close.
    Click-outside dismisses only the topmost non-modal layer; clicks inside a
    child popup do not dismiss its parent menu.
12. **Tests.** Menu keyboard/mouse/mnemonics, submenu flip, disabled/checked,
    shortcut display, scroll for tall menus; context menu; dialog focus trap/
    Esc/Enter/clamp/scroll/shadow/result; confirm/prompt; popover dismiss/flip;
    tooltip delay/hide/max width/flip; toast stack/timer/dismiss; z-order and
    focus restore; 1×1, 80×24, 240×67 goldens.
13. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Menubar opens by F10, mnemonic, and mouse; Left/Right switch; Esc closes
      and restores focus.
- [x] Popup menus flip/clamp, scroll when tall, show check/radio/submenu
      markers and right-aligned shortcuts, and skip disabled items.
- [x] Activating a menu item runs its command exactly once and restores focus.
- [x] Context menus open at the requested anchor and dismiss correctly.
- [x] Dialogs are modal, clamp to the screen, scroll their body, keep actions
      visible, trap focus, and map Esc/Enter to cancel/default.
- [x] Confirm/Prompt return the documented result; prompt validation is
      display-only.
- [x] Popovers flip/clamp and honor their dismiss policy.
- [x] Tooltips show after the delay, hide correctly, wrap at max width, and
      flip at screen edges.
- [x] Toasts stack to the max, auto-dismiss, and can be closed manually.
- [x] Overlays receive input before the root; modal traps work; above layers
      do not leak clicks to lower layers.
- [x] Timers use `Scheduler` only; no threads.
- [x] Every overlay passes the conformance matrix at 1×1 and 80×24.

## Pitfalls

- Do not let a menu click-through to the widget underneath.
- Do not dismiss a parent menu when clicking inside a submenu.
- Do not run a disabled command or run a command twice (menu close + keymap).
- Dialog body scroll must not hide the action row.
- Enter must not activate a default action when a text field should consume it.
- Tooltip timers must be canceled on detach; no stale overlay.
- Toast timers must not keep a detached host alive.
- Do not use wall-clock time; use `Scheduler` and the fake clock in tests.
- Do not add a second overlay host; use `GuiSurface.overlays`.

## Plan seed

> Plan section 12 only. Implement MenuBar/Menu/MenuPopup/ContextMenu,
> Dialog/ConfirmDialog/PromptDialog, Popover, Tooltip/TooltipController, and
> Toast/ToastManager on top of `OverlayHost`, `Command`, `Scheduler`, and the
> existing buttons/text fields. Add keyboard, mouse, flip/clamp, focus trap,
> timer, z-order, and golden tests. No window ownership.
