package com.psiwray.organization.terminal_window_gui.examples.gallery

import com.psiwray.organization.terminal_window.TerminalWindow
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.examples.ExampleApp
import com.psiwray.organization.terminal_window_gui.examples.ExampleLoop
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Alignment
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.MainAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import com.psiwray.organization.terminal_window_gui.widget.AppShell
import com.psiwray.organization.terminal_window_gui.widget.Badge
import com.psiwray.organization.terminal_window_gui.widget.Banner
import com.psiwray.organization.terminal_window_gui.widget.Button
import com.psiwray.organization.terminal_window_gui.widget.CheckState
import com.psiwray.organization.terminal_window_gui.widget.Checkbox
import com.psiwray.organization.terminal_window_gui.widget.Column
import com.psiwray.organization.terminal_window_gui.widget.ConfirmDialog
import com.psiwray.organization.terminal_window_gui.widget.ContextMenu
import com.psiwray.organization.terminal_window_gui.widget.Dialog
import com.psiwray.organization.terminal_window_gui.widget.DialogAction
import com.psiwray.organization.terminal_window_gui.widget.DialogResult
import com.psiwray.organization.terminal_window_gui.widget.Expanded
import com.psiwray.organization.terminal_window_gui.widget.Form
import com.psiwray.organization.terminal_window_gui.widget.FormField
import com.psiwray.organization.terminal_window_gui.widget.FormLabelWidth
import com.psiwray.organization.terminal_window_gui.widget.Grid
import com.psiwray.organization.terminal_window_gui.widget.GridTrack
import com.psiwray.organization.terminal_window_gui.widget.Icon
import com.psiwray.organization.terminal_window_gui.widget.Label
import com.psiwray.organization.terminal_window_gui.widget.LineNumbers
import com.psiwray.organization.terminal_window_gui.widget.ListView
import com.psiwray.organization.terminal_window_gui.widget.LogView
import com.psiwray.organization.terminal_window_gui.widget.Menu
import com.psiwray.organization.terminal_window_gui.widget.MenuBar
import com.psiwray.organization.terminal_window_gui.widget.MenuItem
import com.psiwray.organization.terminal_window_gui.widget.Panel
import com.psiwray.organization.terminal_window_gui.widget.Popover
import com.psiwray.organization.terminal_window_gui.widget.ProgressBar
import com.psiwray.organization.terminal_window_gui.widget.PromptDialog
import com.psiwray.organization.terminal_window_gui.widget.RadioButton
import com.psiwray.organization.terminal_window_gui.widget.RadioGroup
import com.psiwray.organization.terminal_window_gui.widget.RichLabel
import com.psiwray.organization.terminal_window_gui.widget.Row
import com.psiwray.organization.terminal_window_gui.widget.ScrollBar
import com.psiwray.organization.terminal_window_gui.widget.ScrollBarVisibility
import com.psiwray.organization.terminal_window_gui.widget.ScrollModel
import com.psiwray.organization.terminal_window_gui.widget.ScrollView
import com.psiwray.organization.terminal_window_gui.widget.SearchField
import com.psiwray.organization.terminal_window_gui.widget.Separator
import com.psiwray.organization.terminal_window_gui.widget.Severity
import com.psiwray.organization.terminal_window_gui.widget.SizedBox
import com.psiwray.organization.terminal_window_gui.widget.Spacer
import com.psiwray.organization.terminal_window_gui.widget.Spinner
import com.psiwray.organization.terminal_window_gui.widget.SplitPane
import com.psiwray.organization.terminal_window_gui.widget.StatusBar
import com.psiwray.organization.terminal_window_gui.widget.Switch
import com.psiwray.organization.terminal_window_gui.widget.Tab
import com.psiwray.organization.terminal_window_gui.widget.TabView
import com.psiwray.organization.terminal_window_gui.widget.TableCell
import com.psiwray.organization.terminal_window_gui.widget.TableColumn
import com.psiwray.organization.terminal_window_gui.widget.TableView
import com.psiwray.organization.terminal_window_gui.widget.TextArea
import com.psiwray.organization.terminal_window_gui.widget.TextField
import com.psiwray.organization.terminal_window_gui.widget.TextSpan
import com.psiwray.organization.terminal_window_gui.widget.ToastManager
import com.psiwray.organization.terminal_window_gui.widget.Toolbar
import com.psiwray.organization.terminal_window_gui.widget.TreeNode
import com.psiwray.organization.terminal_window_gui.widget.TreeView
import com.psiwray.organization.terminal_window_gui.widget.Widget

/** Entry point for the visual gallery. */
public fun main(args: Array<String>) {
    ExampleLoop.run(GalleryApp(), args)
}

private class GalleryApp : ExampleApp("terminal-window-gui | gallery", columns = 100, rows = 32) {
    private lateinit var surface: GuiSurface
    private lateinit var window: TerminalWindow
    private lateinit var toast: ToastManager
    private val tooltipTarget = Button(label = "Tooltip target")
    private val scrollingDemoModel = ScrollModel()
    private var theme = Themes.dark()
    private var shapes = ShapeSets.unicode()
    private val font = GuiFonts.bundled()

    override val initialTheme: Theme get() = theme

    override val initialShapes: com.psiwray.organization.terminal_window_gui.shape.ShapeSet
        get() = shapes

    override fun configure(
        surface: GuiSurface,
        window: TerminalWindow,
    ) {
        this.surface = surface
        this.window = window
        toast = ToastManager(surface)
        com.psiwray.organization.terminal_window_gui.widget
            .TooltipController(surface, 500)
            .register(tooltipTarget, "Hover me for a delayed tooltip.\nF1 changes the theme.")
        surface.keymap.bind(
            KeyChord(Key.F1),
            Command("theme", action = { cycleTheme() }),
        )
        surface.keymap.bind(
            KeyChord(Key.F2),
            Command("shapes", action = { cycleShapes() }),
        )
        surface.keymap.bind(
            KeyChord(Key.F3),
            Command("help", action = { showHelp() }),
        )
    }

    override fun root(): Widget {
        val menu = buildMenu()
        val status = StatusBar(left = "Gallery", center = "", right = "F1 theme | F2 shapes | F3 help | Ctrl+Q quit")
        val content =
            Column(
                gap = 1,
                children =
                    listOf(
                        Banner(
                            message = "Gallery of v1 widgets. Theme and shape sets switch without restarting.",
                            severity = Severity.INFO,
                            closable = false,
                        ),
                        Toolbar(
                            listOf(
                                Button(label = "Dialog", onActivate = { showDialog() }),
                                Button(label = "Confirm", onActivate = { showConfirm() }),
                                Button(label = "Prompt", onActivate = { showPrompt() }),
                                Button(label = "Toast", onActivate = { showToast() }),
                                Button(label = "Popover", onActivate = { showPopover() }),
                                Button(label = "Menu", onActivate = { showContextMenu() }),
                                Button(label = "Theme", onActivate = { cycleTheme() }),
                                Button(label = "ASCII", onActivate = { cycleShapes() }),
                                Spacer(),
                                Label("100x32"),
                            ),
                        ),
                        Expanded(child = ScrollView(buildPage())),
                    ),
            )
        return AppShell(menuBar = menu, content = content, statusBar = status)
    }

    private fun buildPage(): Widget =
        Column(
            gap = 1,
            mainAxisAlignment = MainAxisAlignment.START,
            crossAxisAlignment = CrossAxisAlignment.START,
            children =
                listOf(
                    section(
                        "Labels, separators, badges, icons",
                        Column(
                            gap = 0,
                            children =
                                listOf(
                                    Label("Label with ellipsis and wrapping"),
                                    RichLabel(
                                        spans =
                                            listOf(
                                                TextSpan("RichLabel "),
                                                TextSpan("with color spans", fg = theme.accent),
                                            ),
                                    ),
                                    Separator(),
                                    Row(
                                        gap = 1,
                                        children =
                                            listOf(
                                                Badge(text = "v1"),
                                                Icon(shape = Shape.CHECK),
                                                Icon(shape = Shape.ARROW_RIGHT),
                                                tooltipTarget,
                                                Label("| separators above"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                    section(
                        "Buttons, toggles, toolbar",
                        Column(
                            gap = 1,
                            children =
                                listOf(
                                    Row(
                                        gap = 1,
                                        children =
                                            listOf(
                                                Button(label = "Default", default = true),
                                                Button(label = "Disabled", enabled = false),
                                                Button(label = "Icon", icon = Shape.ARROW_RIGHT),
                                            ),
                                    ),
                                    Checkbox(label = "Checkbox", state = CheckState.CHECKED),
                                    Checkbox(
                                        label = "Indeterminate",
                                        allowIndeterminate = true,
                                        state = CheckState.INDETERMINATE,
                                    ),
                                    RadioGroup(
                                        selectedKey = "one",
                                        children =
                                            listOf(
                                                RadioButton("one", "Radio one"),
                                                RadioButton("two", "Radio two"),
                                            ),
                                    ),
                                    Switch(label = "Switch", checked = true),
                                    Toolbar(
                                        listOf(
                                            Button(label = "Bold"),
                                            Button(label = "Italic"),
                                            Separator(),
                                            Spinner(),
                                            Label("spinner"),
                                        ),
                                    ),
                                ),
                        ),
                    ),
                    section(
                        "Text editing",
                        Column(
                            gap = 1,
                            children =
                                listOf(
                                    TextField("Text field", onSubmit = { showToast("Submitted: $it", Severity.INFO) }),
                                    SearchField(placeholder = "Search field"),
                                    SizedBox(
                                        height = 4,
                                        child =
                                            TextArea(
                                                controller = TextEditingController("TextArea line 1\nTextArea line 2"),
                                                lineNumbers = LineNumbers(),
                                            ),
                                    ),
                                    SizedBox(
                                        height = 4,
                                        child =
                                            LogView(
                                                lines = listOf("log: started", "log: ready", "log: follow-tail"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                    section(
                        "Collections",
                        Column(
                            gap = 1,
                            children =
                                listOf(
                                    SizedBox(
                                        height = 4,
                                        child =
                                            ListView(
                                                items = listOf("Alpha", "Beta", "Gamma", "Delta"),
                                                itemBuilder = { index, item ->
                                                    Row(
                                                        gap = 2,
                                                        children = listOf(Label("$index"), Label(item)),
                                                    )
                                                },
                                            ),
                                    ),
                                    SizedBox(
                                        height = 5,
                                        child =
                                            TableView(
                                                items =
                                                    listOf(
                                                        TableRow("Ada", "Engineer", "42"),
                                                        TableRow("Grace", "Admiral", "85"),
                                                        TableRow("Linus", "Kernel", "31"),
                                                    ),
                                                columns =
                                                    listOf(
                                                        TableColumn("Name", cell = { TableCell(it.values[0]) }),
                                                        TableColumn("Role", cell = { TableCell(it.values[1]) }),
                                                        TableColumn(
                                                            "Score",
                                                            alignment = TextAlignment.RIGHT,
                                                            cell = { TableCell(it.values[2]) },
                                                        ),
                                                    ),
                                            ),
                                    ),
                                    SizedBox(
                                        height = 5,
                                        child =
                                            TreeView(
                                                nodes =
                                                    listOf(
                                                        TreeNode(
                                                            key = "src",
                                                            label = "src",
                                                            children =
                                                                listOf(
                                                                    TreeNode("main", "main"),
                                                                    TreeNode("test", "test"),
                                                                ),
                                                        ),
                                                    ),
                                                keyOf = { it.key },
                                                label = { it.label },
                                                children = { it.children },
                                                expandedKeys = setOf("src"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                    section(
                        "Containers and navigation",
                        Column(
                            gap = 1,
                            children =
                                listOf(
                                    SizedBox(
                                        height = 5,
                                        child =
                                            TabView(
                                                tabs =
                                                    listOf(
                                                        Tab("General", Label("TabView general content")),
                                                        Tab("Details", Label("TabView details content")),
                                                    ),
                                            ),
                                    ),
                                    SizedBox(
                                        height = 6,
                                        child =
                                            SplitPane(
                                                first = Panel("Left pane", child = Label("SplitPane")),
                                                second = Panel("Right pane", child = Label("drag the divider")),
                                                ratio = 0.45,
                                            ),
                                    ),
                                    Form(
                                        listOf(
                                            FormField("Name", TextField("Ada"), helpText = "Form field help"),
                                            FormField("Role", TextField("Engineer"), required = true),
                                        ),
                                        labelWidth = FormLabelWidth.Fixed(8),
                                    ),
                                    Grid(
                                        columns = listOf(GridTrack.fraction(1f), GridTrack.fraction(1f)),
                                        gap = 1,
                                        children = listOf(Label("Grid left"), Label("Grid right")),
                                    ),
                                ),
                        ),
                    ),
                    section(
                        "Status and scrolling",
                        Column(
                            gap = 1,
                            children =
                                listOf(
                                    ProgressBar(value = 0.42, showPercent = true),
                                    ProgressBar(indeterminate = true),
                                    Row(gap = 1, children = listOf(Spinner(), Label("Spinner and progress bars"))),
                                    SizedBox(
                                        height = 6,
                                        child =
                                            Row(
                                                children =
                                                    listOf(
                                                        Expanded(
                                                            child =
                                                                ScrollView(
                                                                    model = scrollingDemoModel,
                                                                    child =
                                                                        Label(
                                                                            (1..20).joinToString(
                                                                                "\n",
                                                                            ) { "scroll row $it" },
                                                                        ),
                                                                ),
                                                        ),
                                                        ScrollBar(
                                                            model = scrollingDemoModel,
                                                            visibility = ScrollBarVisibility.ALWAYS,
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                ),
        )

    private fun section(
        title: String,
        content: Widget,
    ): Widget = Panel(title = title, child = content)

    private fun buildMenu(): MenuBar {
        val quit =
            Command(
                id = "quit",
                label = "Quit",
                shortcut = KeyChord(Key.Q, Modifiers.CONTROL),
                action = { window.requestClose() },
            )
        val dark =
            Command(id = "theme.dark", label = "Dark", action = { setTheme(Themes.dark()) })
        val light =
            Command(id = "theme.light", label = "Light", action = { setTheme(Themes.light()) })
        val contrast =
            Command(id = "theme.contrast", label = "High contrast", action = { setTheme(Themes.highContrast()) })
        val unicode =
            Command(id = "shape.unicode", label = "Unicode", action = { setShapes(ShapeSets.unicode()) })
        val ascii =
            Command(id = "shape.ascii", label = "ASCII", action = { setShapes(ShapeSets.ascii()) })
        val help = Command(id = "help", label = "Keyboard help", action = { showHelp() })
        return MenuBar(
            Menu("File", 'F', listOf(MenuItem.CommandItem(quit))),
            Menu(
                "View",
                'V',
                listOf(
                    MenuItem.CommandItem(dark),
                    MenuItem.CommandItem(light),
                    MenuItem.CommandItem(contrast),
                    MenuItem.Separator,
                    MenuItem.CommandItem(unicode),
                    MenuItem.CommandItem(ascii),
                    MenuItem.Separator,
                    MenuItem.CommandItem(help),
                ),
            ),
            Menu("Help", 'H', listOf(MenuItem.CommandItem(help))),
        )
    }

    private fun cycleTheme() {
        theme =
            when {
                theme == Themes.dark() -> Themes.light()
                theme == Themes.light() -> Themes.highContrast()
                else -> Themes.dark()
            }
        surface.setTheme(theme)
    }

    private fun setTheme(next: Theme) {
        theme = next
        surface.setTheme(next)
    }

    private fun cycleShapes() {
        shapes = if (shapes === ShapeSets.unicode()) ShapeSets.ascii() else ShapeSets.unicode()
        surface.setShapes(shapes.resolve(font))
        showToast("Shape set: ${if (shapes === ShapeSets.ascii()) "ASCII" else "Unicode"}", Severity.INFO, 1500)
    }

    private fun setShapes(next: com.psiwray.organization.terminal_window_gui.shape.ShapeSet) {
        shapes = next
        surface.setShapes(next.resolve(font))
    }

    private fun showDialog() {
        Dialog(
            title = "Dialog",
            body =
                Label(
                    (1..40).joinToString("\n") { "scrollable dialog body line $it" },
                ),
            actions =
                listOf(
                    DialogAction("Cancel", cancel = true),
                    DialogAction("OK", default = true),
                ),
            result = { result: DialogResult -> showToast("Dialog result: $result", Severity.INFO, 1500) },
        ).show(surface.overlays)
    }

    private fun showConfirm() {
        ConfirmDialog(
            "Confirm",
            "Proceed with the gallery action?",
            result = { ok -> showToast("Confirmed: $ok", Severity.INFO, 1500) },
        ).show(surface.overlays)
    }

    private fun showPrompt() {
        PromptDialog(
            "Prompt",
            "Enter a value",
            initialValue = "Terminal",
            result = { value -> showToast("Prompt: ${value ?: "cancelled"}", Severity.INFO, 1500) },
        ).show(surface.overlays)
    }

    private fun showToast() {
        showToast("Hello from ToastManager", Severity.SUCCESS)
    }

    private fun showToast(
        message: String,
        severity: Severity,
        duration: Long = 2500,
    ) {
        toast.show(message, severity, duration)
    }

    private fun showPopover() {
        val popover =
            Popover(
                content = Panel("Popover", child = Label("Anchored content with flip/clamp.")),
            )
        popover.show(surface.overlays, Rect(8, 5, 20, 4))
    }

    private fun showContextMenu() {
        val run =
            Command(
                id = "context.run",
                label = "Run command",
                action = { showToast("Context command executed", Severity.SUCCESS) },
            )
        ContextMenu(
            listOf(
                MenuItem.CommandItem(run),
                MenuItem.Separator,
                MenuItem.CommandItem(Command("context.close", "Close", action = {})),
            ),
        ).open(surface.overlays, 10, 6)
    }

    private fun showHelp() {
        Dialog(
            title = "Keyboard help",
            body =
                Label(
                    "F10 opens the menu bar.\n" +
                        "F1 cycles themes; F2 cycles Unicode/ASCII shapes.\n" +
                        "F3 opens this help. Ctrl+Q quits.\n" +
                        "Tab and Shift+Tab traverse focus. Esc closes overlays.",
                ),
            actions = listOf(DialogAction("Close", default = true)),
        ).show(surface.overlays, OverlayAnchor.Screen)
    }
}

private data class TableRow(
    val values: List<String>,
) {
    constructor(vararg values: String) : this(values.toList())
}
