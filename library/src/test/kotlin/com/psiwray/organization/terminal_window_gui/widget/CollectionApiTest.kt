package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionApiTest {
    @Test
    fun documentedConstructorsCompileAndWork() {
        val trailing = ListView(listOf("a", "b")) { _, item -> Label(item) }
        assertEquals(2, trailing.items.size)

        val named =
            ListView(
                items = listOf("a"),
                itemBuilder = { _, item -> Label(item) },
                keyOf = { it },
                selectionMode = SelectionMode.MULTIPLE,
                selection = SelectionModel(SelectionMode.MULTIPLE),
                onActivate = {},
                emptyText = "empty",
            )
        assertEquals(1, named.items.size)

        val fixed = TableColumn<String>("A", ColumnWidth.Fixed(5), TextAlignment.LEFT) { TableCell(it) }
        val flex = TableColumn<String>("B", 5) { TableCell("b") }
        val table =
            TableView(
                items = listOf("a"),
                columns = listOf(fixed, flex),
                keyOf = { it },
                onHeaderClick = {},
                sortColumn = 0,
                sortDirection = SortDirection.ASCENDING,
                onActivate = {},
                emptyText = "none",
            )
        assertEquals(2, table.columns.size)

        val expanded = mutableSetOf<Any>("root")
        val root = TreeNode("root", "Root", children = listOf(TreeNode("child", "Child")))
        val tree = TreeView(nodes = listOf(root), expandedKeys = expanded, selection = SelectionModel())
        assertEquals(1, tree.nodes.size)

        data class Node(
            val id: String,
            val name: String,
            val kids: List<Node>,
        )

        val custom =
            TreeView(
                nodes = listOf(Node("a", "A", emptyList())),
                keyOf = { it.id },
                label = { it.name },
                children = { it.kids },
                expandedKeys = setOf("a"),
            )
        assertEquals(1, custom.nodes.size)
        val tableTrailing = TableView(listOf("a"), listOf(fixed, flex)) { }
        assertEquals(1, tableTrailing.items.size)

        val treeTrailing = TreeView(listOf(TreeNode("a", "A"))) { }
        assertEquals(1, treeTrailing.nodes.size)

        val model = SelectionModel(SelectionMode.MULTIPLE, listOf("a", "b"))
        model.select("a")
        assertEquals(setOf("a"), model.selectedKeys)
    }
}
