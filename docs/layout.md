# Layout

Layout is integer-cell-only and caller-driven. There is one measure path and no
mutable preferred/minimum/maximum size fields. The contracts below are covered
by the [compatibility policy](compatibility.md).

## Coordinates and constraints

- Root bounds are `Rect(0, 0, columns, rows)` in root-local coordinates.
- Child `bounds` are relative to the parent's **content box**, not the screen.
- `Constraints(minWidth, maxWidth, minHeight, maxHeight)` uses inclusive
  bounds. Negative minima normalize to `0`; a maximum below its minimum is
  raised to the minimum.
- `maxWidth == Int.MAX_VALUE` and `maxHeight == Int.MAX_VALUE` mean
  content-bounded on that axis.
- `Constraints.tight(size)`, `loose(size)`, `unbounded()`, `loosen()`,
  `tighten()`, and `constrain(size)` are the only public helpers.
- `IntSize` and `Rect` use half-open rectangles (`x .. x + width`,
  `y .. y + height`).

## Measure and arrange

```text
parent.measure(constraints) -> child.measure(childConstraints) -> IntSize
parent.arrange(child, localRect)
```

A widget must return a size inside the constraints it received. Layout is
idempotent for unchanged state and constraints. A layout cache is valid only
for one pass; state, theme, shape, data, or constraint changes invalidate it.
The root is measured with tight surface dimensions and arranged at the surface
origin.

## Lengths and flex

| Type | Meaning |
|---|---|
| `Length.Cells` | Absolute cells; edge positions may be negative |
| `Length.Fraction` | Fraction of the parent content extent in `0.0..1.0` |
| `Length.Content` | Measured intrinsic child extent |
| `Length.Fill` | Parent content extent |
| `Flexible(flex, fit)` | Shares main-axis free space; `FlexFit.LOOSE` may be smaller |
| `Expanded(flex)` | `Flexible` with `FlexFit.TIGHT` |

`Row` and `Column` support `MainAxisAlignment.START/CENTER/END/SPACE_BETWEEN`
and `CrossAxisAlignment.START/CENTER/END/STRETCH`. `mainAxisSize` is `MIN` or
`MAX`; `gap` is cells between children.

## Positioning

`Stack` overlays children in order. Its `fit` is `LOOSE` (largest child) or
`EXPAND` (bounded parent), with an alignment for non-positioned children.
`Positioned` accepts `left`, `top`, `right`, `bottom`, `width`, and `height` as
`Length` values. Width cannot be combined with both `left` and `right`; height
cannot be combined with both `top` and `bottom`.

`Align` and `Center` place a child inside the available box using
`Alignment`. `Padding` subtracts `Insets`; `SizedBox` sets tight extents;
`ConstrainedBox` clamps its child; `FractionallySizedBox` sizes from parent
fractions; `Spacer` takes flex in a `Row`/`Column`.

## Grid and Wrap

`Grid(columns, rows, gap, children, alignment)` uses `GridTrack.Fixed`,
`GridTrack.Fraction`, and `GridTrack.Flex` tracks. Children fill cells in row
order; a missing row track is content-sized. `Wrap(gap, runGap,
crossAxisAlignment, children)` flows children left-to-right and wraps at the
available width, never painting outside its content box.

## Clipping, scrolling, and rounding

`ContainerWidget.overflow` is `CLIP` by default. `ScrollView` measures its
child along an unbounded scroll axis, sets its model's content/viewport sizes,
and clips the viewport. Child bounds do not move while scrolling; the content
offset is applied during paint and hit testing.

Fractional alignment and fractions are rounded with `roundToInt` and clamped
at zero. No sub-cell positioning exists in v1.

## Virtualization

`ListView`, `TableView`, and `TreeView` build only visible rows and cache
measured row sizes for the current layout. `ScrollModel` clamps offsets after
content or viewport changes and preserves stable-key selection across reorder.

## Related

[Architecture](architecture.md) · [Input](input.md) · [Widgets](widgets.md) ·
[Public API](public-api.md) · [ADR-0007](adr/0007-one-measure-path.md)
