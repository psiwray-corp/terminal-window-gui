# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-21

First release of `terminal-window-gui`.

### Added

- Section 01 decisions: artifact identity, MIT/CC0 licensing, JDK 25/Kotlin
  2.4.21 toolchain, `:library`/`:examples` modules, passive `GuiSurface`
  ownership, local coordinates, one measure path, semantic shapes with a
  bundled CC0 font, v1 component scope, testing policy, v1.1 roadmap, and the
  local Maven Central release model.
- Architecture, compatibility, testing, and roadmap documents.
- Repository README, contribution guide, agent instructions, and license.
- Section 02 scaffolding: Gradle 9.9.1 wrapper with pinned checksum, Kotlin
  2.4.21/JDK 25 multi-project build (`:library`, `:examples`), version catalog,
  Spotless/Dokka quality gates, local publication, shared `ci/*.sh` scripts,
  GitHub/Forgejo workflows, and release scripts.
- Repository hygiene files (`.editorconfig`, `.gitattributes`, `.gitignore`).
- Section 03 foundations: semantic `Shape`/`ShapeSet`/`ResolvedShapeSet` model,
  Unicode and ASCII catalogs, bundled CC0 8x16 `.twf` font, `GuiFonts`, clipped
  `CellCanvas`, local-coordinate `PaintContext`, deterministic `TextMetrics`,
  and shape/text/canvas golden tests.
- Section 04 layout: normalized `Constraints`, local-coordinate `Widget` tree,
  measure caching, clipping containers, Row/Column flex, Stack/Positioned,
  Grid, Wrap, Padding/Align/Center/SizedBox/ConstrainedBox, and 1x1/80x24/240x67
  layout goldens.
- Section 05 interaction runtime: widget focus/visibility/enabled/event hooks,
  local-coordinate capture/target/bubble routing, pointer capture, hover paths,
  double/triple click detection, focus traversal/scopes/traps, overlay stacking
  with flip/clamp/dismiss/modal focus restoration, theme tokens and state
  styles, commands and scoped keymaps, text clipboard interface, and a
  caller-driven scheduler. Added root `GridSize` for resize events.
- Section 06 surface runtime: passive `GuiSurface` with lazy layout, no-paint
  dispatch, caller-driven timer ticks, full-surface paint with failure-retry
  flags, advisory pointer shape, pure `GuiInput` host translation, `GuiException`,
  a headless `GuiTestHarness` with structural/style dumps and a fake clock,
  golden surface fixtures, and one Xvfb GL frame test. Host LWJGL natives are
  build-only and never published.
- Section 07 scrolling and display primitives: clamped `ScrollModel`,
  scroll-aware `ScrollView`, `ScrollBar` with thumb/track/arrow interaction,
  `Panel` with single/double/rounded semantic borders, `Label`/`RichLabel` text
  layout with ellipsis and color spans, `Separator`, `Badge`, `Icon`,
  `ProgressBar`, `Spinner`, and `Banner`. Added theme-aware `PaintContext`,
  attach-time service context, signed screen coordinates for scrolled hit
  testing, caller-driven animation timers, and widget golden fixtures.
- Section 08 buttons and toggles: shared press/release activation, cached
  one-line label layout, `Button` with icons, default/focus markers, ellipsis,
  and `Alt+<mnemonic>` dispatch, three-state `Checkbox`, roving-focus
  `RadioGroup`/`RadioButton`, `Switch`, and a one-row `Toolbar`.
- Section 09 text editing: code-point-safe `TextEditingController` with
  directional selection, bounded coalescing undo/redo, ASCII word navigation,
  and clipboard operations; `TextField` with masking, read-only selection,
  horizontal caret scrolling, and caret/selection painting; `SearchField` with
  a Tab-focusable clear button; wrapped `TextArea` with line numbers, visual
  caret mapping, mouse selection, and scrolling; and read-only `LogView` with
  follow-tail and scroll-lock indication. Added text editing goldens and fuzz
  tests.
- Section 10 collections: stable-key `SelectionModel` with single, multiple,
  and none modes; virtualized `ListView` with row caching, type-ahead, keyboard
  and modifier mouse selection, activation, and an empty label; `TableView`
  with fixed/flex/fraction columns, a frozen header, sort display state,
  horizontal scrolling, ellipsized cells, and application-owned header clicks;
  and eager virtualized `TreeView` with cached flattening, expansion, guide
  shapes, parent/child navigation, and stable-key selection. Added collection
  goldens and virtualization tests.
- Section 11 composite containers and navigation: scrolling `TabBar`/`TabView`
  with active-tab visibility, keyboard/mouse switching, inactive-content
  detachment, and per-tab focus restore; clamped `SplitPane` with drag,
  keyboard, double-click reset, and hidden-divider fallback; `Form`/`FormField`
  with fixed/fraction/content label widths, help, error markers, and
  control-preserving truncation; muted `StatusBar`/`KeyHint` with separated
  hints and non-overlapping truncation; `AppShell` menu/content/status rows;
  and `Screen`/`ScreenHost` with stack navigation, back handling, resize
  callbacks, per-screen keymaps, and focus restoration. Added composite
  goldens and 1x1/80x24/240x67 conformance checks.
- Section 12 menus, dialogs, and overlays: one-row `MenuBar` with F10,
  mnemonics, mouse switching, focus restore, and non-color active markers;
  flipping/clamping `MenuPopup` and `ContextMenu` with submenus, checked/radio
  markers, right-aligned shortcuts, disabled skipping, and tall-list scrolling;
  modal `Dialog` with focus trap, body scrolling, visible actions, Esc/Enter
  mapping, one-shot results, `ConfirmDialog`, and display-only `PromptDialog`
  validation; `Popover` placement/dismiss policies; scheduler-driven
  `TooltipController`; and `ToastManager` stacking, auto-dismiss, and manual
  close. Added overlay goldens and 1x1/80x24 conformance checks.
- Section 13 examples: caller-owned `ExampleLoop`, gallery, TODO, responsive
  dashboard, and `Sparkline` custom widget with a golden; `:examples` run,
  smoke, and screenshot tasks; committed screenshots; and the Xvfb smoke gate.
- `GuiSurface.setShapes` for runtime Unicode/ASCII shape switching.

### Fixed

- Nested `ContainerWidget` content origins were applied twice during paint and
  hit testing; children now paint and hit at the parent offset.
- Scrolled containers now hit-test the visible viewport before applying the
  scroll offset. Mouse events reach controls below the initial viewport instead
  of being captured by the `ScrollView` itself.
- `TextField` reserves a caret cell in its preferred width, so an initially
  focused prompt shows its complete value instead of dropping the first glyph.
- High-contrast `surfaceAlt` is now dark; info banners, table headers, focused
  fields, tooltips, and toasts no longer paint white text on white.
- Spinner frames use a coherent four-step `| / - \` rotation instead of a
  partial Braille sequence that did not read as a full cycle.
- The gallery dialog has a genuinely scrollable body, the standalone
  `ScrollBar` now controls a visible `ScrollView`, and the prompt uses
  `Terminal` as its initial value.
- The example host loop no longer installs a 20-second watchdog; interactive
  examples stay open until the window is closed. Smoke mode still exits after
  ten frames.
- Pointer routing now keeps signed screen coordinates, so dragging past a
  window edge clamps instead of constructing an invalid `IntSize`.
- Menu popup width accounts for the label's trailing padding and submenu
  marker, so commands such as `Keyboard help` are not ellipsized.
- Labels and rich labels preserve their destination background unless an
  explicit background is set, keeping selected list-row backgrounds intact.
- `SplitPane` paints its surface background before the divider and children,
  preventing stale dark divider cells while dragging.
- Scroll events are handled by the deepest target and bubble outward; an
  ancestor `ScrollView` no longer consumes them during an implicit capture
  phase. Nested views chain to the parent only when the child cannot scroll.
- Flex table columns use their header title as the minimum width, and an empty
  table keeps its empty row visible. Narrow tables now show the horizontal
  scrollbar instead of silently ellipsizing headers.
- `TableView` no longer paints the focus cursor over the final cell of a row;
  cell content and scrollbar cells remain intact.
- `AppShell` paints its full surface background, so root gaps and panel interiors
  no longer expose the darker canvas background.
- Nested `Panel` content now fills in local coordinates instead of absolute
  panel coordinates, keeping its bottom border visible and preventing it from
  painting over the following panel.
- The font's `U+2510` and `U+2518` box-corner bitmaps no longer contain the
  stray fifth horizontal pixel to the right of the vertical stem.
- Focus changes now invalidate paint for every widget, so focused `Panel`
  borders and other focus visuals update immediately.
- Tab traversal honors `Widget.tabStop`. `MenuBar`, structural `ScrollView`s,
  and banners without an actionable control are skipped while remaining
  focusable through `FocusManager` or pointer input. Dialog body scroll views
  opt back into Tab order.
- Focus changes now reveal the focused widget through every ancestor
  `ScrollView`, scrolling nested viewports only as far as needed.

### Limitations

- No text attributes beyond color, wide/CJK cells, combining marks, grapheme
  clusters, or IME.
- No OS clipboard, runtime font switching, images, sixel/kitty graphics, RTL,
  or accessibility bridge.
- No `DesktopWindow`, slider, pickers, charts, animations, variable-height rows,
  lazy tree children, content-width table columns, or in-cell table editing.
- Rendered pixels, timing, platform font coverage, and exception text are not
  compatibility promises.

See [compatibility](docs/compatibility.md) and the
[release runbook](docs/release/runbook.md) for the published artifact and its
limits.
