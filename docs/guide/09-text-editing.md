# 09 — Text editing

**Phase:** widgets · **Depends on:** 07, 08 · **Blocks:** 10

## Goal

A shared editing core and four text widgets: TextField, SearchField, TextArea,
and read-only LogView. Caret, selection, undo/redo, clipboard, masking,
horizontal/vertical scrolling, and wrapping. No IME.

## Context budget

Read: `GUIDE.md`, this file, sections 03–08 public types, and the conformance
matrix. Do not read sibling sections. Expected output: controller/editing model
and the four widgets plus tests. Split seam if needed: 09a controller/TextField,
09b TextArea/LogView.

## Deliverables

- `.../text/TextEditingController.kt`, `TextSelection.kt`, `EditHistory.kt`,
  `TextEdit.kt`, `WordBoundary.kt`, `Caret.kt`.
- `.../widget/TextField.kt`, `SearchField.kt`, `TextArea.kt`, `LogView.kt`,
  `TextAreaWrap.kt`, `LineNumbers.kt`.
- Tests: `TextEditingControllerTest`, `TextFieldTest`, `SearchFieldTest`,
  `TextAreaTest`, `LogViewTest`, `TextEditFuzzTest`, plus golden fixtures.

## Steps

1. **Controller.** `TextEditingController(text = "")` owns:
   - `text: String`;
   - `selection: TextSelection(anchor, focus)` with normalized `start`, `end`,
     `isCollapsed`; `caret = focus`;
   - `maxLength` in Unicode code points (default unlimited);
   - `canUndo/canRedo`, `undo()`, `redo()`;
   - `setText`, `insert`, `deleteBefore/After`, `replaceSelection`,
     `selectAll`, `cut/copy/paste(Clipboard)`;
   - `moveCaret(deltaCodePoints, extendSelection)`, `moveWord(direction,
     extendSelection)`, `home/end(extendSelection)`, `lineUp/Down`;
   - `revision` for invalidation.
   Character indices are UTF-16 char indices, but every movement and deletion
   snaps to code-point boundaries; a surrogate pair is never split.
2. **Undo/redo.** Store bounded edit commands or snapshots (max 200). Typing
   coalesces within a 500 ms window and at the same caret; cursor movement,
   paste, cut, and delete break coalescing. Undo/redo restores text, selection,
   and caret. No wall-clock dependency in tests; the controller clock is
   injectable or edits carry a timestamp from `GuiKeyEvent` handling.
3. **Word boundaries.** v1 word characters are ASCII letters, digits, and
   underscore; punctuation is its own boundary. `Ctrl+Left/Right` moves by
   word; `Ctrl+Backspace/Delete` deletes by word. Document that Unicode word
   segmentation is v1.1.
4. **TextField.** Single row, no wrap. Properties: controller, placeholder,
   mask (`*` when set), readOnly, `error`, onSubmit, onValueChange, horizontal
   scroll offset. Render selection background, caret as an inverted cell,
   placeholder when empty and unfocused. Caret is hidden when unfocused.
   Preferred height 1; minimum 1×1. Horizontal scroll follows the caret.
5. **SearchField.** TextField plus a clear button (`CROSS`) and submit. Clear
   focuses the field and empties it; submit calls `onSearch(text)`. Clear
   button is focusable by mouse only or participates in Tab order; document
   the chosen behavior (recommended: Tab order).
6. **TextArea.** Multi-line controller, `TextWrap.NONE/WORD/CHAR`, optional
   line numbers, vertical `ScrollModel`, focusable. Wrap `NONE` uses horizontal
   scrolling; wrapped modes map caret to visual lines. `Enter` inserts a
   newline; `PageUp/PageDown`, `Ctrl+Home/End`, arrows, selection, undo/redo,
   clipboard. Minimum 1×1; line numbers consume width and are right-aligned.
7. **LogView.** Read-only lines (list of strings or a controller in read-only
   mode), wrap mode, `followTail`, scroll lock indicator. New lines appended
   while `followTail` keep the bottom visible; scrolling up disables follow
   until the user returns to the bottom. No editing, no caret; selection/copy
   is optional v1.1 unless implemented with the shared selection model (if
   implemented, tests are mandatory).
8. **Key handling.** Printable input arrives as `GuiCharEvent`; do not
   reconstruct characters from key codes. Handle Backspace/Delete,
   Left/Right, Home/End, Ctrl+Left/Right, Shift+arrows/Home/End, Ctrl+A/C/X/V,
   Ctrl+Z/Y, PageUp/PageDown, Enter, Tab (focus traversal, not insertion).
   TextField strips newlines from pasted text or replaces them with spaces;
   TextArea inserts them. Password fields disable copy/cut; paste is allowed.
9. **Mouse.** Click sets caret; drag extends selection; double-click selects a
   word; triple-click selects a line (TextArea/LogView). Wheel scrolls
   TextArea/LogView. Clicking outside does not lose focus; clicking another
   widget does via the focus manager.
10. **Painting.** Use cached `TextLayout` from section 03; never wrap during
    paint. Selection uses `selectionBackground`/`selectionForeground`; caret
    uses `caret` or inversion. Ensure the caret is visible after edits, focus,
    resize, and scroll. No blinking in v1; optional blink is v1.1.
11. **No IME.** `CharEvent` is committed text only. Preedit/composition and
    candidate-window placement are v1.1 and blocked on `terminal-window`.
12. **Tests.** Controller insert/delete/replace; surrogate boundaries;
    selection direction; undo/redo/coalescing; cut/copy/paste; max length;
    password masking; word navigation; TextField horizontal scroll; TextArea
    wrap/caret mapping/line numbers; LogView follow-tail; mouse click/drag/
    double/triple; resize; read-only; empty/1×1/80×24 goldens; fuzz random
    edits against a reference string model.
13. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Every edit and caret movement preserves surrogate pairs.
- [x] Selection has a stable anchor/focus, supports forward/backward, and
      replacement deletes exactly the selected range.
- [x] Undo/redo restores text, selection, and caret; coalescing is bounded and
      tested.
- [x] Cut/copy/paste use the `Clipboard` interface; password fields do not
      expose text through copy/cut.
- [x] TextField is one row, scrolls horizontally, and keeps the caret visible.
- [x] TextArea wraps by word/char/none, scrolls vertically, and maps caret up/
      down across visual lines correctly.
- [x] LogView follows the tail until the user scrolls up and re-enables at the
      bottom.
- [x] Mouse click/drag/double-click/triple-click and wheel work.
- [x] Read-only widgets reject edits but still allow focus and scrolling.
- [x] The fuzz test matches a reference model for random inserts, deletes,
      selections, and undo/redo.
- [x] No IME, text attributes, or hardcoded shape codepoints are present.
- [x] Every widget passes the conformance matrix.

## Pitfalls

- Do not store the caret in visual columns; store it in the text and map to
  visual position after wrap.
- Do not split a surrogate pair when moving, deleting, or limiting length.
- Do not let undo records grow without bound; 200 entries is enough.
- Do not insert a tab on Tab; Tab is focus traversal.
- Do not treat Ctrl+C as a character event; handle it as a key chord.
- Do not wrap text during paint; cache the layout.
- A pasted newline into a TextField must not create a second visual line.
- Do not blink the caret by default; blinking forces repaint and is v1.1.
- LogView selection is not free; if implemented, test it fully or defer it.

## Plan seed

> Plan section 09 only. Implement `TextEditingController` with selections,
> code-point-safe edits, undo/redo, word navigation, and clipboard; then
> `TextField`, `SearchField`, `TextArea`, and `LogView` with caret painting,
> masking, wrap, scroll, line numbers, and follow-tail. Add exhaustive editing,
> mouse, resize, and fuzz tests. No collections, dialogs, IME, or window code.
