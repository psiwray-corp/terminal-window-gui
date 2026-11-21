# ADR-0002: MIT license and CC0 bundled font

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

The GUI library is intended to be embedded in applications, including
proprietary applications. The host `terminal-window` 2.0.0 is MIT-licensed and
its public types are used at the GUI boundary. The project owns the new GUI
code and can choose a permissive license.

The library also needs a bundled bitmap font so that a fresh consumer gets
deterministic shape coverage without installing a system font. The chosen
source glyphs are public-domain VGA-derived `font8x8_basic` glyphs by Daniel
Hepper, based on Marcel Sondaar / IBM public-domain VGA glyphs. The font file
is dedicated to CC0-1.0.

Published versions are immutable. A license decision must be made once for
`1.0.0` and must not depend on copying GPL-licensed terminal-window v1 code.

## Decision

We will license all original code, build scripts, and documentation in this
repository under the **MIT License**.

The bundled font `terminal-window-gui-8x16.twf` is dedicated to **CC0-1.0**.
Its attribution is repeated in the `.twf` header and in `docs/shapes.md`:

> Glyph data derived from `font8x8_basic` by Daniel Hepper, based on Marcel
> Sondaar / IBM public-domain VGA glyphs. Dedicated to CC0-1.0.

Rules:

1. No GPL-licensed `terminal-window` v1 source is copied into this repository.
2. New source is authored for this project or adapted only from permissively
   licensed material with attribution.
3. Inbound contributions are accepted under MIT (inbound = outbound). No CLA is
   required.
4. The repository ships `LICENSE` with the MIT text and the copyright notice.
5. The font's CC0 dedication is recorded with the font and in the shape
   documentation; CC0 requires no `NOTICE` file.
6. Dependency licenses are audited when dependencies are added. The host
   `terminal-window` 2.0.0 is MIT.
7. Copyright notices are preserved in copies and substantial portions of the
   software.

## Consequences

### Positive

- Proprietary and open-source applications can embed the library without
  copyleft obligations.
- MIT is compatible with the host's MIT license and the permissive dependency
  baseline.
- CC0 removes font licensing friction for redistributors.
- The artifact has no `NOTICE`-file obligation.

### Negative

- MIT grants no express patent license and no trademark rights.
- The font must keep its CC0 attribution; removing it would make provenance
  unauditable.
- GPL-only consumers cannot relicense the combined work as MIT; they may still
  combine MIT code into a GPL work, which then carries GPL terms.
- GPL-licensed host v1 code cannot be reused in this repository.

### Neutral

- The license does not affect API compatibility or runtime behavior.
- `docs/shapes.md` is delivered in section 03; this ADR fixes the attribution
  text now so the font and documentation cannot drift.

## Alternatives

| Alternative | Why rejected |
|---|---|
| GPL-3.0 | Copyleft is an adoption barrier for a library meant to be embedded. |
| Apache-2.0 | Adds patent and `NOTICE` obligations that this project does not need. |
| LGPL-3.0 | Still imposes relinking and reverse-engineering rights on combined works. |
| Proprietary license | Blocks open-source use and community contributions. |
| No bundled font | Makes shape coverage depend on the consumer's fonts and breaks deterministic goldens. |
| Use a system font | Platform coverage and licensing vary; the library cannot guarantee shapes. |

## References

- MIT License: <https://opensource.org/license/mit>.
- CC0 1.0 Universal: <https://creativecommons.org/publicdomain/zero/1.0/>.
- `font8x8_basic` by Daniel Hepper: <https://github.com/dhepper/font8x8>.
- `terminal-window` 2.0.0 license: MIT.
