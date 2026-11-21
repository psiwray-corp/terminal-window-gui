# Theming

A theme is an immutable set of ARGB color tokens. The host cell model supports
foreground and background color only; there are no bold, italic, underline, or
other attributes in v1. Widgets combine color with shape, markers, and cursor
position so focus and selection remain distinguishable in monochrome themes.

## Tokens

[`Theme`](../library/src/main/kotlin/com/psiwray/organization/terminal_window_gui/style/Theme.kt)
contains:

| Token | Role |
|---|---|
| `background` | Surface background |
| `surface` | Normal widget background |
| `surfaceAlt` | Alternate/hover background |
| `text` | Primary text |
| `textMuted` | Secondary text |
| `textDisabled` | Disabled text |
| `accent` | Emphasis and active indicator |
| `success`, `warning`, `error` | Semantic feedback |
| `border`, `borderFocused` | Normal and focused borders |
| `selectionBackground`, `selectionForeground` | Selected rows and text selection |
| `caret` | Text caret |
| `shadow` | Reserved shadow/ghost color |

`Theme.style(state)` returns the `Style(fg, bg)` for a `WidgetState`:

| State | Foreground | Background |
|---|---|---|
| `NORMAL` | `text` | `surface` |
| `FOCUSED` | alternate text fallback | `surfaceAlt` |
| `HOVERED` | alternate text fallback | `surfaceAlt` |
| `PRESSED` | `selectionForeground` | `accent` |
| `DISABLED` | `textDisabled` | `surface` |
| `SELECTED` | `selectionForeground` | `selectionBackground` |
| `ERROR` | `error` | `surface` |

`Style.resolve(theme, state)` is the convenience form. Applications may use any
`Style` for custom painting; component state styles use the table above.

## Built-in themes

`Themes.dark()`, `Themes.light()`, and `Themes.highContrast()` are immutable
factory methods. A caller may construct its own `Theme` and install it with
`GuiSurface.setTheme`, which invalidates paint only. Replacing a theme never
changes layout, widget state, focus, or model data.

The examples use `Themes.dark()`; the gallery switches dark, light, and
high-contrast at runtime with F1.

## Monochrome and focus rules

The high-contrast theme maps semantic colors to black and white. Focus must not
depend on color alone:

- `Panel` uses the focused border token and an inverted title.
- `Button` uses a `*` default/focus marker and an ellipsis for truncation.
- `Label`/`RichLabel` keep the selected text and background distinct.
- `Checkbox`, `RadioGroup`, and `Switch` use semantic shapes and labels.
- `MenuBar` uses an active marker that survives a color-blind or monochrome
  palette.
- `StatusBar` and `KeyHint` use muted text and separators.

A custom widget should follow the same rule: pair color with a semantic `Shape`
or a non-color marker.

## Changing theme and shapes

`GuiSurface.setTheme(theme)` and `GuiSurface.setShapes(shapes)` are
single-thread-affine calls. Both invalidate paint, not layout. The caller
resolves a shape set against the font it owns and must pass the resulting
`ResolvedShapeSet`; the library never switches a window font.

## Related

[Shapes](shapes.md) · [Widgets](widgets.md) · [Architecture](architecture.md) ·
[Public API](public-api.md)
