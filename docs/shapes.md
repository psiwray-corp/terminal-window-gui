# Shapes

Semantic shape roles are defined by `com.psiwray.organization.terminal_window_gui.shape.Shape`.
Widget code requests a role; `ShapeSet.resolve` maps it to a code point for the
host `BitmapFont`. No widget hardcodes a shape code point.

- Policy: [ADR-0008](adr/0008-shapes-and-font.md)
- Implementation: [section 03](guide/03-fonts-shapes-canvas-text.md)
- Architecture: [architecture.md](architecture.md)
- Public API: [public-api.md](public-api.md)
- Build guide: [GUIDE.md](../GUIDE.md)

## Standard code points

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

Rounded edges, tees, and the cross reuse single-line code points; only the four
corners differ. Heavy roles exist in `Shape.entries` and the bundled font but
are reserved for v1.1 widgets.

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

| Role | Code point | Glyph | ASCII fallback |
|---|---|---|---|
| Vertical track | U+2502 | `│` | `\|` |
| Vertical thumb | U+2588 | `█` | `#` |
| Horizontal track | U+2500 | `─` | `-` |
| Horizontal thumb | U+2588 | `█` | `#` |
| Up | U+25B2 | `▲` | `^` |
| Down | U+25BC | `▼` | `v` |
| Left | U+25C0 | `◀` | `<` |
| Right | U+25B6 | `▶` | `>` |

### Indicators, tree, arrows, composition, spinner

| Role | Code point | Glyph | ASCII fallback |
|---|---|---|---|
| Check | U+2713 | `✓` | `x` |
| Cross | U+2717 | `✗` | `X` |
| Close | U+2715 | `✕` | `x` |
| Dot | U+2022 | `•` | `*` |
| Ellipsis | U+2026 | `…` | `.` |
| Tree branch | U+251C | `├` | `+` |
| Tree last | U+2514 | `└` | `+` |
| Tree vertical | U+2502 | `│` | `\|` |
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
| Spinner 0 | U+280B | `⠋` | `\|` |
| Spinner 1 | U+2819 | `⠙` | `/` |
| Spinner 2 | U+2839 | `⠹` | `-` |
| Spinner 3 | U+2838 | `⠸` | `\` |

Menu submenu uses `TREE_COLLAPSED`; menu check uses `CHECK`; menu radio uses
`DOT`. A checkbox composes `[` + check/space + `]`; a radio composes
`(` + dot/space + `)`. No Unicode Private Use Area code points are used.

## Fallback rules

`ShapeSet.resolve(font, policy)` uses this order:

1. the primary code point, when `font.hasGlyph(primary)` is true;
2. the per-shape fallback, when declared and present;
3. the global last resort, `U+003F` by default, when present;
4. otherwise `MissingShapeException`.

`MissingShapePolicy.FALLBACK` is the default. `MissingShapePolicy.FAIL` throws
when any primary is absent, even when a fallback exists, and lists every
missing primary.

`ShapeSets.unicode()` carries these standard primaries and the ASCII fallbacks
above. `ShapeSets.ascii()` maps every role directly to its ASCII fallback.
Box-drawing ASCII fallbacks are:

| Role group | ASCII fallback |
|---|---|
| Horizontal edges, all styles | `-` |
| Vertical edges, all styles | `\|` |
| Corners, tees, crosses, all styles | `+` |

The same fallback applies to single, double, rounded, and heavy roles. The
rounded and heavy styles therefore degrade to the same ASCII skeleton as the
single style.

## Bundled font

`library/src/main/resources/fonts/terminal-window-gui-8x16.twf` is a `.twf`
version 1 font with:

- 8 by 16 cells;
- UTF-8, LF, `fallback U+003F`;
- printable ASCII `U+0020..U+007E`;
- every standard shape primary, including the reserved heavy box set;
- every ASCII fallback glyph.

`GuiFonts.bundled()` loads the resource with `BitmapFont.parse(InputStream)`.
The shape coverage and golden catalog tests assert that every `Shape.entries`
primary is present.

## Font attribution

The bundled font is dedicated to CC0-1.0:

> Glyph data derived from `font8x8_basic` by Daniel Hepper, based on Marcel
> Sondaar / IBM public-domain VGA glyphs. New shape glyphs authored for
> terminal-window-gui. Dedicated to CC0-1.0.

- `font8x8_basic`: <https://github.com/dhepper/font8x8>
- CC0 1.0: <https://creativecommons.org/publicdomain/zero/1.0/>

## Related

[Architecture](architecture.md) · [Compatibility](compatibility.md) ·
[Testing](testing.md) · [Build guide](../GUIDE.md)
