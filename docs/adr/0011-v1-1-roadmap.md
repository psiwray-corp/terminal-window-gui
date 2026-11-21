# ADR-0011: v1.1 roadmap and `terminal-window` gaps

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

Several GUI features cannot be implemented correctly on `terminal-window`
2.0.0. They need per-cell attributes, width-aware cells, preedit events, OS
clipboard access, runtime font changes, image support, bidi, accessibility, or
pixel-precise mouse positions. Others are GUI-only and can be built without a
host change.

A roadmap that says "later" without naming the host gap invites stubs and
rework. Every candidate must be labeled `v1.1`, `blocked`, or `not planned`,
and every blocked item must name the exact missing host capability.

## Decision

We will track v1.1 candidates in `docs/roadmap.md` with this status scheme:

- `v1.1`: buildable in the GUI once scheduled.
- `blocked`: requires a named `terminal-window` change first.
- `not planned`: explicitly outside the 1.x horizon.

The blocked set is:

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

The GUI-only v1.1 set is:

`DesktopWindow`, slider, command palette, date/color/file pickers,
charts/sparklines, variable-height list rows, lazy tree children, content-width
table columns, in-cell table editing, drag-and-drop reorder,
animations/transitions, menu/toolbar overflow, smooth/kinetic scrolling, rich
tooltips/toasts, multiple GUI windows with independent surfaces, form
validation engine, hyperlinks/click regions, persistent log history.

No item is a stub in v1. The v1.1 roadmap is evidence that the cut line is
deliberate.

## Consequences

### Positive

- Host work and GUI work can be scheduled independently.
- Blocked items cannot be "fixed" by faking attributes or widths in the GUI.
- GUI-only items can ship without waiting for a host release.
- Consumers see exactly why a missing feature is missing.

### Negative

- Some features will wait for an upstream release cadence.
- The roadmap must be updated when the host gains capabilities.
- "Not planned" items must be stated explicitly rather than omitted.

### Neutral

- `docs/roadmap.md` is normative; this ADR records the decision and the gap
  table.
- A future host change requires a new ADR and a rewrite of the affected GUI
  contract.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Promise features in v1 | They cannot be rendered correctly without host changes; stubs are forbidden. |
| Hide gaps behind "future work" | Consumers cannot plan and implementers cannot scope. |
| Implement a second renderer | Duplicates the host and breaks the composition contract. |
| Drop blocked features permanently | They are valid GUI needs; the host gap is the blocker, not the feature. |

## References

- `terminal-window` ADR-0005 instance layout (reserved `u16` at offset 6).
- `terminal-window` `docs/input.md` event fields.
- `terminal-window` `docs/font-format.md` fixed font at window creation.
- `docs/roadmap.md` normative table.
