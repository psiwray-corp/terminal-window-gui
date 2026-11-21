# ADR-0008: Semantic shape set and bundled CC0 font

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

A terminal GUI draws boxes, arrows, checks, scrollbars, spinners, and tree
markers. If widgets hardcode Unicode characters, a font with partial coverage
turns a working UI into missing-glyph boxes, and a caller cannot substitute an
ASCII-only font. The host `BitmapFont` resolves one code point at a time and
has no notion of semantic shape roles.

The host's font is fixed at window creation and the terminal matrix supports
one code point per cell. The GUI therefore needs a shape catalog, a fallback
order, and one bundled font that covers the catalog.

## Decision

We will use semantic `Shape` roles, a `ShapeSet` with a per-shape primary and
fallback, a global last resort, and a bundled CC0 `.twf` font.

### Roles

The public `Shape` enum contains these roles:

- Box drawing: `H`, `V`, `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`,
  `BOTTOM_RIGHT`, `TEE_DOWN`, `TEE_UP`, `TEE_LEFT`, `TEE_RIGHT`, `CROSS` in
  `SINGLE`, `DOUBLE`, `ROUNDED`, and reserved `HEAVY` styles.
- Progress: empty, 1/8, 1/4, 3/8, 1/2, 5/8, 3/4, 7/8, and full.
- Scrollbar: vertical/horizontal tracks, vertical/horizontal thumbs, up, down,
  left, and right.
- Indicators: check, cross, close, dot, and ellipsis.
- Tree: branch, last, vertical, expanded, and collapsed.
- Arrows and sort: up, down, left, right, ascending, and descending.
- Brackets and parens: left/right for each.
- Spinner: frames 0, 1, 2, and 3.

Rounded box styles reuse the single-line edges, tees, and cross; only corners
differ. Heavy box roles are present in the enum and the bundled font but are
reserved for v1.1 widget use.

### Code points and fallback order

The normative code-point table is `docs/shapes.md`, delivered in section 03.
The policy is fixed here:

1. Primary: the role's Unicode code point if `font.hasGlyph` is true.
2. Per-shape fallback: the role's declared fallback code point if present.
3. Last resort: the `ShapeSet.lastResortCodePoint`, default U+003F (`?`), if
   the font has it.
4. Otherwise throw `MissingShapeException`.

`MissingShapePolicy.FAIL` throws when any primary is missing, even if a
fallback exists. The exception lists every missing primary. Resolution is
validated once at surface construction and again if the caller supplies a new
resolved set.

The Unicode set uses the standard Unicode shape code points, not Private Use
Area code points. It carries ASCII fallbacks so a partial font degrades per
glyph. The ASCII set maps every role to its ASCII fallback and targets a
minimal 95-glyph font.

| Group | Primary source | ASCII fallback examples |
|---|---|---|
| Box drawing | U+2500..U+257F | `-`, vertical bar, `+` |
| Progress | U+2588..U+258F | `#` |
| Scrollbar | tracks U+2500/U+2502, thumbs U+2588, arrows U+25B2/U+25BC/U+25C0/U+25B6 | `-`, vertical bar, `#`, `^`, `v`, `<`, `>` |
| Indicators | U+2713, U+2717, U+2715, U+2022, U+2026 | `x`, `X`, `x`, `*`, `.` |
| Tree | U+251C, U+2514, U+2502, U+25BE, U+25B8 | `+`, `+`, vertical bar, `v`, `>` |
| Arrows/sort | U+2190..U+2193 | `^`, `v`, `<`, `>` |
| Brackets/parens | ASCII `[` `]` `(` `)` | same |
| Spinner | U+280B, U+2819, U+2839, U+2838 | vertical bar, `/`, `-`, backslash |

### Bundled font

`terminal-window-gui-8x16.twf` is an 8×16 `.twf` v1 font with:

- ASCII `U+0020..U+007E` and fallback `U+003F`;
- every v1 primary shape code point;
- the reserved heavy box set;
- every ASCII fallback glyph.

`GuiFonts.bundled()` loads it from the library resources with
`BitmapFont.parse(InputStream)`. The font is CC0-1.0 with the attribution in
ADR-0002, the `.twf` header, and `docs/shapes.md`.

## Consequences

### Positive

- Widgets request semantic shapes, not glyph code points.
- A font missing one shape degrades to that shape's fallback instead of
  breaking the whole UI.
- ASCII-only fonts and monochrome/high-contrast themes are deterministic.
- Golden tests can enumerate every role and both shape sets.

### Negative

- The bundled font is larger than ASCII alone.
- A per-shape fallback is not always visually equivalent; a box corner may
  become `+`.
- Strict resolution can reject a font that would render most of the UI.
- Heavy styles are present in the enum and font before widgets use them.

### Neutral

- `docs/shapes.md` owns the complete code-point table; this ADR owns the policy.
- A future font switch still requires a host capability; the GUI can resolve
  a different `BitmapFont` only when the caller can supply one.
- No Private Use Area code points and no emoji presentation are permitted.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Hardcode Unicode in widgets | Partial fonts fail and widget code duplicates glyph knowledge. |
| Use Private Use Area code points | Requires a custom font and breaks with standard fonts. |
| Use Nerd Font glyphs | Extra glyph set, license surface, and width assumptions. |
| No bundled font | Golden tests and shape coverage depend on the consumer's font. |
| Single fallback only | One missing shape degrades the whole UI; per-shape fallbacks are precise. |
| Fail on any missing glyph | Makes partial fonts unusable and prevents graceful ASCII fallback. |

## References

- Unicode Box Drawing block U+2500..U+257F.
- Unicode Block Elements U+2580..U+259F.
- `terminal-window` `docs/font-format.md` `.twf` v1.
- `docs/guide/03-fonts-shapes-canvas-text.md` full code-point table.
- `font8x8_basic` by Daniel Hepper: <https://github.com/dhepper/font8x8>.
- CC0 1.0: <https://creativecommons.org/publicdomain/zero/1.0/>.
