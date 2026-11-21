package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreeViewTest {
    private fun nodes(): List<TreeNode<String>> =
        listOf(
            TreeNode(
                key = "root",
                label = "Root",
                children =
                    listOf(
                        TreeNode(
                            key = "alpha",
                            label = "Alpha",
                            children = listOf(TreeNode("alpha-1", "Alpha One")),
                        ),
                        TreeNode("beta", "Beta"),
                    ),
            ),
        )

    @Test
    fun expandedTreePaintsFlattenedRowsAndIndentation() {
        val tree = TreeView(nodes = nodes(), expandedKeys = setOf("root"))
        val harness = GuiTestHarness(tree, columns = 20, rows = 4)
        harness.paint()
        val rows = harness.rows()
        assertTrue(rows[0].contains("Root"))
        assertTrue(rows[1].contains("Alpha"))
        assertTrue(rows[2].contains("Beta"))
        assertTrue(
            rows.indices.any { rows[it].contains("Alpha") && rows[it].indexOf("Alpha") > rows[0].indexOf("Root") },
        )
        assertFalse(harness.rows().any { it.contains("Alpha One") })
    }

    @Test
    fun rightAndLeftNavigateExpansionAndParent() {
        val tree = TreeView(nodes = nodes(), expandedKeys = setOf("root"))
        val harness = GuiTestHarness(tree, columns = 20, rows = 4)
        harness.paint()
        tree.selection.select("root")
        harness.surface.focus.focus(tree)
        assertTrue(harness.key(Key.RIGHT))
        assertEquals("alpha", tree.selection.cursor)
        assertFalse(tree.isExpanded("alpha"))
        assertTrue(harness.key(Key.RIGHT))
        assertTrue(tree.isExpanded("alpha"))
        harness.paint()
        assertTrue(harness.rows().any { it.contains("Alpha One") })
        assertTrue(harness.key(Key.LEFT))
        assertEquals("alpha", tree.selection.cursor)
        assertTrue(harness.key(Key.LEFT))
        assertEquals("root", tree.selection.cursor)
    }

    @Test
    fun spaceAndExpanderClickToggle() {
        val tree = TreeView(nodes = nodes(), expandedKeys = setOf("root"))
        val harness = GuiTestHarness(tree, columns = 20, rows = 4)
        harness.paint()
        tree.selection.select("root")
        harness.surface.focus.focus(tree)
        assertTrue(harness.key(Key.SPACE))
        assertFalse(tree.isExpanded("root"))
        harness.paint()
        assertFalse(harness.rows().any { it.contains("Alpha") })
        assertTrue(harness.click(0, 0))
        assertTrue(tree.isExpanded("root"))
        assertEquals("root", tree.selection.cursor)
    }

    @Test
    fun activationAndTypeAheadWork() {
        val activated = ArrayList<String>()
        val tree = TreeView(nodes = nodes(), expandedKeys = setOf("root"), onActivate = { activated.add(it.label) })
        val harness = GuiTestHarness(tree, columns = 20, rows = 4)
        harness.paint()
        harness.surface.focus.focus(tree)
        assertTrue(harness.char('b'))
        assertEquals("beta", tree.selection.cursor)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(listOf("Beta"), activated)
        harness.click(4, 0)
        harness.click(4, 0)
        assertTrue(activated.contains("Root"))
    }

    @Test
    fun customAccessorsFlattenArbitraryNodes() {
        data class Node(
            val id: String,
            val name: String,
            val kids: List<Node>,
        )

        val root = Node("r", "root", listOf(Node("c", "child", emptyList())))
        val tree =
            TreeView(
                nodes = listOf(root),
                keyOf = { it.id },
                label = { it.name },
                children = { it.kids },
                expandedKeys = setOf("r"),
            )
        val harness = GuiTestHarness(tree, columns = 12, rows = 2)
        harness.paint()
        assertTrue(harness.rows()[1].contains("child"))
    }

    @Test
    fun emptyAndOneCellRender() {
        val tree = TreeView<String>(nodes = emptyList(), emptyText = "none")
        val harness = GuiTestHarness(tree, columns = 8, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("none"))
        val tiny = TreeView(listOf(TreeNode("a", "A")), expandedKeys = setOf("a"))
        val tinyHarness = GuiTestHarness(tiny, columns = 1, rows = 1)
        tinyHarness.paint()
        assertTrue(tinyHarness.rows().size == 1)
    }

    @Test
    fun selectionAndScrollKeepCursorVisible() {
        val children = (0 until 30).map { TreeNode("n$it", "Node $it") }
        val root = TreeNode("root", "Root", children = children)
        val tree = TreeView(nodes = listOf(root), expandedKeys = setOf("root"))
        val harness = GuiTestHarness(tree, columns = 16, rows = 3)
        harness.paint()
        tree.selection.select("n29")
        harness.paint()
        assertTrue(tree.scroll.offsetY > 0)
        assertTrue(harness.rows().any { it.contains("Node 29") })
    }
}
