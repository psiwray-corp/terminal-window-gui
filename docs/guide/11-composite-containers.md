# 11 — Composite containers and navigation

**Phase:** widgets · **Depends on:** 10 · **Blocks:** 12

## Goal

Tabs, SplitPane, Form/Field, StatusBar, AppShell, and ScreenHost. These compose
earlier widgets into application structure without owning the window.

## Context budget

Read: `GUIDE.md`, this file, sections 04–10 public types, and the conformance
matrix. Do not read sibling sections. Expected output: composite widgets,
navigation, and tests. Split seam if needed: 11a layout composites, 11b shell/
navigation.

## Deliverables

- `.../widget/Tabs.kt`, `TabView.kt`, `Tab.kt`, `TabBar.kt`.
- `.../widget/SplitPane.kt`, `SplitOrientation.kt`.
- `.../widget/Form.kt`, `FormField.kt`.
- `.../widget/StatusBar.kt`, `KeyHint.kt`.
- `.../widget/AppShell.kt`, `ScreenHost.kt`, `Screen.kt`.
- Tests: `TabsTest`, `SplitPaneTest`, `FormTest`, `StatusBarTest`,
  `AppShellTest`, `ScreenHostTest`, plus golden fixtures.

## Steps

1. **Tabs / TabView.** `Tab(key, title, content, enabled)`; `TabView(tabs,
   activeIndex, onChanged)`. Tab bar height 1; active tab uses theme selection/
   accent and a non-color marker (brackets or `>`); inactive tabs are muted.
   Overflow scrolls horizontally with `SCROLL_LEFT/RIGHT` indicators; the
   active tab is always made visible. Keyboard when the tab bar is focused:
   Left/Right move, Enter/Space activates, Ctrl+Tab/Ctrl+Shift+Tab cycle.
   Mouse click activates. Content is the active tab’s widget; switching
   invalidates layout/focus and restores the previous focus for that tab.
   No close buttons in v1.
2. **SplitPane.** `SplitPane(orientation, first, second, ratio = 0.5,
   minFirst = 1, minSecond = 1)`. Divider is one cell; drag captures the
   pointer and updates the ratio; double-click resets to 0.5. Keyboard when the
   divider is focusable: arrows move the divider by one cell; Home/End clamp.
   Clamp ratio against both minima and the total; if the total is below
   `minFirst + minSecond + 1`, hide the divider and allocate proportionally
   without negative sizes. Nested splits are allowed. Resize preserves the
   ratio and clamps.
3. **Form / FormField.** `Form(children: List<FormField>, labelWidth)` lays out
   fields vertically with one blank row between fields by default.
   `FormField(label, control, helpText, errorText, required)`:
   - label column width is `Fixed`, `Fraction`, or `Content`;
   - control takes the remaining width;
   - help/error line is below the control, one row, truncated with ellipsis;
   - `errorText` uses the error color and a non-color marker (`!`);
   - focus enters the control, not the label;
   - validation is app-owned; the widget only displays strings.
   Field minimum: label/control at least 1×1; error line optional.
4. **StatusBar / KeyHint.** One row split into left, center, right sections;
   separators between key hints; truncation with ellipsis; muted text. `KeyHint
   (keys, label)` renders `Ctrl+S Save` style; a `KeyChord` formatter is shared
   with commands. No interaction; focus is not taken. Height 1; minimum 1×1.
5. **AppShell.** `AppShell(menuBar: Widget?, content: Widget, statusBar:
   Widget?)`. Lays out menu (1 row if present), content (flex), status (1 row if
   present). It does not set the window title or own a loop; a caller may read
   `currentTitle` from `ScreenHost` and apply it. The menu bar type is `Widget?`
   so section 12 can supply `MenuBar` without a dependency cycle.
6. **Screen / ScreenHost.** `Screen(key, title, content, keymap?)` with
   `onEnter`, `onExit`, `onResize`, `onBack(): Boolean`. `ScreenHost(screens,
   initialKey)` owns a stack:
   - `push(screen)`, `pop(): Boolean`, `replace(screen)`, `current`;
   - push saves focus, pop restores it; a failed back (`onBack` returns false)
     is not popped;
   - `Esc` may be routed to `onBack` only when no modal/overlay consumes it;
   - `onResize` receives the new grid before layout; screens may switch
     responsive layouts;
   - per-screen keymaps are installed while the screen is active;
   - `currentTitle` is advisory for the caller’s window title.
7. **Layout and focus.** All composites use local coordinates and existing
   containers. Hidden/inactive tab contents are not painted, measured, or
   focusable. SplitPane divider focus is optional but must be consistent.
   ScreenHost does not paint inactive screens.
8. **Tests.** Tabs overflow/scroll/active visibility/keyboard/mouse/focus
   restore; splitter drag/min/clamp/double-click/resize; form label widths/
   help/error/truncation; status sections/separators/truncation; app shell
   rows; screen push/pop/back/focus restore/resize/keymap; 1×1, 80×24, 240×67
   goldens.
9. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Tabs scroll horizontally, keep the active tab visible, and switch by
      keyboard and mouse.
- [x] Inactive tab content is not painted, measured, or focusable.
- [x] SplitPane clamps both minima, hides the divider when impossible, and
      supports drag, keyboard, double-click reset, and resize.
- [x] FormField displays label/control/help/error and never loses the control
      on truncation.
- [x] StatusBar truncates sections without overlapping.
- [x] AppShell lays out menu/content/status and does not own the window title.
- [x] ScreenHost pushes/pops/replaces, restores focus, handles back, resizes,
      and installs per-screen keymaps.
- [x] Inactive screens are not painted.
- [x] Every composite passes the conformance matrix at 1×1, 80×24, and 240×67.
- [x] No dialogs, menus, or window ownership are implemented here.

## Pitfalls

- Do not let a hidden tab/screen capture focus or paint.
- Do not let the splitter create a zero/negative pane; hide the divider when
  the total is too small.
- Ratio is a hint; always clamp after rounding.
- Do not let FormField labels grow without bound; use the configured width.
- Do not let StatusBar sections overlap; truncate each section.
- Do not set the window title from the library; expose `currentTitle` only.
- Do not push a screen without saving focus; pop must restore it.
- Do not route Esc to `onBack` when a dialog/overlay consumed it.

## Plan seed

> Plan section 11 only. Implement Tabs/TabView, SplitPane, Form/FormField,
> StatusBar/KeyHint, AppShell, and ScreenHost/Screen using local coordinates and
> existing primitives. Add overflow, splitter, form, shell, navigation, focus
> restore, resize, and golden tests. No menus, dialogs, or window ownership.
