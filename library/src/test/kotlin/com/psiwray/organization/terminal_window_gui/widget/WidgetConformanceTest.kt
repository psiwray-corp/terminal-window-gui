package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.layout.Length
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the v1 widget catalog and gives every public concrete widget a
 * render/interact case. The interaction is intentionally generic: it proves
 * that the widget can be reached by pointer and key routing without relying on
 * a bespoke test for every control.
 */
class WidgetConformanceTest {
    @Test
    fun everyPublicWidgetHasAConformanceCase() {
        val factories = factories()
        val publicWidgets = publicConcreteWidgetTypes().mapNotNull { it.qualifiedName }.toSet()
        assertEquals(
            publicWidgets,
            factories.keys,
            "public widget registry is incomplete or contains a stale entry",
        )
        for ((name, factory) in factories.toSortedMap()) {
            val widget = factory()
            val harness = GuiTestHarness(widget, columns = 40, rows = 8)
            try {
                harness.paint()
                assertTrue(harness.rows().isNotEmpty(), "$name did not paint")
                harness.move(0, 0)
                harness.click(0, 0)
                harness.key(Key.TAB)
                harness.key(Key.ENTER)
                harness.scroll(0, 1)
                harness.advance(1)
                harness.paint()
            } catch (failure: Throwable) {
                throw AssertionError("conformance case failed for ${name.substringAfterLast('.')}", failure)
            }
        }
    }

    private fun factories(): Map<String, () -> Widget> =
        mapOf(
            key<Align>() to { Align(child = Label("x")) },
            key<AppShell>() to {
                AppShell(
                    menuBar = MenuBar(listOf(Menu("File", items = listOf(MenuItem.Separator)))),
                    content = Label("content"),
                    statusBar = StatusBar("ready"),
                )
            },
            key<Badge>() to { Badge("new") },
            key<Banner>() to { Banner("message") },
            key<Button>() to { Button("OK") },
            key<Center>() to { Center(Label("x")) },
            key<Checkbox>() to { Checkbox("check") },
            key<Column>() to { Column(children = listOf(Label("a"), Label("b"))) },
            key<ConfirmDialog>() to { ConfirmDialog("Confirm", "Proceed?") },
            key<ConstrainedBox>() to { ConstrainedBox(minWidth = 1, maxWidth = 10, child = Label("x")) },
            key<ContextMenu>() to { ContextMenu(listOf(MenuItem.CommandItem(Command("open") {}))) },
            key<Dialog>() to {
                Dialog(
                    title = "Dialog",
                    body = Label("body"),
                    actions = listOf(DialogAction("OK", default = true)),
                )
            },
            key<Expanded>() to { Expanded(child = Label("x")) },
            key<Flexible>() to { Flexible(child = Label("x")) },
            key<Form>() to { Form(listOf(FormField("Name", TextField("x")))) },
            key<FormField>() to { FormField("Name", TextField("x")) },
            key<FractionallySizedBox>() to { FractionallySizedBox(widthFactor = 0.5f, child = Label("x")) },
            key<Grid>() to { Grid(columns = listOf(GridTrack.Fixed(2)), children = listOf(Label("x"))) },
            key<Icon>() to { Icon(Shape.CHECK) },
            key<KeyHint>() to { KeyHint("Ctrl+S", "Save") },
            key<Label>() to { Label("label") },
            key<ListView<String>>() to {
                ListView<String>(
                    items = listOf("alpha"),
                    itemBuilder = { _, item -> Label(item) },
                )
            },
            key<LogView>() to { LogView(listOf("line")) },
            key<MenuBar>() to { MenuBar(listOf(Menu("File", items = listOf(MenuItem.Separator)))) },
            key<MenuPopup>() to { MenuPopup(listOf(MenuItem.CommandItem(Command("open") {}))) },
            key<Padding>() to { Padding(1, Label("x")) },
            key<Panel>() to { Panel(child = Label("x")) },
            key<Popover>() to { Popover(Label("popover")) },
            key<Positioned>() to { Positioned(left = Length.Cells(0), child = Label("x")) },
            key<ProgressBar>() to { ProgressBar(0.5) },
            key<PromptDialog>() to { PromptDialog("Prompt", "Value") },
            key<RadioButton>() to { RadioButton("a", "A") },
            key<RadioGroup>() to { RadioGroup(children = listOf(RadioButton("a", "A"))) },
            key<RichLabel>() to { RichLabel(spans = listOf(TextSpan("rich"))) },
            key<Row>() to { Row(children = listOf(Label("a"), Label("b"))) },
            key<ScreenHost>() to { ScreenHost(listOf(Screen("a", "A", Label("screen"))), "a") },
            key<ScrollBar>() to {
                ScrollBar(ScrollModel(contentWidth = 1, contentHeight = 5, viewportWidth = 1, viewportHeight = 2))
            },
            key<ScrollView>() to { ScrollView(Label("x")) },
            key<SearchField>() to { SearchField(placeholder = "search") },
            key<Separator>() to { Separator() },
            key<SizedBox>() to { SizedBox(width = 2, height = 1, child = Label("x")) },
            key<Spacer>() to { Spacer() },
            key<Spinner>() to { Spinner() },
            key<SplitPane>() to { SplitPane(SplitOrientation.HORIZONTAL, Label("a"), Label("b")) },
            key<Stack>() to { Stack(children = listOf(Label("a"), Label("b"))) },
            key<StatusBar>() to { StatusBar("left") },
            key<Switch>() to { Switch("switch") },
            key<TabBar>() to { TabBar(listOf(Tab("a", "A", Label("a")))) },
            key<TabView>() to { TabView(listOf(Tab("a", "A", Label("a")))) },
            key<TableView<String>>() to {
                TableView<String>(
                    items = listOf("alpha"),
                    columns = listOf(TableColumn("Name", cell = { TableCell(it) })),
                )
            },
            key<TextArea>() to { TextArea() },
            key<TextField>() to { TextField() },
            key<Toast>() to { Toast("toast") },
            key<Toolbar>() to { Toolbar(Button("OK")) },
            key<Tooltip>() to { Tooltip("tip") },
            key<TreeView<String>>() to { TreeView(nodes = listOf(TreeNode("a", "A"))) },
            key<Wrap>() to { Wrap(children = listOf(Label("a"), Label("b"))) },
        )

    private inline fun <reified T : Widget> key(): String = T::class.qualifiedName!!

    @Suppress("UNCHECKED_CAST")
    private fun publicConcreteWidgetTypes(): List<KClass<out Widget>> {
        val root = classRoot()
        val paths = Files.walk(root)
        val names =
            try {
                paths
                    .filter { it.name.endsWith(".class") }
                    .map {
                        root
                            .relativize(it)
                            .toString()
                            .removeSuffix(".class")
                            .replace('\\', '.')
                            .replace('/', '.')
                    }.toList()
            } finally {
                paths.close()
            }
        return names
            .filter { it.substringAfterLast('.').isNotEmpty() }
            .filter { !it.contains(".internal.") }
            .filter { !it.substringAfterLast('.').startsWith("WhenMappings") }
            .mapNotNull { name ->
                val clazz = Class.forName(name, false, javaClass.classLoader)
                if (clazz.isSynthetic || clazz.isAnonymousClass || clazz.isLocalClass) {
                    null
                } else {
                    clazz.kotlin
                }
            }.filter { type ->
                type.visibility == KVisibility.PUBLIC &&
                    !type.isCompanion &&
                    !type.isAbstract &&
                    Widget::class.java.isAssignableFrom(type.java)
            }.map { it as KClass<out Widget> }
            .sortedBy { it.qualifiedName }
    }

    private fun classRoot(): Path =
        Path.of(
            Widget::class.java.protectionDomain.codeSource.location
                .toURI(),
        )
}
