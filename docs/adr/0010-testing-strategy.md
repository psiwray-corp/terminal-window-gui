# ADR-0010: Testing strategy and conformance matrix

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

A terminal GUI's visible output is a grid of code points and colors. A test
that only asserts "paint did not throw" cannot catch a one-cell clip error, a
lost selection, an off-by-one caret, or a missing shape fallback. A screenshot
comparison of a GL window is slow, platform-dependent, and tests the host
renderer rather than the GUI.

The host `terminal-window` already has GL integration coverage. The GUI needs
fast deterministic component tests plus a thin real-window smoke test.

## Decision

We will test the GUI at four layers:

1. **Pure unit tests** for geometry, constraints, shape resolution, text
   metrics, editing state, and models.
2. **Golden component tests** through `GuiTestHarness`, which renders into a
   `TerminalMatrix` with no GL and compares structural rows and theme-token
   style dumps.
3. **Property/fuzz tests** for random layout trees, random edit sequences, and
   random scroll offsets, using fixed seeds.
4. **GL integration tests** under Xvfb that open a real `TerminalWindow`, paint
   cells, render a frame, and close; they assert `drawCalls > 0` and no
   screenshot diff.

`GuiTestHarness` exposes `resize`, `paint`, `rows`, `styles`, synthetic
`key`, `char`, `click`, `drag`, `scroll`, `tick`, `advance`, `focusPath`, and
paint/layout counters. It uses a fake clock starting at zero.

Golden files store structural rows; style dumps use theme token names, not raw
ARGB. `-PupdateGoldens` rewrites goldens locally; CI never updates them.

The conformance matrix is mandatory for every widget section:

| Category | Required cases |
|---|---|
| States | normal, focused, hovered, pressed, disabled, selected, checked/indeterminate, error, empty, truncated, minimum size, 1×1 |
| Shape sets | bundled Unicode, ASCII fallback, missing primary → per-shape fallback, missing fallback → last resort, strict failure |
| Themes | dark, light, high-contrast/monochrome |
| Input | tab order, arrow navigation, mnemonics, Enter/Space, click, drag, double-click, wheel, focus trap/restore, event consumption |
| Layout | child bounds inside parent content, Row/Column non-overlap, flex distribution, idempotent layout, clipping, resize |
| Data | zero, one, many items; stable-key selection preservation; scroll clamp |
| Text | caret at 0/mid/end, forward/backward selection, undo/redo, cut/copy/paste, password mask, word navigation, wrap, surrogate pairs |
| Overlays | anchor flip/clamp, Esc/click-outside dismiss, z-order, modal focus trap |
| Property/fuzz | random layout trees keep bounds invariants; random edit sequences match a reference string model; random scroll offsets clamp |
| Runtime | real window under Xvfb: paint, frame, close; assert `drawCalls > 0`; no screenshot diffing |

A conformance registry in section 14 asserts every public widget has at least
one case. GL tests are tagged `gl` and are excluded from the default no-display
suite; they run in `integrationTest` under Xvfb.

No timing assertions and no benchmark module are permitted. The tests must not
depend on wall-clock time, GPU speed, font installation, or window-manager
behavior.

## Consequences

### Positive

- Component failures point to a row, cell, style token, or event sequence.
- Tests run headless and deterministically on every OS.
- The GL smoke test verifies integration without making screenshots a gate.
- The conformance matrix prevents a widget from shipping with only a happy
  path.

### Negative

- Golden files need curation and can be noisy during intentional visual
  changes.
- The matrix is large; every widget section must supply all applicable cases.
- Fuzz tests require fixed seeds and reference models.
- The GL smoke test still needs Xvfb and Mesa on Linux CI.

### Neutral

- `-PupdateGoldens` is a local convenience, never a CI step.
- A pure unit test may cover a model without painting; a golden test must
  paint.
- The integration test asserts host frame counters, not GUI timing.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Screenshot diff of the GL window | Platform-dependent, slow, and tests the host renderer. |
| Only "does not throw" tests | Misses clipping, layout, selection, and caret bugs. |
| Timing-based performance tests | Hardware-dependent and excluded by policy. |
| Manual visual inspection only | Not repeatable and not part of CI. |
| No GL smoke test | Would not prove that the matrix reaches a frame. |

## References

- `GUIDE.md` testing standard and conformance matrix.
- `terminal-window` `docs/frame.md` `drawCalls` semantics.
- `docs/testing.md` normative test policy.
