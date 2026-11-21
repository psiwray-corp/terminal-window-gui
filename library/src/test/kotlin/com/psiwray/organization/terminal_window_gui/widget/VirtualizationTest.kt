package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertTrue

class VirtualizationTest {
    @Test
    fun listBuildsOnlyViewportRows() {
        var builds = 0
        val items = (0 until 10_000).map { "row $it" }
        val list =
            ListView(
                items = items,
                itemBuilder = { _, item ->
                    builds++
                    Label(item)
                },
                keyOf = { it },
            )
        val harness = GuiTestHarness(list, columns = 20, rows = 4)
        harness.paint()
        assertTrue(builds <= 6, "built $builds rows for a four-row viewport")
        harness.surface.focus.focus(list)
        harness.key(Key.END)
        harness.paint()
        assertTrue(list.scroll.offsetY > 0)
        assertTrue(harness.rows().any { it.contains("row 9999") })
    }

    @Test
    fun cursorStaysVisibleAfterLargeDataChangeAndResize() {
        val list =
            ListView(
                items = (0 until 100).map { "row $it" },
                itemBuilder = { _, item -> Label(item) },
                keyOf = { it },
            )
        val harness = GuiTestHarness(list, columns = 16, rows = 3)
        harness.paint()
        list.selection.select("row 99")
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
        harness.resize(12, 2)
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
        list.items = (0 until 100).reversed().map { "row $it" }
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
    }

    @Test
    fun tableCursorStaysVisibleAfterMoveAndResize() {
        val table =
            TableView(
                items = (0 until 100).map { "row $it" },
                columns = listOf(TableColumn("Value", ColumnWidth.Flex(1)) { TableCell(it) }),
                keyOf = { it },
            )
        val harness = GuiTestHarness(table, columns = 20, rows = 3)
        harness.paint()
        table.selection.select("row 99")
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
        table.items = (0 until 100).reversed().map { "row $it" }
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
        harness.resize(16, 2)
        harness.paint()
        assertTrue(harness.rows().any { it.contains("row 99") })
    }

    @Test
    fun treeCallsLabelOnlyForVisibleRows() {
        var labels = 0
        val children = (0 until 10_000).map { TreeNode("n$it", "node $it") }
        val tree =
            TreeView(
                nodes = listOf(TreeNode("root", "root", children = children)),
                expandedKeys = setOf("root"),
                label = {
                    labels++
                    it.label
                },
            )
        val harness = GuiTestHarness(tree, columns = 20, rows = 4)
        harness.paint()
        assertTrue(labels <= 8, "label accessor called $labels times for four rows")
    }

    @Test
    fun largeTableAndTreePaintQuicklyWithoutRowWidgets() {
        val table =
            TableView(
                items = (0 until 10_000).map { "row $it" },
                columns = listOf(TableColumn("Value", ColumnWidth.Flex(1)) { TableCell(it) }),
            )
        val tableHarness = GuiTestHarness(table, columns = 20, rows = 4)
        tableHarness.paint()
        table.selection.select(9999)
        tableHarness.paint()
        assertTrue(tableHarness.rows().any { it.contains("row 9999") })

        val children = (0 until 10_000).map { TreeNode("n$it", "node $it") }
        val tree = TreeView(nodes = listOf(TreeNode("root", "root", children = children)), expandedKeys = setOf("root"))
        val treeHarness = GuiTestHarness(tree, columns = 20, rows = 4)
        treeHarness.paint()
        tree.selection.select("n9999")
        treeHarness.paint()
        assertTrue(treeHarness.rows().any { it.contains("node 9999") })
    }
}
