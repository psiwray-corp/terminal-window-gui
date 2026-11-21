# 01 — Charter, scope, and v1.1 roadmap

**Phase:** decisions · **Depends on:** — · **Blocks:** all

## Goal

Lock identity, scope, architecture decisions, testing policy, and the v1.1
roadmap. Documentation only: no Gradle files, no Kotlin, no assets.

## Context budget

Read: `GUIDE.md`, this file, `terminal-window` `docs/adr/0000-template.md`,
`docs/architecture.md`, `docs/public-api.md`, `docs/frame.md`, `docs/input.md`,
`docs/font-format.md`, and `docs/guide/01-project-charter.md`. Do not read
sibling sections. Expected output: ADRs, architecture, compatibility, roadmap,
testing policy, README/CONTRIBUTING/CHANGELOG/AGENTS, and the guide progress
update.

## Deliverables

- `docs/adr/0000-template.md` (copy from `terminal-window`, adjust names).
- `docs/adr/0001-artifact-identity.md` through
  `docs/adr/0012-release-and-publishing.md`.
- `docs/architecture.md`.
- `docs/compatibility.md`.
- `docs/roadmap.md`.
- `docs/testing.md`.
- `AGENTS.md`.
- `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE`.
- `GUIDE.md` progress update only; the section list is already fixed.

## Steps

1. **Identity.** Fix group `com.psiwray.organization`, artifact
   `terminal-window-gui`, version `1.0.0` (development `1.0.0-SNAPSHOT`),
   package root `com.psiwray.organization.terminal_window_gui`, automatic
   module name `com.psiwray.organization.terminal_window_gui`, repository
   `psiwray-corp/terminal-window-gui`, and modules `:library`, `:examples`.
2. **License.** MIT for code; the bundled font is CC0-1.0 with attribution in
   `docs/shapes.md` and the font header. Do not reuse GPL v1 material.
3. **Runtime.** JDK 25, Kotlin 2.4.21, Gradle 9.9.1, JUnit 6.1.3, Dokka 2.3.0,
   ktlint 1.8.0, Spotless 8.11.0, nmcp 1.6.2. Host library
   `com.psiwray.organization:terminal-window:2.0.0`. No `terminal-window`
   changes in v1.
4. **Modules.** `:library` is the only published artifact; `:examples` proves
   the API. There is no `:benchmarks` module.
5. **Ownership.** Record the passive-surface rule: the caller owns the window,
   service, owner thread, event delivery, paint scheduling, timers, close, and
   GL. `GuiSurface` exposes `resize`, `dispatch`, `tick`, and `paint`; it never
   calls `TerminalWindow`/`TerminalWindowService`.
6. **Coordinates.** Record local widget bounds relative to the parent content
   box and a `PaintContext` origin/clip stack. Screen coordinates are used only
   at the overlay/pointer boundary.
7. **Sizing.** Record one `measure(constraints)` path. Preferred is computed;
   minimums are content clamps and explicit `ConstrainedBox` wrappers;
   maximums are parent constraints or wrappers. No mutable Swing-style sizes.
8. **Shapes and font.** Record semantic `Shape` roles, per-shape primary and
   fallback, a global last resort, the bundled CC0 `.twf` font, and the ASCII
   shape set. No Private Use Area code points.
9. **v1 scope.** Include the component catalog: Panel, Label/RichLabel,
   Separator, Spacer, Badge, Icon, Button, Checkbox, RadioGroup, Switch,
   TextField, SearchField, TextArea, LogView, Select, ListView, TableView,
   TreeView, Tabs, SplitPane, Form/Field, AppShell, StatusBar, ScreenHost,
   MenuBar/Menu/ContextMenu, Dialog/Confirm/Prompt, Popover, Tooltip, Toast,
   ScrollView/ScrollBar, ProgressBar, Spinner, Banner, Toolbar.
10. **v1 limits.** Single-cell code points; no bold/italic/underline, wide
    characters, grapheme clusters, IME, images, OS clipboard, arbitrary
    component drag/resize, animations, charts, or pickers. These are v1.1.
11. **Testing policy.** Record the `GuiTestHarness`, golden cell grids,
    synthetic events, fake clock, component conformance matrix, and the rule
    that GL appears only in integration tests under Xvfb. No timing tests.
12. **Roadmap.** Write the full v1.1 table below into `docs/roadmap.md`,
    including the exact `terminal-window` gap for every blocked item.
13. **Architecture document.** Include package map, object map, thread map,
    paint cycle, coordinate rules, size model, shape resolution, and the
    caller-owned integration sequence.
14. **Compatibility policy.** SemVer 2.0.0. Public Kotlin declarations, JVM
    descriptors, module name, coordinate system, sizing contract, event
    ordering, and shape resolution are covered. Rendered pixels, timing,
    exception text, and platform font coverage are excluded.
15. Write the docs from these decisions. Do not add code or build files.

## ADR set

| ADR | Decision |
|---|---|
| 0001 | Artifact identity and package root |
| 0002 | MIT license; CC0 font |
| 0003 | JDK 25, Kotlin 2.4.21, Gradle 9.9.1 |
| 0004 | Modules `:library`, `:examples`; no benchmarks |
| 0005 | Passive `GuiSurface`; caller owns window and loop |
| 0006 | Local widget coordinates and `PaintContext` transform |
| 0007 | One measure path; computed preferred/min/max |
| 0008 | Semantic shape set and bundled font |
| 0009 | v1 component scope and explicit cut line |
| 0010 | Testing strategy and conformance matrix |
| 0011 | v1.1 roadmap and `terminal-window` gaps |
| 0012 | Release branches, signing, Maven Central |

## v1.1 roadmap

### Blocked on `terminal-window`

| Feature | Required in `terminal-window` | Scale |
|---|---|---|
| Bold, italic, underline, strikethrough, dim | Per-cell attribute model, public `TerminalMatrix.set` overload, shader/atlas support. The 16-byte instance has a reserved `u16` at offset 6, so flags can fit without a stride change; underline/strikethrough can be fragment-shader work, bold/italic need atlas variants or synthetic transforms. | Minor/major |
| CJK double-width, combining marks, grapheme clusters | Width-aware cell spans, grapheme storage, renderer clipping/skipping, caret and clipboard index mapping. | Major |
| IME preedit/composition | Preedit and commit events, text-input rectangle for the candidate window, platform/GLFW spike. `CharEvent` alone is not enough. | Major |
| OS clipboard | `TerminalWindow` get/set clipboard strings mapped to GLFW clipboard calls. | Minor |
| Runtime font switching and fallback chain | Font/atlas lifecycle: `setFont` or controlled reopen, owner-thread atlas rebuild, per-glyph fallback chain. The current font is fixed at window creation. | Minor/major |
| Images/pixels | Image/texture layer or sixel/kitty protocol support. | Major |
| RTL/bidi | Bidi reordering and width in the matrix; layout mirroring follows. | Major |
| Accessibility bridge | Accessible tree and platform integration for an OpenGL window. | Major |
| Sub-cell click precision | Pixel coordinates on `MouseButtonEvent`; `MouseMoveEvent` already has pixels. | Minor |
| Markdown/ANSI rich text, syntax highlighting | Attribute model for full fidelity; color-only spans are GUI-only. | GUI + attributes |
| Scrollback/history | No host gap; GUI-side log model and virtualization. | GUI-only |

### GUI-only

`DesktopWindow` (movable/resizable/z-ordered internal windows), slider, command
palette, date/color/file pickers, charts/sparklines, variable-height list rows,
lazy tree children, content-width table columns, in-cell table editing,
drag-and-drop reorder, animations/transitions, menu/toolbar overflow,
smooth/kinetic scrolling, rich tooltips/toasts, multiple GUI windows with
independent surfaces, form validation engine, hyperlinks/click regions,
persistent log history.

Every item is a v1.1 candidate, not a stub. The roadmap labels each item
`v1.1`, `blocked`, or `not planned`.

## Acceptance

- [x] Every ADR is `Accepted`, has no `TBD`, and cites its primary source.
- [x] ADR-0005 states that the library never owns a window, loop, thread, or
      timer.
- [x] ADR-0006 states local bounds and the `PaintContext` origin/clip rule.
- [x] ADR-0007 states one measure path and rejects mutable Swing-style sizes.
- [x] ADR-0008 lists the shape roles, fallback order, bundled font, and ASCII
      set.
- [x] `docs/roadmap.md` contains the full v1.1 table with exact
      `terminal-window` gaps.
- [x] `docs/architecture.md` has package, object, thread, and paint-cycle maps.
- [x] `docs/testing.md` contains the conformance matrix and the no-timing rule.
- [x] `docs/compatibility.md` states the 1.0 policy and v1 gaps.
- [x] `README.md` is at most 150 lines and links only to existing files.
- [x] `AGENTS.md` keeps the no-commit rule and context budget.
- [x] No Gradle, Kotlin, or asset files were created in this section.

## Pitfalls

- Do not promise window ownership, an application loop, background repaint, or
  threads. The library is passive.
- Do not call v1.1 items “MVP” or “follow-up” in a way that implies stubs.
- Do not claim text attributes, wide characters, IME, images, or OS clipboard.
- Do not use Private Use Area code points or invent Unicode shapes.
- Do not re-derive `terminal-window` bootstrap/release facts; reference them.
- Do not add `:benchmarks` or a performance table.
- Do not make the README a second guide; keep it a fresh-visitor front page.

## Plan seed

> Plan section 01 only. Write the ADRs, architecture, compatibility, roadmap,
> testing policy, AGENTS, README, CONTRIBUTING, CHANGELOG, and LICENSE for a
> passive Kotlin 2.4.21 / JDK 25 GUI library built on `terminal-window 2.0.0`,
> with local coordinates, one measure path, semantic shapes, a bundled CC0
> font, 15 build sections, no benchmarks, and a v1.1 roadmap that records every
> missing `terminal-window` capability. No code.
