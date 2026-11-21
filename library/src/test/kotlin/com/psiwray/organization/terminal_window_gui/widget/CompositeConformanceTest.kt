package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.FocusTraversal
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositeConformanceTest {
    @Test
    fun compositesPaintAtMatrixSizes() {
        for ((columns, rows) in listOf(1 to 1, 80 to 24, 240 to 67)) {
            for (factory in factories()) {
                val widget = factory()
                val harness = GuiTestHarness(widget, columns = columns, rows = rows)
                harness.paint()
                val structure = harness.rows()
                assertEquals(rows, structure.size)
                assertTrue(structure.all { it.length == columns })
                assertInside(widget)
                FocusTraversal.order(widget)
            }
        }
    }

    @Test
    fun compositeOneCellGoldensAreStable() {
        for ((name, widget) in factories().mapIndexed { index, factory -> index.toString() to factory() }) {
            val harness = GuiTestHarness(widget, columns = 1, rows = 1)
            harness.paint()
            assertEquals(1, harness.rows().size)
            assertEquals(1, harness.rows()[0].length)
            assertTrue(name.isNotEmpty())
        }
    }

    private fun factories(): List<() -> Widget> =
        listOf(
            {
                TabView(
                    listOf(
                        Tab("a", "A", Label("alpha")),
                        Tab("b", "B", Label("beta")),
                        Tab("c", "C", Label("gamma")),
                    ),
                )
            },
            { SplitPane(SplitOrientation.HORIZONTAL, Label("left"), Label("right")) },
            {
                Form(
                    listOf(
                        FormField("Name", TextField("Ada"), helpText = "help"),
                        FormField("Port", TextField("8080"), errorText = "invalid"),
                    ),
                    labelWidth = FormLabelWidth.Fixed(8),
                )
            },
            { StatusBar("left", "center", "right") },
            { AppShell(Label("menu"), Label("content"), Label("status")) },
            {
                ScreenHost(
                    listOf(
                        Screen("a", "A", Label("alpha")),
                        Screen("b", "B", Label("beta")),
                    ),
                    "a",
                )
            },
        )

    private fun assertInside(widget: Widget) {
        val parent = widget.parent
        if (parent is ContainerWidget) {
            assertTrue(widget.bounds.x >= 0, "${widget::class.simpleName} x")
            assertTrue(widget.bounds.y >= 0, "${widget::class.simpleName} y")
            assertTrue(widget.bounds.right <= parent.contentRect.width, "${widget::class.simpleName} right")
            assertTrue(widget.bounds.bottom <= parent.contentRect.height, "${widget::class.simpleName} bottom")
        }
        for (child in widget.traversalChildren()) {
            assertInside(child)
        }
    }
}
