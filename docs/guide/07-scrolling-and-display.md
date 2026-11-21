# 07 — Scrolling and display primitives

**Phase:** widgets · **Depends on:** 06 · **Blocks:** 08, 09

## Goal

Scrolling infrastructure and the first visible widgets: Panel, Label/RichLabel,
Separator, Badge, Icon, ProgressBar, Spinner, Banner. All shapes come from
`ResolvedShapeSet`; all coordinates are local.

## Context budget

Read: `GUIDE.md`, this file, sections 03–06 public types, and the shape table in
section 03. Do not read sibling sections. Expected output: scroll models/widgets
and display widgets plus tests. Split seam if needed: 07a scrolling, 07b
display.

## Deliverables

- `.../widget/ScrollModel.kt`, `ScrollBar.kt`, `ScrollView.kt`,
  `ScrollBarVisibility.kt`, `Orientation.kt`.
- `.../widget/Panel.kt`, `BoxStyle.kt`, `Label.kt`, `RichLabel.kt`,
  `TextSpan.kt`, `Separator.kt`, `Badge.kt`, `Icon.kt`, `ProgressBar.kt`,
  `Spinner.kt`, `Banner.kt`, `Severity.kt`.
- `.../internal/BoxPainter.kt`, `ScrollThumb.kt` (implementation details).
- Tests: `ScrollModelTest`, `ScrollViewTest`, `ScrollBarTest`,
  `PanelTest`, `LabelTest`, `RichLabelTest`, `SeparatorTest`, `BadgeTest`,
  `IconTest`, `ProgressBarTest`, `SpinnerTest`, `BannerTest`, plus golden
  fixtures for each widget.

## Steps

### Scrolling

1. **ScrollModel.** Mutable view state:
   `contentWidth`, `contentHeight`, `viewportWidth`, `viewportHeight`,
   `offsetX`, `offsetY`, `followEnd`. Clamp offsets on every content/viewport
   change. Methods: `scrollBy(dx, dy)`, `scrollTo(x, y)`,
   `ensureVisible(x, y, width, height)`, `pageUp/pageDown`, `home/end`,
   `maxOffsetX/Y`. Revision counter for invalidation.
2. **ScrollView.** One child. Measure with unbounded main axis and viewport
   cross axis; content size is the child’s preferred size. Clip to the viewport
   and paint the child through a `PaintContext` origin shifted by
   `(-offsetX, -offsetY)`. Pointer events are translated by `(+offsetX,
   +offsetY)`. Wheel scrolls; keyboard Up/Down/PageUp/PageDown/Home/End when
   focused. `followEnd` keeps the bottom visible when content grows.
3. **ScrollBar.** Orientation, model, visibility (`ALWAYS`, `AUTO`, `NEVER`).
   Draw track/thumb/arrows with `SCROLL_*` shapes. Thumb length is
   `max(1, viewport * track / content)`, position from offset. Track click
   pages; arrow click scrolls one line; thumb drag captures the pointer and
   maps cell delta to offset. Keyboard when focusable. Hide when content fits
   and visibility is `AUTO`.
4. **Layout interaction.** A vertical scrollbar consumes one column, a
   horizontal one consumes one row. If the viewport would become zero,
   hide the scrollbar or clip the child; never allocate negative size. A
   `ScrollView` is at least 1×1.

### Display primitives

5. **Panel.** Single child, optional title, `BoxStyle` (`SINGLE`, `DOUBLE`,
   `ROUNDED`; `HEAVY` reserved v1.1), padding, background, focus ring. Border
   consumes one cell per side; title is painted into the top edge with one cell
   of padding and truncated with ellipsis. Minimum 2×2; with a title, enough
   cells for corner + space + one title cell + space + corner. Focus must be
   visible without color: use the focused border color plus an inverted title
   or a `>` corner marker. `BoxPainter` composes corners, edges, and optional
   tees; it never hardcodes a glyph.
6. **Label.** Single/multi-line text, `TextAlignment`, `TextWrap`, `maxLines`,
   ellipsis. Measure with `TextMetrics`; paint precomputed lines; truncated
   last line ends with `Shape.ELLIPSIS`. Minimum 1×1. No attributes.
7. **RichLabel.** Ordered `TextSpan(text, fg?, bg?)`; spans flow through word
   wrap; colors only. Same truncation rule. Measure and paint must not split
   surrogate pairs or tabs.
8. **Separator.** Horizontal or vertical rule, optional centered label.
   Uses `BOX_*_HORIZONTAL`/`BOX_*_VERTICAL`; one cell thick. Minimum 1×1.
9. **Badge.** One-line text with background, optional foreground; minimum text
   width; truncates with ellipsis; no border by default.
10. **Icon.** One cell from `Shape` or an explicit code point. `Icon.shape`
    uses `ResolvedShapeSet`; `Icon.glyph` relies on the font fallback. Minimum
    1×1; no image support.
11. **ProgressBar.** Determinate `value: Double` in `0.0..1.0`, optional
    percentage label, optional `indeterminate`. Determinate painting selects
    `PROGRESS_EMPTY`, the correct eighth block, and `PROGRESS_FULL`; ASCII
    fallback renders `#`. Indeterminate uses a moving fill driven by
    `Scheduler`; stop when detached. Minimum 3 cells; one row.
12. **Spinner.** Four frames from `SPINNER_0..3`; `start`/`stop`; tick via
    `Scheduler`; one cell. Detach stops the timer. No thread.
13. **Banner.** One row: severity icon/label, message, optional action, close.
    `Severity` (`INFO`, `SUCCESS`, `WARNING`, `ERROR`) selects theme colors and
    a shape. Message truncates with ellipsis; action/close are focusable.
    Minimum 3×1. Multi-line banners are v1.1.
14. **Tests.** Scroll clamp/ensure-visible/follow-end; scrollbar thumb math and
    drag; wheel and keys; panel borders for all box styles and shape sets;
    label wrap/ellipsis; rich-label spans; separator; badge; icon fallback;
    progress partials and ASCII fallback; spinner timer with fake clock; banner
    action/close; 1×1 and 80×24 goldens.
15. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] Scroll offsets clamp for zero/one/many content sizes and after viewport
      changes.
- [x] `ensureVisible` makes the requested rect visible with the smallest offset
      change.
- [x] `followEnd` sticks to the bottom until the user scrolls up.
- [x] Scrollbar thumb length/position are correct at 0%, 50%, 100%, and when
      content fits.
- [x] Thumb drag and track paging update the model; arrows scroll one line.
- [x] Panel borders render correctly for single/double/rounded and the ASCII
      shape set; title truncates; focus is visible without color.
- [x] Label and RichLabel wrap, align, truncate, and preserve surrogate pairs;
      no shape codepoint is hardcoded.
- [x] Separator, Badge, and Icon render at 1×1 and minimum sizes.
- [x] ProgressBar maps 0–100% to the correct block; ASCII fallback works;
      indeterminate stops when detached.
- [x] Spinner advances with the fake clock and stops on detach.
- [x] Banner action and close are focusable and work by keyboard and mouse.
- [x] Every widget passes the conformance matrix: states, Unicode/ASCII shapes,
      dark/light/monochrome themes, 1×1, resize, and golden rows.
- [x] No event, focus, or GL behavior leaks outside the shared runtime.

## Pitfalls

- Do not mutate child bounds for scrolling; shift the paint origin and
  translate events.
- A scrollbar that consumes the last cell leaves no viewport; hide it instead
  of producing a zero/negative child size.
- Use integer thumbnail math with a minimum thumb of one cell; avoid division
  by zero when content equals viewport.
- `followEnd` must disable when the user scrolls up and re-enable at the
  bottom.
- Panel title truncation must account for both corners and padding.
- Do not use color alone for focus; monochrome terminals must show it.
- ProgressBar partial blocks go from 1/8 to 7/8; do not round a non-empty
  value down to empty.
- Spinner/tooltip timers are caller-driven; no background threads.
- `Icon.glyph` may fall back in the renderer; do not assume the code point is
  present.

## Plan seed

> Plan section 07 only. Implement `ScrollModel`, `ScrollView`, `ScrollBar`,
> `Panel`/`BoxPainter`, `Label`/`RichLabel`, `Separator`, `Badge`, `Icon`,
> `ProgressBar`, `Spinner`, and `Banner` using `ResolvedShapeSet` and local
> coordinates. Add clamp, thumb, wrap, border, progress, timer, and golden
> tests. No collections, text editing, or window code.
