# 10 — Collections

**Phase:** widgets · **Depends on:** 09 · **Blocks:** 11

## Goal

Virtualized data views: ListView, TableView, TreeView, and a shared selection
model. Stable keys, row caching, type-ahead, keyboard/mouse navigation, sorting
indicator, horizontal scrolling, and expansion. No in-cell editing.

## Context budget

Read: `GUIDE.md`, this file, sections 07–09 public types, and the conformance
matrix. Do not read sibling sections. Expected output: selection model and three
collections plus tests. Split seam if needed: 10a selection/ListView, 10b
Table/Tree.

## Deliverables

- `.../widget/SelectionModel.kt`, `SelectionMode.kt`.
- `.../widget/ListView.kt`, `TableColumn.kt`, `ColumnWidth.kt`,
  `TableCell.kt`, `TableView.kt`, `TreeNode.kt`, `TreeView.kt`.
- `.../internal/VisibleRowCache.kt`, `TypeAhead.kt`, `ColumnLayout.kt`.
- Tests: `SelectionModelTest`, `ListViewTest`, `TableViewTest`, `TreeViewTest`,
  `VirtualizationTest`, plus golden fixtures.

## Steps

1. **Selection model.** `SelectionMode { NONE, SINGLE, MULTIPLE }`.
   `SelectionModel` stores selected stable keys (`LinkedHashSet<Any>`), a
   cursor key, and an anchor key. Methods: `select(key, additive, range)`,
   `toggle`, `clear`, `selectAll`, `isSelected`, `cursor`, `anchor`,
   `moveCursor(delta, extend)`, `setCursor(key)`. In `SINGLE`, cursor is always
   selected. In `MULTIPLE`, cursor may be unselected; Shift ranges use the
   anchor. Stable keys are required for selection preservation across sorting
   and data changes; default `keyOf` is the item index and is documented as
   unstable.
2. **Visible-row cache.** Collections create widgets only for visible rows,
   cache them by stable key, and invalidate on data revision, key change, or
   item-builder change. Painting a 10,000-item list must create only the
   viewport rows; a paint counter test proves it.
3. **ListView.** `ListView<T>(items, itemBuilder, keyOf, selectionMode,
   selection, onActivate, emptyText)`.
   - One row per item, height exactly 1. `itemBuilder(index, item)` returns an
     arbitrary widget that must fit one row; use `Row`/`Stack` inside for
     columns.
   - Keyboard: Up/Down, PageUp/PageDown, Home/End, Space toggles in multiple,
     Shift+Up/Down extends, Ctrl+A selects all, Enter activates, type-ahead.
   - Mouse: click selects and sets cursor; Ctrl toggles; Shift ranges;
     double-click activates; wheel scrolls.
   - Type-ahead: printable characters, 500 ms timeout, prefix match from the
     cursor, wraps at the end.
   - Empty state: one non-focusable muted label.
   - Minimum 1×1; vertical scrollbar when content exceeds the viewport.
4. **TableColumn / ColumnWidth.** `TableColumn<T>(title, width, alignment,
   cell)`. `ColumnWidth.Fixed(cells)`, `Flex(weight)`, `Fraction(f)`; minimum
   1 cell. `cell(item)` returns `TableCell(text, icon?, fg?, bg?)`; text is
   truncated with ellipsis, icon is one cell. Cell widgets are not supported in
   v1; in-cell editing is v1.1.
5. **TableView.** Header row (height 1), body rows (height 1), vertical
   virtualization, horizontal scrolling when the sum of minimum column widths
   exceeds the viewport. Header shows titles, alignment, and `SORT_ASCENDING`/
   `SORT_DESCENDING` on the active column. Sorting is app-owned:
   `onHeaderClick(columnIndex)` and `sortColumn/sortDirection` are display
   state; the table never reorders data.
   - Keyboard: Up/Down/Page/Home/End move the cursor; Enter activates; Shift/
     Ctrl selection as in ListView; Left/Right scroll horizontally.
   - Mouse: header click, row click/selection, double-click activate, wheel.
   - Empty state and minimum 1×1.
6. **TreeNode / TreeView.** `TreeNode<T>(key, label, icon?, children)` is eager
   in v1; lazy children are v1.1. `TreeView(nodes, keyOf, label, children,
   expandedKeys, selection, onActivate)`.
   - Flatten visible nodes on data/expansion revision; cache the flat list.
   - Indent two cells per depth; optional guide lines using `TREE_*` shapes;
     expander uses `TREE_EXPANDED`/`TREE_COLLAPSED`; leaf rows leave the
     expander cell blank.
   - Keyboard: Up/Down visible rows; Right expands or moves to first child;
     Left collapses or moves to parent; Space toggles; Enter activates; Home/
     End; PageUp/PageDown; type-ahead.
   - Single selection in v1; multiple selection is v1.1.
   - Virtualize visible flat rows; row cache by node key.
   - Empty state and minimum 1×1.
7. **Scrolling and focus.** Collections own a `ScrollModel` and optional
   `ScrollBar`; ensure the cursor is visible after every move, data change, and
   resize. Focus stays on the collection, not individual rows.
8. **Tests.** Selection modes; stable-key preservation across reorder;
   type-ahead; page/home/end; shift/ctrl; virtualization paint counts; list
   empty/one/many; table fixed/flex/fraction widths, truncation, header, sort
   indicator, horizontal scroll; tree expand/collapse, deep indent, guide
   lines, keyboard parent/child movement; 1×1 and 80×24 goldens.
9. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] `ListView` paints only viewport rows; a 10,000-item list creates no more
      than the visible row count plus a small overscan.
- [x] Selection is preserved by stable key across reorder/data change.
- [x] ListView keyboard, mouse, shift/ctrl, double-click, wheel, and type-ahead
      work.
- [x] Table columns honor fixed/flex/fraction widths and minimum 1 cell.
- [x] Table header and sort indicator render; sorting is app-owned.
- [x] Table horizontal scroll works and cell text truncates.
- [x] Tree expand/collapse, indent, guide lines, and keyboard parent/child
      movement work.
- [x] Tree only paints visible flattened rows.
- [x] Cursor stays visible after move/data change/resize.
- [x] Empty/one/many states render at 1×1 and 80×24.
- [x] No in-cell editing, variable-height rows, lazy tree children, or
      content-width columns are implemented.
- [x] Every collection passes the conformance matrix.

## Pitfalls

- Do not create a widget for every item; virtualize.
- Do not use unstable index keys for sorted tables; document `keyOf`.
- Do not let a cell widget escape its column rect; clip and truncate.
- Do not let a table header scroll vertically; it is frozen.
- Do not reorder data on header click; only report the intent.
- Do not expand a leaf or move to a non-existent child.
- Do not lose focus when the selected row scrolls out of view.
- Do not recompute the flattened tree every paint; cache by revision.
- Do not allocate a row widget per paint; reuse the visible-row cache.

## Plan seed

> Plan section 10 only. Implement `SelectionModel`, `ListView`, `TableView`
> (fixed/flex/fraction columns, header, sort indicator, horizontal scroll), and
> `TreeView` (eager nodes, expansion, indent, guide lines) with stable-key row
> caching, virtualization, type-ahead, keyboard/mouse navigation, and golden
> tests. No in-cell editing, lazy nodes, or variable-height rows.
