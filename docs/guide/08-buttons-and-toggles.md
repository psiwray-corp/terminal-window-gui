# 08 — Buttons and toggles

**Phase:** widgets · **Depends on:** 07 · **Blocks:** 09

## Goal

Focusable activation and selection controls: Button, Checkbox, RadioGroup/
RadioButton, Switch, and a one-row Toolbar. Mouse, keyboard, mnemonics, states,
and truncation.

## Context budget

Read: `GUIDE.md`, this file, sections 03–07 public types, and the conformance
matrix in `GUIDE.md`. Do not read sibling sections. Expected output: the five
widget families and their tests.

## Deliverables

- `.../widget/Button.kt`, `ButtonVisuals.kt`.
- `.../widget/Checkbox.kt`, `CheckState.kt`.
- `.../widget/RadioGroup.kt`, `RadioButton.kt`.
- `.../widget/Switch.kt`.
- `.../widget/Toolbar.kt`.
- Tests: `ButtonTest`, `CheckboxTest`, `RadioGroupTest`, `SwitchTest`,
  `ToolbarTest`, plus golden fixtures for every state.

## Steps

1. **Shared activation.** Implement one internal `Activation` helper for
   press/release semantics: press inside sets `PRESSED`; release inside
   activates once; release outside or focus loss cancels; disabled ignores.
   Keyboard activation uses `Enter`/`Space` on `PRESS`; `REPEAT` does not
   re-activate. Consume the event that activates.
2. **Button.** `Button(label, icon?, default, enabled, mnemonic, onActivate)`.
   - Preferred width: icon (1 + gap if present) + label width + one cell of
     horizontal padding on each side when space allows; minimum 1×1.
   - Label truncates with `Shape.ELLIPSIS`; icon is one cell.
   - States: normal, hovered, pressed, focused, disabled, default.
   - Focus is visible without color: inverted label, bracket markers, or a
     `>`/`*` prefix. Default is marked in addition to accent color.
   - `Enter` activates the default button only when the event reaches it
     (dialogs handle default/cancel in section 12); `Space` always activates
     the focused button.
   - Mnemonic: `Alt+<char>` activates the first visible enabled button with
     that mnemonic in the current focus scope. The mnemonic character is
     highlighted by color or an inverted cell; no underline attribute.
   - Mouse: press inside sets pressed; release inside activates; drag out
     cancels.
3. **Checkbox.** `Checkbox(label, state, allowIndeterminate, enabled,
   onChanged)`. `CheckState` is `UNCHECKED`, `CHECKED`, `INDETERMINATE`.
   Render `[` + `CHECK`/space/`-` + `]` + space + label, using
   `BRACKET_LEFT`/`BRACKET_RIGHT` shapes. `Space` toggles:
   unchecked -> checked; checked -> unchecked; indeterminate -> checked when
   `allowIndeterminate`, otherwise unchecked. Mouse click on the box or label
   toggles. Preferred width is box width + gap + label; truncates.
4. **RadioGroup / RadioButton.** `RadioGroup(selectedKey, enabled, onChanged,
   children)` owns selection; `RadioButton(key, label, enabled)` is a child.
   Render `(` + `DOT`/space + `)` + space + label. Arrow keys move to the next/
   previous enabled button and select it; `Space` selects the focused button.
   Roving focus: only the selected (or last focused) button participates in
   Tab order; arrows move within the group. Mouse click selects. Exactly one
   selected key unless the group is empty. Disabled group skips all children.
5. **Switch.** `Switch(label, checked, enabled, onChanged)`. One row; render
   `ON`/`OFF` (or a theme-provided pair) with a clear focus marker. `Space`/
   `Enter` toggles; left/right select off/on. Mouse toggles. Minimum 1×1;
   label truncates.
6. **Toolbar.** One-row `Row` of Buttons, Separators, and Spacers with
   `gap = 1`; minimum height 1. No overflow menu in v1 (menu/toolbar overflow
   is v1.1). Focus traversal follows child order. Disabled items remain
   visible but skipped.
7. **State resolution.** Every control resolves `WidgetState` through `Theme`
   for fg/bg. Focus and selection must remain visible in monochrome:
   bracket/paren markers, `>` cursor, or inverted label; never color alone.
8. **Tests.** All states for every control; Unicode and ASCII shape sets;
   dark/light/monochrome; `Enter`/`Space`; press/release inside/outside;
   disabled; mnemonic activation and conflicts; label truncation at 1 cell;
   checkbox tri-state transitions; radio arrow selection and roving focus;
   switch toggling; toolbar focus order; 1×1 and 80×24 goldens.
9. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Button activates once per press; key repeat does not re-activate; drag
      out cancels.
- [x] Disabled buttons/checks/radios/switches receive no focus or activation.
- [x] Focus and default states are visible without color.
- [x] Mnemonics activate the correct control and are displayed without an
      underline attribute.
- [x] Checkbox supports two- and three-state modes with the documented
      transitions.
- [x] RadioGroup has exactly one selection, arrow navigation skips disabled
      items, and roving focus works.
- [x] Switch toggles by keyboard and mouse and renders `ON`/`OFF`.
- [x] Toolbar lays out buttons, separators, and spacers in one row and does not
      overflow silently.
- [x] Labels truncate with the ellipsis shape and never paint outside bounds.
- [x] Every control passes the conformance matrix.
- [x] No text attributes or hardcoded shape code points are used.

## Pitfalls

- Do not activate on `REPEAT`; key repeat is for navigation, not presses.
- Do not use color alone for focus/default/selection.
- A radio group must not allow zero selection after user interaction unless it
  starts empty.
- Roving focus must keep Tab and arrow behavior distinct.
- Mnemonic conflicts are allowed but must be deterministic (first visible
  enabled match).
- Do not allocate a new label string per paint; cache the truncated layout.
- The toolbar is not a flex container for arbitrary widgets; use `Row` when
  more complex layout is needed.
- Do not add toolbar overflow in v1; it is v1.1.

## Plan seed

> Plan section 08 only. Implement `Button`, `Checkbox`, `RadioGroup`/
> `RadioButton`, `Switch`, and a one-row `Toolbar` with shared activation,
> keyboard/mouse/mnemonic input, focus/default/disabled/selected states,
> ellipsis truncation, and golden tests for both shape sets and three themes.
> No text editing, collections, menus, or window code.
