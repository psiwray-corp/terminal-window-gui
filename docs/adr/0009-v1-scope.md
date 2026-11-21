# ADR-0009: v1 component scope and explicit cut line

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

The GUI library can grow without bound if every terminal-adjacent control is
treated as a v1 requirement. The host `terminal-window` 2.0.0 supports one
code point per cell, one font, and color-only cells. It has no per-cell
attributes, wide characters, IME, images, or OS clipboard.

The v1 scope must be sufficient for a shell, an editor surface, a dashboard,
and a settings form, while leaving the host gaps to v1.1. A component that is
listed for v1 but cannot be implemented correctly under those limits would be
a stub, and stubs are not allowed.

## Decision

We will ship this v1 component catalog:

| Group | Components |
|---|---|
| Primitives | `Panel`, `Label`, `RichLabel` (color spans only), `Separator`, `Spacer`, `Badge`, `Icon` |
| Actions | `Button`, `Checkbox`, `RadioGroup`, `RadioButton`, `Switch`, `Toolbar` |
| Text | `TextField`, `SearchField`, `TextArea`, `LogView` |
| Selection | `Select`, `ListView`, `TableView`, `TreeView` |
| Navigation | `Tabs`, `Tab`, `SplitPane`, `Form`, `Field`, `AppShell`, `StatusBar`, `ScreenHost` |
| Overlays | `MenuBar`, `Menu`, `MenuItem`, `ContextMenu`, `Dialog`, `ConfirmDialog`, `PromptDialog`, `Popover`, `Tooltip`, `Toast` |
| Scrolling and display | `ScrollView`, `ScrollBar`, `ProgressBar`, `Spinner`, `Banner` |

> **Pre-release revision (section 14):** the shipped 1.0 inventory excludes
> `Select`; selection is `ListView`, `TableView`, and `TreeView`. `Select`
> remains a v1.1 candidate in [`docs/roadmap.md`](../roadmap.md), not a hidden
> v1 stub. The authoritative 1.0 inventory is [`docs/public-api.md`](../public-api.md).

The v1 limits are explicit:

- one code point per cell; no bold, italic, underline, strikethrough, or dim;
- color comes from the theme, not per-cell attributes;
- no CJK double-width cells or combining-mark/grapheme handling;
- no IME preedit or composition;
- no images, sixel, or kitty graphics;
- no OS clipboard (the library exposes a `Clipboard` interface with an
  in-memory default);
- no arbitrary component drag/resize;
- no animations, transitions, charts, sliders, date/color/file pickers, or
  `DesktopWindow`;
- no runtime font switching;
- no RTL/bidi or accessibility bridge;
- no syntax highlighting, Markdown, or ANSI rich text;
- no scrollback beyond a caller-supplied `LogView` model.

`RichLabel` supports color spans and line breaks only; it does not parse
Markdown or ANSI. `TextArea` and `LogView` are text widgets, not code editors.
`AppShell` and `ScreenHost` host application content but do not own a native
window, a menu bar outside the matrix, or a desktop window manager.

Every omitted item is a v1.1 candidate tracked in `docs/roadmap.md`; none is a
hidden stub in v1.

## Consequences

### Positive

- Every v1 component can be implemented and tested against the host's actual
  cell model.
- The cut line prevents attribute and layout promises the renderer cannot
  keep.
- The catalog is broad enough for forms, lists, tables, trees, dialogs, and
  dashboards.
- v1.1 work has explicit prerequisites instead of vague future work.

### Negative

- Rich typography, CJK, IME, images, and OS clipboard are unavailable in v1.
- Users needing a code editor or syntax highlighting must wait for host
  attributes and GUI-side parsers.
- `DesktopWindow`, sliders, pickers, and charts are not usable in v1.
- Some users may expect a window manager from `AppShell`; the documentation
  must state otherwise.

### Neutral

- The host remains unchanged in v1; the GUI does not fork or patch it.
- New components require a new section and conformance cases, not an
  opportunistic addition.
- Color-only spans are the only rich text in v1.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Include every widget in v1 | Requires host attributes, width handling, IME, images, and more; would force stubs or broken behavior. |
| Minimal v1 (buttons and labels only) | Does not prove lists, tables, trees, overlays, or editing, which are core GUI needs. |
| Add per-cell attributes in the GUI | The host matrix has no attribute field; the GUI cannot render them. |
| Add a second GUI renderer | Duplicates `terminal-window` and breaks the host contract. |
| Implement rich text as images | No image layer in the host and no text semantics. |

## References

- `GUIDE.md` component catalog and v1 limits.
- `terminal-window` `docs/font-format.md` one code point per cell.
- `docs/roadmap.md` v1.1 candidates and host gaps.
