# Testing

The GUI is tested as a deterministic cell-grid renderer, not as a screenshot.
`GuiTestHarness` paints into a `TerminalMatrix` with no GL and compares
structural rows and theme-token style dumps. GL appears only in a thin
integration test under Xvfb.

## Test layers

| Layer | Surface | Purpose |
|---|---|---|
| Unit | Pure Kotlin values, layout, shapes, text metrics, edit models | Fast invariants and edge cases |
| Component golden | `GuiTestHarness` + `TerminalMatrix` | Exact cells, clipping, focus, selection, overlays |
| Property/fuzz | Generated trees/edits/scrolling with fixed seeds | Invariants beyond hand-written cases |
| GL integration | Real `TerminalWindow` under Xvfb | Paint reaches a frame; close works |
| API/docs | Compiled test classpath, JAR manifest, and Markdown | Public inventory, module name, links, README budget |

## Harness contract

`GuiTestHarness(root, theme, shapes, columns = 80, rows = 24)` exposes:

- `resize(columns, rows)`, `paint()`, `rows()`, `styles()`;
- synthetic `key`, `char`, `click`, `drag`, `scroll`;
- `tick`, `advance(millis)`, `focusPath`, `paintCount`, `layoutCount`;
- a fake clock starting at zero.

`rows()` returns the cell grid as strings. `styles()` returns theme token names,
not raw ARGB. A component test must call `paint`; a test that only exercises a
model is a unit test, not a golden test.

## Conformance matrix

Every widget section must cover the applicable cases:

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

`WidgetConformanceTest` has one registry entry per public concrete widget. The
case paints the widget, exercises pointer/key/scroll delivery through
`GuiTestHarness`, and the completeness check compares the registry with the
public concrete `Widget` subclasses on the compiled classpath. Missing or stale
entries are failures, not warnings.

## Golden rules

- Golden files contain structural rows and theme token names.
- `-PupdateGoldens` rewrites goldens locally. CI never updates them.
- A golden change must be explained by a user-visible behavior change.
- Golden files are UTF-8, LF, and stored under `library/src/test/resources/golden/`.
- Tests must not depend on wall-clock time, GPU speed, locale, platform font,
  or window-manager behavior.
- `GuiTestHarness` uses a fake clock; `tick` advances only when `advance` is
  called.
- Property/fuzz tests use fixed seeds and print the seed on failure.

## GL integration

GL tests are tagged `gl` and excluded from the default no-display suite. They
run under Xvfb with Mesa:

```text
xvfb-run -a -s "-screen 0 1280x1024x24" ./gradlew integrationTest
```

An integration test:

1. opens a host `TerminalWindow` and acquires its owner thread;
2. constructs one `GuiSurface` and paints a representative tree;
3. runs at least one host frame;
4. asserts `drawCalls > 0` and that close completes;
5. performs no screenshot comparison.

The integration suite may be skipped on a host without a display; it must not
be replaced by a screenshot gate.

The examples have a separate GL smoke gate. Each screen accepts `--smoke`,
paints ten frame opportunities, asserts at least one draw call, and closes
under a 20 s watchdog:

```text
xvfb-run -a -s "-screen 0 1280x1024x24" ./gradlew :examples:smoke
```

`ci/integration-test.sh` runs the library integration suite first and then the
example smoke tasks. Screenshots under `docs/assets/` are visual artifacts,
not test assertions.

## No timing rule

There are no timing assertions, no performance budgets, and no benchmark
module in this repository. A test must not sleep to wait for a repaint, measure
frame duration, assert frames per second, or compare hardware throughput.
Cursor blink, spinner, toast, and tooltip behavior are tested by advancing the
fake clock, not by sleeping.

## API and documentation checks

- `ApiSurfaceTest` checks the exact public type inventory, explicit visibility
  gate, sealed `GuiEvent` hierarchy, inline-function ban, and absence of
  `internal`/`org.lwjgl.*` types in public signatures.
- `ReadmeQuickstartTest` holds the README caller-loop snippet as compile-only
  test source so the quickstart cannot refer to missing APIs.
- `WidgetConformanceTest` checks that every public widget has a matrix case.
- `ci/check-module.sh` runs `jar --describe-module` and checks that no
  `module-info.class` is present.
- `ci/check-docs.sh` checks internal Markdown links and the README line budget.
- `dokkaGenerate` fails on undocumented public declarations.

## Related

[Architecture](architecture.md) · [Compatibility](compatibility.md) ·
[ADR-0010](adr/0010-testing-strategy.md) · [Build guide](../GUIDE.md)
