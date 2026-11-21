# 03 — Fonts, shapes, cell canvas, text metrics

**Phase:** foundations · **Depends on:** 02 · **Blocks:** 04

## Goal

A semantic shape model with per-shape fallbacks, a bundled CC0 `.twf` font that
covers every standard shape, a clipped cell canvas with local-coordinate paint
context, and deterministic text measurement. No widgets, no window calls.

## Context budget

Read: `GUIDE.md`, this file, `terminal-window` `docs/font-format.md`,
`docs/guide/10-matrix-and-color.md`, `docs/guide/07-font-format.md`,
`docs/guide/08-font-parser.md`, and the `BitmapFont`/`TerminalMatrix` sources.
Do not read sibling sections. Expected output: shape/font/paint/text code and
tests. Split seam if needed: 03a shapes/font, 03b canvas/text.

## Deliverables

- `library/src/main/resources/fonts/terminal-window-gui-8x16.twf`.
- `.../shape/Shape.kt`, `ShapeSpec.kt`, `ShapeSet.kt`, `ShapeSets.kt`,
  `ResolvedShapeSet.kt`, `MissingShapePolicy.kt`, `MissingShapeException.kt`,
  `GuiFonts.kt`.
- `.../paint/CellCanvas.kt`, `PaintContext.kt`, `TextMetrics.kt`,
  `TextLayout.kt`, `TextWrap.kt`, `TextAlignment.kt`.
- `.../layout/IntSize.kt`, `Rect.kt` (geometry primitives used here; layout
  expands them in section 04).
- Tests: `ShapeCoverageTest`, `ShapeResolutionTest`, `CellCanvasTest`,
  `PaintContextTest`, `TextMetricsTest`, `ShapeCatalogGoldenTest`.
- `docs/shapes.md` with the table below, fallback rules, and font attribution.

## Standard shape code points

### Box drawing

| Role | Single | Double | Rounded | Heavy (v1.1 reserved) |
|---|---|---|---|---|
| Horizontal | U+2500 `─` | U+2550 `═` | U+2500 `─` | U+2501 `━` |
| Vertical | U+2502 `│` | U+2551 `║` | U+2502 `│` | U+2503 `┃` |
| Top-left | U+250C `┌` | U+2554 `╔` | U+256D `╭` | U+250F `┏` |
| Top-right | U+2510 `┐` | U+2557 `╗` | U+256E `╮` | U+2513 `┓` |
| Bottom-left | U+2514 `└` | U+255A `╚` | U+256F `╰` | U+2517 `┗` |
| Bottom-right | U+2518 `┘` | U+255D `╝` | U+2570 `╯` | U+251B `┛` |
| Tee down | U+252C `┬` | U+2566 `╦` | U+252C `┬` | U+2533 `┳` |
| Tee up | U+2534 `┴` | U+2569 `╩` | U+2534 `┴` | U+253B `┻` |
| Tee right | U+251C `├` | U+2560 `╠` | U+251C `├` | U+2523 `┣` |
| Tee left | U+2524 `┤` | U+2563 `╣` | U+2524 `┤` | U+252B `┫` |
| Cross | U+253C `┼` | U+256C `╬` | U+253C `┼` | U+254B `╋` |

Rounded reuses single edges, tees, and cross; only corners differ. Heavy is
reserved for v1.1 but present in the bundled font and in `Shape.entries`.

### Progress

| Role | Code point | Glyph | ASCII fallback |
|---|---|---|---|
| Empty | U+0020 | space | space |
| 1/8 | U+258F | `▏` | `#` |
| 1/4 | U+258E | `▎` | `#` |
| 3/8 | U+258D | `▍` | `#` |
| 1/2 | U+258C | `▌` | `#` |
| 5/8 | U+258B | `▋` | `#` |
| 3/4 | U+258A | `▊` | `#` |
| 7/8 | U+2589 | `▉` | `#` |
| Full | U+2588 | `█` | `#` |

### Scrollbar

| Role | Code point | Glyph | ASCII |
|---|---|---|---|
| Vertical track | U+2502 | `│` | `|` |
| Vertical thumb | U+2588 | `█` | `#` |
| Horizontal track | U+2500 | `─` | `-` |
| Horizontal thumb | U+2588 | `█` | `#` |
| Up | U+25B2 | `▲` | `^` |
| Down | U+25BC | `▼` | `v` |
| Left | U+25C0 | `◀` | `<` |
| Right | U+25B6 | `▶` | `>` |

### Indicators, tree, arrows, composition, spinner

| Role | Code point | Glyph | ASCII |
|---|---|---|---|
| Check | U+2713 | `✓` | `x` |
| Cross | U+2717 | `✗` | `X` |
| Close | U+2715 | `✕` | `x` |
| Dot | U+2022 | `•` | `*` |
| Ellipsis | U+2026 | `…` | `.` |
| Tree branch | U+251C | `├` | `+` |
| Tree last | U+2514 | `└` | `+` |
| Tree vertical | U+2502 | `│` | `|` |
| Tree expanded | U+25BE | `▾` | `v` |
| Tree collapsed | U+25B8 | `▸` | `>` |
| Arrow up | U+2191 | `↑` | `^` |
| Arrow down | U+2193 | `↓` | `v` |
| Arrow left | U+2190 | `←` | `<` |
| Arrow right | U+2192 | `→` | `>` |
| Sort ascending | U+2191 | `↑` | `^` |
| Sort descending | U+2193 | `↓` | `v` |
| Bracket left/right | U+005B / U+005D | `[` / `]` | same |
| Paren left/right | U+0028 / U+0029 | `(` / `)` | same |
| Spinner 0 | U+280B | `⠋` | `|` |
| Spinner 1 | U+2819 | `⠙` | `/` |
| Spinner 2 | U+2839 | `⠹` | `-` |
| Spinner 3 | U+2838 | `⠸` | `\` |

Menu submenu uses `TREE_COLLAPSED`; menu check uses `CHECK`; menu radio uses
`DOT`. Checkbox is composed `[` + check/space + `]`; radio is `(` + dot/space +
`)`. No Unicode Private Use Area code points.

## Steps

1. **Shape enum.** Define `Shape` with the roles above. Exact enum names:
   `BOX_<STYLE>_<ROLE>`, `PROGRESS_*`, `SCROLL_*`, `CHECK`, `CROSS`, `CLOSE`,
   `DOT`, `ELLIPSIS`, `TREE_*`, `ARROW_*`, `SORT_*`, `BRACKET_*`, `PAREN_*`,
   `SPINNER_0..3`.
2. **Shape spec and set.** `ShapeSpec(codePoint, fallbackCodePoint?)`;
   `ShapeSet(shapes: Map<Shape, ShapeSpec>, lastResortCodePoint: Int)` is
   immutable. Validate every code point is a Unicode scalar value, not a
   surrogate or control; `lastResortCodePoint` defaults to U+003F. Provide
   `with(shape, codePoint, fallbackCodePoint?)`, `ShapeSets.unicode()`,
   `ShapeSets.ascii()`, and a builder.
3. **Resolution.**
   `ShapeSet.resolve(font: BitmapFont, policy: MissingShapePolicy)` returns
   `ResolvedShapeSet`. Order: primary if `font.hasGlyph`; else per-shape
   fallback if present; else `lastResortCodePoint`; if that is absent, throw
   `MissingShapeException`. `MissingShapePolicy.FAIL` throws when any primary is
   missing, even if a fallback exists. Expose `missing`, `fallbackUsed`, and
   `lastResortUsed`.
4. **Bundled font.** Author `terminal-window-gui-8x16.twf`: 8×16 cells, UTF-8,
   `version 1`, `fallback U+003F`, ASCII 0x20–0x7E, every v1 primary shape,
   the reserved heavy set, and all ASCII fallback glyphs. License CC0-1.0 with
   attribution in the header and `docs/shapes.md`. `GuiFonts.bundled()` loads
   it with `BitmapFont.parse(InputStream)`.
5. **Coverage test.** Enumerate `Shape.entries`; assert the bundled font has
   every primary. Assert `ShapeSets.unicode()` uses the table above and
   `ShapeSets.ascii()` resolves against a minimal 95-glyph font.
6. **Cell canvas.** `CellCanvas(matrix: TerminalMatrix)` writes at absolute
   screen cells with a half-open clip rect and an origin offset. `set`, `text`,
   `fill`, `hline`, `vline`, `pushClip`, `popClip`, `pushOrigin`, `popOrigin`.
   `text` iterates code points, expands tabs, handles `\n`, stops at the clip
   and matrix edge, and returns the next column. No per-cell allocation.
7. **Paint context.** `PaintContext(canvas, resolvedShapes)` exposes
   widget-local `text`, `fill`, `shape`, `pushOrigin`, `popOrigin`,
   `pushClip`, `popClip`. A local clip is translated by the current origin and
   intersected with the canvas clip. `shape(Shape, x, y, fg, bg)` resolves the
   code point through `ResolvedShapeSet`. Stack depth is fixed and validated.
8. **Text metrics.** `TextMetrics(tabWidth = 4)` counts Unicode code points,
   one cell each. Expand tabs to the next tab stop; preserve `\n`. Reject or
   replace C0 controls other than `\n`/`\t` with a space. `measure(text,
   maxWidth, wrap, maxLines)` returns `TextLayout(lines, width, height,
   truncated)`; `TextWrap.NONE`, `WORD`, `CHAR`; word wrap breaks at spaces and
   hard-breaks words wider than `maxWidth`; `maxLines` sets `truncated`.
   `TextAlignment` is applied by widgets, not metrics.
9. **ASCII fallback set.** `ShapeSets.ascii()` maps every role to the ASCII
   fallback column. The Unicode set carries those same fallbacks, so a font
   missing a primary degrades per glyph rather than failing the UI.
10. **Tests.** Coverage, resolution order, strict failure, invalid code points,
    missing last resort, canvas clipping/origin, surrogate pairs, tabs,
    newlines, wrap/ellipsis, paint-context shape resolution, and a golden
    catalog of every shape.
11. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] The bundled font parses with `BitmapFont.parse` and has every
      `Shape.entries` primary.
- [x] `ShapeSets.unicode()` uses the exact code points above; no Private Use
      Area.
- [x] `ShapeSets.ascii()` resolves against a minimal 95-glyph font.
- [x] A missing primary uses its per-shape fallback; a missing fallback uses
      the last resort; a missing last resort throws.
- [x] `MissingShapePolicy.FAIL` throws and lists every missing primary.
- [x] `CellCanvas` never writes outside its clip or the matrix; text stops at
      the right edge; surrogate pairs occupy one cell.
- [x] `PaintContext` local origins and clips produce the expected screen cells
      for nested offsets.
- [x] `TextMetrics` handles width 1, tabs, newlines, word/char wrap, long
      words, `maxLines`, and `truncated`.
- [x] The golden shape catalog renders every shape with both shape sets.
- [x] No GL, window, or thread code is present.
- [x] No shape code point is hardcoded in widget code; only `Shape` and
      `ResolvedShapeSet` are used.

## Pitfalls

- Do not use Private Use Area code points; use the standard Unicode shapes.
- Do not use emoji presentation selectors or assume emoji width.
- `String[i]` splits surrogate pairs; iterate code points.
- A fallback must be a scalar value; avoid cycles or a fallback equal to a
  missing primary with no last resort.
- The last resort must itself be present; otherwise resolution is invalid.
- Progress partial blocks are U+258F down to U+2589, then U+2588.
- Rounded box edges reuse single code points; only corners differ.
- Do not split text with `Regex`/`String.split` during paint; measure once and
  paint precomputed rows.
- Clips are half-open; an off-by-one writes outside the widget.
- Tab width is fixed at 4 in v1; document it once and use it everywhere.

## Plan seed

> Plan section 03 only. Implement the `Shape`/`ShapeSet`/`ResolvedShapeSet`
> model with the exact Unicode code points and ASCII fallbacks above, author
> the CC0 `terminal-window-gui-8x16.twf` font with full coverage, add
> `GuiFonts.bundled()`, `CellCanvas` with clipping/origin, `PaintContext` with
> local coordinates, and `TextMetrics` with width 1 and deterministic wrap.
> Add coverage, fallback, canvas, text, and golden catalog tests. No widgets,
> no window.
