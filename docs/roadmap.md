# Roadmap

Version 1.0 ships the frozen component inventory in [`public-api.md`](public-api.md)
and [`widgets.md`](widgets.md). Version 1.1 candidates are listed here. Every
item is labeled:

| Label | Meaning |
|---|---|
| `v1.1` | Buildable in the GUI once scheduled; no host change required |
| `blocked` | Requires the named `terminal-window` change first |
| `not planned` | Outside the 1.x horizon; listed only if explicitly removed |

The `not planned` label is reserved for the explicit non-goals at the end of
this document. No candidate in the two v1.1 tables is labeled `not planned`,
and no item is a stub in v1.

## Blocked on `terminal-window`

| Feature | Required in `terminal-window` | Scale | Status |
|---|---|---|---|
| Bold, italic, underline, strikethrough, dim | Per-cell attribute model, public `TerminalMatrix.set` overload, shader/atlas support. The 16-byte instance has a reserved `u16` at offset 6, so flags can fit without a stride change; underline/strikethrough can be fragment-shader work, bold/italic need atlas variants or synthetic transforms. | Minor/major | `blocked` |
| CJK double-width, combining marks, grapheme clusters | Width-aware cell spans, grapheme storage, renderer clipping/skipping, caret and clipboard index mapping. | Major | `blocked` |
| IME preedit/composition | Preedit and commit events, text-input rectangle for the candidate window, platform/GLFW spike. `CharEvent` alone is not enough. | Major | `blocked` |
| OS clipboard | `TerminalWindow` get/set clipboard strings mapped to GLFW clipboard calls. | Minor | `blocked` |
| Runtime font switching and fallback chain | Font/atlas lifecycle: `setFont` or controlled reopen, owner-thread atlas rebuild, per-glyph fallback chain. The current font is fixed at window creation. | Minor/major | `blocked` |
| Images/pixels | Image/texture layer or sixel/kitty protocol support. | Major | `blocked` |
| RTL/bidi | Bidi reordering and width in the matrix; layout mirroring follows. | Major | `blocked` |
| Accessibility bridge | Accessible tree and platform integration for an OpenGL window. | Major | `blocked` |
| Sub-cell click precision | Pixel coordinates on `MouseButtonEvent`; `MouseMoveEvent` already has pixels. | Minor | `blocked` |
| Markdown/ANSI rich text, syntax highlighting | Attribute model for full fidelity; color-only spans are GUI-only. | GUI + attributes | `blocked` |
| Scrollback/history | No host gap; GUI-side log model and virtualization. | GUI-only | `v1.1` |

The first ten items cannot be implemented correctly by the GUI alone. The
reserved instance `u16` means basic attributes do not necessarily require a
renderer stride change, but they still require a host API and shader/atlas
work. Width, IME, images, bidi, and accessibility are host-level changes.

## GUI-only

These items can be built on the v1 cell model without a `terminal-window`
change. They are candidates, not commitments to a release date.

| Feature | Notes | Status |
|---|---|---|
| `DesktopWindow` | Movable/resizable/z-ordered internal windows; not native OS windows | `v1.1` |
| Slider | Horizontal/vertical integer or fraction control | `v1.1` |
| Command palette | Searchable command list over `Command`/`Keymap` | `v1.1` |
| Date, color, and file pickers | Cell-grid pickers; file picker returns paths, not OS dialogs | `v1.1` |
| Charts and sparklines | Cell/block rendering, no image layer | `v1.1` |
| Variable-height list rows | Measured row heights with virtualization | `v1.1` |
| Lazy tree children | Async/callback expansion, loading state | `v1.1` |
| Content-width table columns | Measure cells and clamp to available width | `v1.1` |
| In-cell table editing | Editor overlay anchored to a cell | `v1.1` |
| Drag-and-drop reorder | List/tree drop targets and insertion indicator | `v1.1` |
| Animations/transitions | Caller-driven frames and `tick`; no background thread | `v1.1` |
| Menu/toolbar overflow | Collapse items into an overflow popup | `v1.1` |
| Smooth/kinetic scrolling | Caller-driven decay and clamp | `v1.1` |
| Rich tooltips/toasts | Multi-line content, severity, actions, timers | `v1.1` |
| Multiple GUI windows | Independent `GuiSurface` instances on caller-owned host windows | `v1.1` |
| Form validation engine | Cross-field rules and error summaries | `v1.1` |
| Hyperlinks/click regions | Cell-range hit regions and caller callback | `v1.1` |
| Persistent log history | Bounded ring buffer and virtualized `LogView` | `v1.1` |

## Not planned for 1.x

These are outside the 1.x horizon and must not be documented as supported:

| Feature | Status |
|---|---|
| TTF/OTF shaping and fallback beyond the resolved shape set | `not planned` |
| Native window manager, OS menu bar, or OS file dialog | `not planned` |
| Code editing, syntax highlighting, Markdown/ANSI rich text | `not planned` |
| Images, sixel, kitty graphics, or a second renderer | `not planned` |
| Network/plugin system | `not planned` |

If one becomes a requirement, it needs an ADR, a host capability decision, and
a new section; it is not a patch to v1.

## Related

[Compatibility](compatibility.md) · [Public API](public-api.md) ·
[Architecture](architecture.md) · [ADR-0011](adr/0011-v1-1-roadmap.md)
