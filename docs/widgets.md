# Widgets

This is the v1 component catalog. Every widget paints through `PaintContext`,
uses semantic `Shape` roles, and participates in the same measure/arrange/event
contract. The columns give the intrinsic minimum before parent constraints; a
parent may still force a smaller allocation, in which case the widget clips or
truncates rather than painting outside its bounds. `1×1` is a supported surface
for every entry and is covered by the conformance suite.

Input notation: `Tab` means traversal, `Space`/`Enter` activation, `Esc`
cancel/dismiss, arrows navigation, and `click` the pointer path. Widgets that
take text also accept `GuiCharEvent` and selection keys.

## Layout and decoration

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `Align`, `Center` | child or 0 | none | Places child by `Alignment`; fills a bounded parent |
| `Row`, `Column` | flex result of children | none | `MainAxisAlignment`, `CrossAxisAlignment`, `MainAxisSize`, `gap`; `Flexible`/`Expanded` children |
| `Stack` | largest child, or bounded parent | none | `LOOSE`/`EXPAND`; positioned children do not contribute to loose size |
| `Grid` | track sizes; 0 empty | none | Fixed/fraction/flex column and row tracks |
| `Wrap` | widest run, total height | none | Wraps at available width; `gap`, `runGap` |
| `Padding` | child + `Insets` | none | One-child inset wrapper |
| `SizedBox` | set extent or child | none | Tightens its child |
| `ConstrainedBox` | child clamped by bounds | none | Parent/child constraint intersection |
| `FractionallySizedBox` | fraction of bounded parent | none | Rounds and clamps |
| `Positioned` | resolved rect | none | Stack parent-data; edge/fill/content lengths |
| `Spacer` | 0 | none | Flex in Row/Column |
| `Panel` | 2×2 plus padding | none | Optional title; single/double/rounded border; focused title |
| `Separator` | 1×1 | none | Horizontal or vertical line with optional centered label |
| `Badge` | text cells, at least 1×1 | none | Accent/selection colors |
| `Icon` | 1×1 | none | `Shape` or code-point glyph, no hardcoded widget glyphs |
| `ProgressBar` | 3×1 | none | Determinate value, indeterminate frame, start/stop |
| `Spinner` | 1×1 | none | Caller-driven frame; optional auto-start |
| `Banner` | 3×1 plus text/action/close | `Enter`, `Esc`, click | Severity marker, optional action and close; Tab stop only when actionable |
| `KeyHint` | text length, 1 row | none | Muted `keys label` |
| `Toolbar` | 1 row; sum children + gaps | child events | Splits width; fixed children shrink |

## Controls

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `Button` | `2 + icon (2) + label`, 1 row | `Space`, default `Enter`, `Alt+mnemonic`, click | Normal/focused/hovered/pressed/disabled; ellipsis; one activation per press |
| `Checkbox` | `4 + label`, 1 row | `Space`, click | Checked, unchecked, indeterminate; `allowIndeterminate`; label |
| `RadioGroup` | widest button × one row per child | arrows, `Space`, click | Roving focus; one selected key; empty group allowed |
| `RadioButton` | `4 + label`, 1 row | group keys, click | Selected/unselected/disabled; belongs to one group |
| `Switch` | `5 + label`, 1 row | `Space`, click | On/off/disabled |
| `TextField` | 1×1 | chars, caret/selection, Home/End, clipboard, `Enter` submit | Placeholder, mask, read-only, error, horizontal scroll |
| `SearchField` | field width + clear cell, 1 row | chars, `Enter`, click clear | Composes a `TextField`; clear restores focus |
| `TextArea` | 1×1 plus optional gutter | chars, arrows, Home/End, PgUp/PgDn, selection, clipboard | Wrapped/none, optional line numbers, read-only, scroll |
| `LogView` | 1×1 | arrows, PgUp/PgDn, wheel | Read-only lines, wrap, follow-tail, scroll lock, optional controller |

Text controls share `TextEditingController`: code-point-safe selection,
undo/redo, cut/copy/paste through `Clipboard`, password masking, and ASCII word
navigation. There is no IME preedit in v1.

## Collections

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `ListView<T>` | 1×1; visible rows | arrows, Home/End, PgUp/PgDn, type-ahead, click, Ctrl/Shift click, double-click, wheel | Single/multiple/none selection; stable-key selection; virtualization; empty text |
| `TableView<T>` | 1×1; header + visible rows | header click callback, arrows, click, wheel | Fixed/flex/fraction columns; frozen header; horizontal scroll; sort display is caller-owned |
| `TreeView<T>` | 1×1; visible flattened nodes | arrows, Home/End, expansion keys, click, wheel | Stable-key selection; expansion set; guide shapes; eager children |
| `SelectionModel`, `SelectionMode` | n/a | n/a | Stable-key cursor/anchor/selection model |
| `TableColumn`, `ColumnWidth`, `TableCell`, `SortDirection` | n/a | n/a | Table configuration and display values |

## Composite containers

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `TabBar` | sum tab labels, 1 row | arrows, Home/End, click, wheel | Scrolls horizontally; `Tab` content |
| `TabView`/`Tabs` | 1×1; bar + active content | bar input; Tab | Inactive content detached; per-tab focus restore |
| `SplitPane` | max child extents; 1×1 | drag divider, arrows when focused, double-click reset | Clamped ratio and minimums; hidden divider fallback |
| `Form` | 1×1; rows | child input | `FormField` rows, label width, gap |
| `FormField`/`Field` | 1×1; label + control + message | child input | Help/error, required marker, fixed/fraction/content label width |
| `AppShell` | 1×1 | menu/content/status input | Rows: menu bar, content, status bar |
| `StatusBar` | 1×1 | none | Left/center/right text and `KeyHint` list with truncation |
| `ScreenHost` | 1×1 | screen content, back keys | Push/pop/replace, per-screen keymap and callbacks, focus restore |
| `Screen` | content size | screen keys | Key/title/content; enter/exit/resize/back hooks |

## Menus, dialogs, and overlays

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `MenuBar` | menu titles, 1 row | F10, Alt mnemonic, arrows, Esc, click | Active marker; opens one popup; focus restore; not a Tab stop |
| `Menu`, `MenuItem` | n/a | n/a | Command/submenu/separator data |
| `MenuPopup` | widest entry, one row per item | arrows, Home/End, Enter, Esc, click, wheel | Overlay root; checked/radio markers; tall-list scroll; submenus |
| `ContextMenu` | menu width | same as popup | Opens at cell/widget/rect; no owning menu bar |
| `Dialog` | 1×1; body + actions | Tab trap, Esc cancel, Enter default, click | Modal overlay; one-shot result; body scroll |
| `ConfirmDialog` | dialog minimum | Esc/Enter/click | Boolean result; confirm default |
| `PromptDialog` | dialog minimum | text field + actions | Display-only error; password/read-only; string result |
| `Popover` | child size | Esc/click outside per `DismissPolicy` | Auto/above/below placement; optional modal |
| `Tooltip` | 1×1, capped `maxWidth` | none | Display content; scheduler-driven controller |
| `Toast` | `max(4, message + 4)`, 1 row | click close | Severity, duration, managed by `ToastManager` |

## Scrolling

| Widget | Intrinsic minimum | Input | States/notes |
|---|---|---|---|
| `ScrollView` | child viewport, 1×1 | arrows, PgUp/PgDn, Home/End, wheel | One child; content offsets; clipping; not a Tab stop by default |
| `ScrollBar` | 0 when hidden; 1×1 when shown | arrows, PgUp/PgDn, Home/End, click track, drag thumb | Auto/always/never; horizontal/vertical |
| `ScrollModel` | n/a | caller methods | Clamped offsets; follow-end; scroll-to/ensure-visible |

## Common states and accessibility

All applicable widgets implement normal, focused, hovered, pressed, disabled,
selected, checked/indeterminate, error, empty, truncated, minimum-size, and
`1×1` states. Shape tests cover bundled Unicode, ASCII fallback, missing primary
with fallback, missing fallback with last resort, and strict failure. Themes
cover dark, light, and high-contrast; color is never the only focus or selection
signal.

The GUI has no accessibility bridge, screen-reader API, wide-character layout,
attributes, IME, images, or OS clipboard in v1. Those are roadmap items, not
silent fallbacks.

## Related

[Layout](layout.md) · [Input](input.md) · [Theming](theming.md) ·
[Shapes](shapes.md) · [Testing](testing.md) · [Compatibility](compatibility.md)
