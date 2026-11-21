package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test

class CollectionGoldenTest {
    @Test
    fun listViewGolden() {
        val items = (0 until 30).map { "Item $it" }
        val list = ListView(items = items, itemBuilder = { _, item -> Label(item) }, keyOf = { it })
        val harness = GuiTestHarness(list, columns = 80, rows = 24)
        harness.paint()
        GoldenAssert.assertRows("list-view-80x24.txt", harness.rows())
    }

    @Test
    fun tableViewGolden() {
        val items = (0 until 30).map { "row $it" }
        val table =
            TableView(
                items = items,
                columns =
                    listOf(
                        TableColumn("Id", ColumnWidth.Fixed(6)) { TableCell(it.substringAfter(' ')) },
                        TableColumn("Name", ColumnWidth.Flex(1)) { TableCell(it) },
                    ),
                keyOf = { it },
                sortColumn = 1,
                sortDirection = SortDirection.DESCENDING,
            )
        val harness = GuiTestHarness(table, columns = 80, rows = 24)
        harness.paint()
        GoldenAssert.assertRows("table-view-80x24.txt", harness.rows())
    }

    @Test
    fun treeViewGolden() {
        val children = (0 until 30).map { TreeNode("n$it", "Node $it") }
        val root = TreeNode("root", "Root", children = children)
        val tree = TreeView(nodes = listOf(root), expandedKeys = setOf("root"))
        val harness = GuiTestHarness(tree, columns = 80, rows = 24)
        harness.paint()
        GoldenAssert.assertRows("tree-view-80x24.txt", harness.rows())
    }

    @Test
    fun emptyCollectionsAtOneCellGolden() {
        val list = ListView<String>(items = emptyList(), itemBuilder = { _, item -> Label(item) })
        val listHarness = GuiTestHarness(list, columns = 1, rows = 1)
        listHarness.paint()
        GoldenAssert.assertRows("list-view-empty-1x1.txt", listHarness.rows())

        val table = TableView<String>(items = emptyList())
        val tableHarness = GuiTestHarness(table, columns = 1, rows = 1)
        tableHarness.paint()
        GoldenAssert.assertRows("table-view-empty-1x1.txt", tableHarness.rows())

        val tree = TreeView<String>(nodes = emptyList())
        val treeHarness = GuiTestHarness(tree, columns = 1, rows = 1)
        treeHarness.paint()
        GoldenAssert.assertRows("tree-view-empty-1x1.txt", treeHarness.rows())
    }
}
