package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableViewTest {
    private val items = listOf("alpha", "beta", "gamma", "delta")

    private fun table(
        items: List<String> = this.items,
        onHeader: (Int) -> Unit = {},
        activated: MutableList<String> = ArrayList(),
    ): TableView<String> =
        TableView(
            items = items,
            columns =
                listOf(
                    TableColumn("Name", ColumnWidth.Fixed(5)) { TableCell(it) },
                    TableColumn("Value", ColumnWidth.Flex(1), TextAlignment.RIGHT) {
                        TableCell(it.uppercase())
                    },
                ),
            keyOf = { it },
            selectionMode = SelectionMode.MULTIPLE,
            onHeaderClick = onHeader,
            onActivate = { activated.add(it) },
            emptyText = "none",
        )

    @Test
    fun headerRowsCellsAndSortingIndicatorRender() {
        val table = table()
        table.sortColumn = 0
        table.sortDirection = SortDirection.ASCENDING
        val harness = GuiTestHarness(table, columns = 14, rows = 4)
        harness.paint()
        val rows = harness.rows()
        assertTrue(rows[0].contains("Name"), rows.joinToString("\n"))
        assertTrue(rows[0].contains("Value"), rows.joinToString("\n"))
        assertTrue(rows[0].contains("↑") || rows[0].contains("^"), rows.joinToString("\n"))
        assertTrue(rows[1].contains("alpha"), rows.joinToString("\n"))
        assertTrue(rows[1].contains("ALPHA"), rows.joinToString("\n"))
    }

    @Test
    fun fixedFlexAndFractionWidthsPlaceHeaders() {
        val table =
            TableView(
                items = listOf("x"),
                columns =
                    listOf(
                        TableColumn("A", ColumnWidth.Fixed(5)) { TableCell("a") },
                        TableColumn("B", ColumnWidth.Flex(1)) { TableCell("b") },
                        TableColumn("C", ColumnWidth.Fraction(0.25)) { TableCell("c") },
                    ),
            )
        val harness = GuiTestHarness(table, columns = 20, rows = 2)
        harness.paint()
        val header = harness.rows()[0]
        assertEquals(0, header.indexOf('A'))
        assertEquals(5, header.indexOf('B'))
        assertEquals(15, header.indexOf('C'))
    }

    @Test
    fun headerClickReportsIndexWithoutReordering() {
        val calls = ArrayList<Int>()
        val table = table(onHeader = { calls.add(it) })
        val harness = GuiTestHarness(table, columns = 14, rows = 4)
        harness.paint()
        assertTrue(harness.click(1, 0))
        assertEquals(listOf(0), calls)
        assertEquals(items, table.items)
    }

    @Test
    fun selectionKeyboardAndActivationWork() {
        val activated = ArrayList<String>()
        val table = table(activated = activated)
        val harness = GuiTestHarness(table, columns = 14, rows = 3)
        harness.paint()
        harness.surface.focus.focus(table)
        harness.key(Key.DOWN)
        assertEquals("alpha", table.selection.cursor)
        harness.key(Key.DOWN, com.psiwray.organization.terminal_window.input.Modifiers.SHIFT)
        assertEquals(setOf("alpha", "beta"), table.selection.selectedKeys)
        harness.key(Key.END)
        assertEquals("delta", table.selection.cursor)
        harness.key(Key.ENTER)
        assertEquals(listOf("delta"), activated)
    }

    @Test
    fun focusedRowDoesNotOverwriteTheLastCell() {
        val table = table()
        val harness = GuiTestHarness(table, columns = 14, rows = 5)
        harness.paint()
        harness.surface.focus.focus(table)
        harness.key(Key.DOWN)
        harness.paint()
        assertTrue(harness.rows()[1].endsWith("ALPHA"), harness.rows()[1])
    }

    @Test
    fun clickRowSelectsAndDoubleClickActivates() {
        val activated = ArrayList<String>()
        val table = table(activated = activated)
        val harness = GuiTestHarness(table, columns = 14, rows = 4)
        harness.paint()
        assertTrue(harness.click(1, 2))
        assertEquals("beta", table.selection.cursor)
        harness.click(1, 2)
        assertEquals(listOf("beta"), activated)
    }

    @Test
    fun narrowFlexHeadersKeepTheirMinimumAndScrollHorizontally() {
        val table =
            TableView(
                items = listOf("a"),
                columns =
                    listOf(
                        TableColumn("Alpha", ColumnWidth.Flex(1)) { TableCell("a") },
                        TableColumn("Beta", ColumnWidth.Flex(1)) { TableCell("b") },
                        TableColumn("Gamma", ColumnWidth.Flex(1)) { TableCell("c") },
                    ),
            )
        val harness = GuiTestHarness(table, columns = 10, rows = 3)
        harness.paint()
        assertTrue(table.horizontalScrollBar.shown)
        table.scroll.scrollTo(table.scroll.maxOffsetX, 0)
        harness.paint()
        assertTrue(harness.rows()[0].contains("Gamma"))
    }

    @Test
    fun horizontalScrollMovesHeaderAndBody() {
        val table =
            TableView(
                items = listOf("abcdef", "ghijkl"),
                columns =
                    listOf(
                        TableColumn("First", ColumnWidth.Fixed(6)) { TableCell(it) },
                        TableColumn("Second", ColumnWidth.Fixed(6)) { TableCell(it) },
                    ),
            )
        val harness = GuiTestHarness(table, columns = 8, rows = 3)
        harness.paint()
        val before = harness.rows()[0]
        assertTrue(harness.scroll(1, 0, 1, 1))
        harness.paint()
        assertTrue(table.scroll.offsetX > 0)
        assertTrue(harness.rows()[0] != before)
    }

    @Test
    fun emptyAndOneCellRender() {
        val empty = table(items = emptyList())
        val harness = GuiTestHarness(empty, columns = 8, rows = 2)
        harness.paint()
        assertTrue(harness.rows()[1].contains("none"))
        val tiny = table(items = listOf("x"))
        val tinyHarness = GuiTestHarness(tiny, columns = 1, rows = 1)
        tinyHarness.paint()
        assertTrue(tinyHarness.rows().size == 1)
    }
}
