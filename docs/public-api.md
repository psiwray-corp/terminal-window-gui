# Public API

This page is the consumer-facing inventory for
`com.psiwray.organization:terminal-window-gui:1.0.0`. The compiled API is
frozen by `ApiSurfaceTest` and [`public-api.txt`](../library/src/test/resources/public-api.txt);
this page explains the shape of that inventory. See
[Compatibility](compatibility.md) for SemVer rules and [Architecture](architecture.md)
for package dependencies.

## Ownership boundary

The library is passive. The caller owns the `TerminalWindow`, its
`TerminalWindowService`, the host owner thread, the frame loop, input delivery,
repaint scheduling, timers, close, and OpenGL. `GuiSurface` only:

- stores the root, theme, resolved shapes, and caller clipboard;
- exposes `size`, `needsLayout`, `needsPaint`, `pointerShape`, `focus`,
  `scheduler`, `clipboard`, `overlays`, `hover`, and `keymap`;
- performs `resize`, `dispatch`, `tick`, `paint`, `setTheme`, `setShapes`,
  `requestLayout`, and `requestPaint`.

There is no `TerminalGui`, built-in event loop, background painter, library
thread, coroutine, or hidden timer. `tick(nowNanos)` runs only timers the caller
has scheduled and whose due time has passed.

The allowed `terminal-window` boundary types are `BitmapFont`,
`TerminalMatrix`, `Key`, `KeyAction`, `Modifiers`, `MouseButton`,
`CursorShape`, and host input events accepted by `GuiInput`. No `org.lwjgl.*`,
GLFW, OpenGL, window-handle, or service type appears in a public signature.

## Inventory

The exact public type set is the checked-in list of 180 class names. Companion
objects are treated as members of their owning type; the public aliases
`Field = FormField` and `Tabs = TabView` are source-level names for their
targets.

| Package | Public types |
|---|---|
| root | `GuiSurface`, `GuiInput`, `GridSize`, `GuiException` |
| `.command` | `Command`, `KeyChord`, `Keymap` |
| `.input` | `GuiEvent`, `GuiKeyEvent`, `GuiCharEvent`, `GuiPointerMoveEvent`, `GuiPointerButtonEvent`, `GuiScrollEvent`, `GuiFocusEvent`, `GuiResizeEvent`, `EventRouter`, `FocusManager`, `FocusScope`, `FocusTraversal`, `OverlayHost`, `OverlayEntry`, `OverlayAnchor` (`Rect`, `Screen`, `Widget`), `OverlayPlacement`, `DismissPolicy`, `Scheduler`, `TimerHandle`, `Clipboard`, `InMemoryClipboard`, `PointerCapture`, `HoverState` |
| `.layout` | `Constraints`, `IntSize`, `Rect`, `Length` (`Cells`, `Content`, `Fill`, `Fraction`), `Axis`, `Alignment`, `MainAxisAlignment`, `CrossAxisAlignment`, `MainAxisSize`, `Overflow` |
| `.paint` | `CellCanvas`, `PaintContext`, `TextMetrics`, `TextLayout`, `TextWrap`, `TextAlignment` |
| `.shape` | `Shape`, `ShapeSpec`, `ShapeSet` (and `Builder`), `ShapeSets`, `ResolvedShapeSet`, `MissingShapePolicy`, `MissingShapeException`, `GuiFonts` |
| `.style` | `Theme`, `Style`, `Themes`, `WidgetState` |
| `.text` | `TextEditingController`, `TextSelection` |
| `.widget` | Layout: `Widget`, `ContainerWidget`, `SingleChildWidget`, `MultiChildWidget`, `Align`, `Center`, `Column`, `Row`, `Stack` (`StackFit`), `Grid` (`GridTrack`), `Wrap`, `Padding` (`Insets`), `SizedBox`, `ConstrainedBox`, `FractionallySizedBox`, `Positioned`, `Flexible` (`FlexFit`), `Expanded`, `Spacer` |
| `.widget` | Display: `Panel`, `Separator`, `Label`, `RichLabel` (`TextSpan`), `Badge`, `Icon`, `ProgressBar`, `Spinner`, `Banner`, `Toolbar`, `KeyHint` |
| `.widget` | Controls: `Button`, `Checkbox` (`CheckState`), `RadioGroup`, `RadioButton`, `Switch`, `TextField`, `SearchField`, `TextArea` (`TextAreaWrap`, `LineNumbers`), `LogView` |
| `.widget` | Collections: `SelectionModel`, `SelectionMode`, `ListView`, `TableView`, `TableColumn`, `ColumnWidth`, `TableCell`, `SortDirection`, `TreeView`, `TreeNode` |
| `.widget` | Composites: `Tab`, `TabBar`, `TabView`/`Tabs`, `SplitPane` (`SplitOrientation`), `Form`, `FormField`/`Field` (`FormLabelWidth`; deprecated `FormField.LabelWidth`), `AppShell`, `StatusBar`, `Screen`, `ScreenHost` |
| `.widget` | Menus and overlays: `Menu`, `MenuBar`, `MenuItem` (`CommandItem`, `Separator`, `Submenu`), `MenuPopup`, `ContextMenu`, `Dialog`, `DialogAction`, `DialogResult` (`Key`, `CONFIRMED`, `CANCELLED`), `ConfirmDialog`, `PromptDialog`, `Popover`, `Tooltip`, `TooltipController`, `Toast`, `ToastManager`, `ToastCorner`, `Severity`, `Orientation`, `BoxStyle`, `ScrollView`, `ScrollBar`, `ScrollBarVisibility`, `ScrollModel` |

## `GuiSurface` surface

| Member | Meaning |
|---|---|
| `root` | Root widget, arranged at `(0, 0, columns, rows)` |
| `theme`, `shapes` | Current immutable theme and resolved shape set; replace with `setTheme`/`setShapes` |
| `size` | Current `GridSize`; starts at zero |
| `needsLayout`, `needsPaint` | Dirty flags for the caller's loop |
| `pointerShape` | Advisory `CursorShape?` for `TerminalWindow.setCursor` |
| `focus` | `FocusManager` |
| `scheduler` | Caller-driven `Scheduler` |
| `clipboard` | Caller-owned `Clipboard` |
| `overlays` | `OverlayHost` |
| `hover` | `HoverState` |
| `keymap` | Global `Keymap`, checked after focused-widget handling |
| `screenScope` | Active keymap screen scope |

`GuiSurface` is not `Closeable` and owns no native resource.

## Thread affinity

| Execution context | Permitted work | Must not do |
|---|---|---|
| Surface thread | Construct `GuiSurface`; call `resize`, `dispatch`, `tick`, `paint`, `requestLayout`, `requestPaint`, `setTheme`, `setShapes`; mutate widgets, controllers, focus, overlays, scheduler, and clipboard | Call GL, GLFW, `TerminalWindow`, or `TerminalWindowService` |
| Host owner thread | `window.frame()`, host input callbacks, read `window.matrix`, `requestRepaint`, `setCursor`, `setTitle` | Touch a GUI surface concurrently from another thread |
| Any thread | Immutable values (`GridSize`, `Rect`, `Constraints`, `Theme`, `Shape`, events); `GuiInput` translation; `BitmapFont` parsing; resolving an immutable `ShapeSet` | Mutate an attached surface, widget, controller, or layout cache |
| Caller clipboard | `Clipboard` reads/writes when called by the surface thread | Assume the GUI synchronizes the implementation |
| Library | None | Create threads, coroutines, watchers, or background repaint |

The surface has no synchronization. A caller that uses a separate UI thread
must marshal host events to that thread and ensure only one thread calls surface
methods. `Clipboard` implementations are caller-owned and document their own
thread rules.

## Extension points

Custom widgets subclass `Widget` and override `measure`, `arrange`/paint
hooks, and `onEvent`. Custom decoration uses `PaintContext` and `Shape` roles,
not raw glyphs. Custom text uses `TextMetrics`, not a platform font API.
Custom command handling uses `Command`, `KeyChord`, and `Keymap`.

## Related

[Compatibility](compatibility.md) · [Architecture](architecture.md) ·
[Widgets](widgets.md) · [Testing](testing.md) · [Roadmap](roadmap.md)
