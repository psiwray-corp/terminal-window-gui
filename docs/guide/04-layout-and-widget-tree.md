# 04 — Layout engine and widget tree

**Phase:** core · **Depends on:** 03 · **Blocks:** 05

## Goal

A deterministic integer-cell layout engine: constraints, one measure path,
local widget bounds, static and flex containers, and relative/absolute
positioning. No event routing or widgets beyond layout primitives.

## Context budget

Read: `GUIDE.md`, this file, section 03's `PaintContext`/`CellCanvas`, and the
`terminal-window` matrix/architecture docs. Do not read sibling sections.
Expected output: geometry, `Widget` tree, layout containers, tests. Split seam
if needed: 04a geometry/tree, 04b containers.

## Deliverables

- `.../layout/IntSize.kt`, `Rect.kt`, `Constraints.kt`, `Axis.kt`,
  `Alignment.kt`, `Length.kt`, `Overflow.kt`.
- `.../widget/Widget.kt`, `ContainerWidget.kt`, `SingleChildWidget.kt`,
  `MultiChildWidget.kt`.
- `.../widget/Row.kt`, `Column.kt`, `Flexible.kt`, `Expanded.kt`, `Spacer.kt`.
- `.../widget/Stack.kt`, `Positioned.kt`, `Align.kt`, `Center.kt`,
  `FractionallySizedBox.kt`.
- `.../widget/Grid.kt`, `GridTrack.kt`, `Wrap.kt`.
- `.../widget/Padding.kt`, `SizedBox.kt`, `ConstrainedBox.kt`.
- Tests: `GeometryTest`, `WidgetTreeTest`, `RowColumnLayoutTest`,
  `StackPositioningTest`, `GridLayoutTest`, `WrapLayoutTest`,
  `ConstraintTest`, `LayoutGoldenTest`.

## Steps

1. **Geometry.** `IntSize(width, height)` and `Rect(x, y, width, height)` are
   immutable values in cells. `Rect` uses half-open bounds: `right = x + width`,
   `bottom = y + height`; `contains` is `x <= px < right && y <= py < bottom`.
   Reject negative sizes. Provide `intersect`, `offset`, `inset`, and
   `localToParent` helpers.
2. **Constraints.** `Constraints(minWidth, maxWidth, minHeight, maxHeight)`
   with `loosen()`, `tighten()`, `constrain(IntSize)`, and
   `hasBoundedWidth/Height`. Normalize invalid ranges: if `min > max`, clamp
   `max` to `min` and assert in debug tests. `maxWidth/maxHeight` may be
   `Int.MAX_VALUE` for scrollables.
3. **Widget base.** `Widget` stores `bounds: Rect` **relative to the parent
   content box**, `parent`, and a layout/measure cache revision. Public
   operations:
   - `measure(constraints): IntSize` (protected abstract, one path);
   - `arrange(bounds: Rect)` stores local bounds and calls
     `arrangeChildren()`;
   - `paint(context: PaintContext)` (protected abstract);
   - `invalidateLayout()` bubbles to the parent and marks the surface;
   - `invalidatePaint()`;
   - `onAttach()`, `onDetach()`, `onBoundsChanged(old, new)`.
   Section 05 adds focus/event members to this class; do not pre-implement
   them here.
4. **No Swing-style sizes.** Do not add mutable `preferredSize`,
   `minimumSize`, or `maximumSize`. Preferred is the result of `measure`
   under loose constraints; minimums are content clamps and `ConstrainedBox`;
   maximums are parent constraints or `ConstrainedBox`. Fixed sizes use
   `SizedBox`; fractions use `FractionallySizedBox`/`Positioned`.
5. **Containers.** `ContainerWidget` owns ordered children and a
   `contentRect` relative to its own bounds (border/padding excluded).
   `MultiChildWidget` provides `add`, `remove`, `insert`, `clear`, and
   `children` as an immutable view. `SingleChildWidget` owns one child.
   Reparenting invalidates layout; removing the focused child is handled in
   section 05.
6. **Coordinate contract.** A child's `bounds` is relative to the parent
   **content origin**. When painting children, the parent pushes
   `contentRect.x + child.bounds.x`, `contentRect.y + child.bounds.y` onto the
   `PaintContext`. The root widget is arranged at `(0, 0, columns, rows)`.
   Screen coordinates appear only through the surface/overlay boundary.
7. **Clipping.** Every container clips to its own content rect while painting
   children. `ContainerWidget` pushes a clip before children and pops it after.
   `Overflow.CLIP` is the default; scroll containers are section 07.
8. **Row / Column.** Both use the same flex algorithm:
   - measure non-flex children with unbounded main axis and bounded cross axis;
   - sum preferred main sizes plus gaps;
   - distribute remaining space by flex weight; `Expanded` is tight flex,
     `Flexible` is loose;
   - if space is insufficient, shrink non-flex children toward their measured
     minimum before clipping;
   - distribute integer rounding remainder to the last flex child;
   - arrange in order with `mainAxisAlignment` (`START`, `CENTER`, `END`,
     `SPACE_BETWEEN`) and `crossAxisAlignment` (`START`, `CENTER`, `END`,
     `STRETCH`);
   - `mainAxisSize` is `MIN` or `MAX`.
   `gap` is in cells; negative gaps are invalid.
9. **Stack / Positioned.** `Stack` paints children in order, later on top.
   `StackFit.LOOSE` measures non-positioned children with loose constraints;
   `StackFit.EXPAND` tightens them. The stack size is the largest
   non-positioned child, then constrained. `Positioned` accepts `left`, `top`,
   `right`, `bottom`, `width`, `height` as `Length`:
   - `Length.Cells(n)`: absolute cells, negative allowed only for positions;
   - `Length.Fraction(f)`: `0.0..1.0` of the parent content extent;
   - `Length.Content`: measured intrinsic size;
   - `Length.Fill`: fill the available extent.
   Left+right derive width; top+bottom derive height. Specifying both an edge
   pair and an explicit size throws. Fractions resolve with `roundToInt`,
   clamp to non-negative sizes, and are deterministic.
10. **Grid.** `Grid(columns: List<GridTrack>, rows: List<GridTrack>, gap,
    children)` places children row-major into cells. `GridTrack.Fixed(cells)`,
    `GridTrack.Fraction(f)`, `GridTrack.Flex(weight)`. Uniform-track helpers
    are allowed. Cell spans are v1.1; a child larger than its cell is clipped
    or aligned, never allowed to overlap the next cell. Rows may be omitted
    only when the row count can be derived and rows use content height.
11. **Wrap.** `Wrap` measures children with the available width, places them
    left-to-right, wraps when the next child does not fit, and sets each run's
    height to the tallest child. `gap` and `runGap`; cross-axis alignment.
    No virtualization.
12. **Wrappers.**
    - `Padding(insets, child)` shrinks the child constraints by insets.
    - `Align(alignment, child)` positions a child at a fractional alignment.
    - `Center(child)` is `Align(CENTER)`.
    - `SizedBox(width, height, child)` tightens constraints.
    - `ConstrainedBox(minWidth, maxWidth, minHeight, maxHeight, child)` clamps.
    - `Spacer(flex)` is an empty flexible child.
    - `FractionallySizedBox(widthFactor, heightFactor, child)` sizes relative
      to the incoming constraints.
13. **Measure caching.** Cache by `(constraints, contentRevision)` in the
    widget. Invalidate on content/style/layout changes. Measurement must be
    pure, idempotent, and must not paint or allocate per cell.
14. **Tests.** Geometry bounds; constraint normalization; Row/Column flex and
    remainder; cross alignment; gap; Stack pinning/fractions/conflict;
    Grid tracks; Wrap runs; wrappers; clipping; 1×1, 80×24, 240×67; idempotent
    layout; custom widget using only `measure`/`paint`.
15. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## API sketch

```kotlin
public abstract class Widget {
    public var bounds: Rect              // local to parent content
    public val parent: Widget?
    protected abstract fun measure(constraints: Constraints): IntSize
    protected open fun arrangeChildren() {}
    protected abstract fun paint(context: PaintContext)
    public open fun onAttach() {}
    public open fun onDetach() {}
    public open fun onBoundsChanged(old: Rect, new: Rect) {}
    public fun invalidateLayout()
    public fun invalidatePaint()
}

public sealed interface Length {
    public data class Cells(val value: Int) : Length
    public data class Fraction(val value: Float) : Length
    public data object Content : Length
    public data object Fill : Length
}
```

## Acceptance

- [x] Child bounds are local to the parent content box; no screen coordinates
      appear in widget layout or paint.
- [x] `measure` is the only size path; no mutable preferred/min/max fields.
- [x] `Constraints` never yields negative sizes; `min > max` is normalized and
      tested.
- [x] Row/Column distribute flex exactly, including integer remainder; gaps and
      cross alignment are correct.
- [x] `Expanded` is tight, `Flexible` is loose, `Spacer` works.
- [x] `Stack` sizes to non-positioned children; positioned children do not
      affect intrinsic size.
- [x] `Positioned` left+right and top+bottom derive size; explicit size plus an
      edge pair throws; fractions round deterministically.
- [x] `Grid` places row-major children in tracks without overlap; `Wrap` wraps
      at the correct width and uses the tallest run height.
- [x] Containers clip children to their content rect.
- [x] Layout is idempotent: the same constraints and content produce the same
      bounds twice.
- [x] 1×1, 80×24, and 240×67 layouts pass golden tests.
- [x] A custom widget can participate using only `measure`/`paint`.
- [x] No event/focus behavior, non-layout widget, or GL behavior is
      implemented here.

## Pitfalls

- Do not make `bounds` absolute. The parent content origin is the coordinate
  system; screen coordinates are an overlay concern.
- Do not measure flex children before non-flex children; the remaining space is
  unknown.
- Do not let a flex child exceed its constraints; clamp after distribution.
- Integer division loses remainder; assign it deterministically.
- `Length.Fraction` rounding can overflow a small parent; clamp after rounding.
- A `Stack` with only positioned children has no intrinsic size; require
  bounded constraints or a non-positioned child.
- Clipping is half-open; use `right`/`bottom`, not `<=`.
- Do not mutate children during `arrangeChildren`; invalidate and retry.
- A zero-size child must not be measured or painted recursively.
- `min > max` from a caller is a bug; normalize, do not propagate negative
  sizes.

## Plan seed

> Plan section 04 only. Implement geometry, `Constraints`, the local-coordinate
> `Widget`/container tree, Row/Column flex, Stack/Positioned with `Length`,
> Grid tracks, Wrap, Padding/Align/Center/SizedBox/ConstrainedBox/Spacer, and
> clipping. Add geometry, flex, positioning, idempotence, and golden layout
> tests. No event/focus behavior, non-layout widgets, or window code.
