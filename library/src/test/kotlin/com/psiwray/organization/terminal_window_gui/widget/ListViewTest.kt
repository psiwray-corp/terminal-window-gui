package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListViewTest {
    private fun view(
        items: List<String> = listOf("alpha", "beta", "gamma", "delta"),
        mode: SelectionMode = SelectionMode.SINGLE,
        activated: MutableList<String> = ArrayList(),
    ): ListView<String> =
        ListView(
            items = items,
            itemBuilder = { _, item -> Label(item) },
            keyOf = { it },
            selectionMode = mode,
            onActivate = { activated.add(it) },
            emptyText = "empty",
        )

    @Test
    fun keyboardMovesSelectionAndKeepsCursorVisible() {
        val list = view()
        val harness = GuiTestHarness(list, columns = 8, rows = 2)
        harness.paint()
        harness.surface.focus.focus(list)
        assertTrue(harness.key(Key.DOWN))
        assertEquals("alpha", list.selection.cursor)
        assertTrue(harness.key(Key.DOWN))
        assertEquals("beta", list.selection.cursor)
        assertTrue(harness.key(Key.END))
        assertEquals("delta", list.selection.cursor)
        assertTrue(list.scroll.offsetY > 0)
        assertTrue(harness.key(Key.HOME))
        assertEquals("alpha", list.selection.cursor)
    }

    @Test
    fun selectedRowBackgroundShowsThroughLabels() {
        val list = view()
        val harness = GuiTestHarness(list, columns = 8, rows = 2)
        harness.paint()
        harness.surface.focus.focus(list)
        list.selection.select("alpha")
        harness.paint()
        assertEquals(harness.surface.theme.selectionBackground, harness.matrix.backgroundAt(0, 0))
        assertEquals(harness.surface.theme.selectionBackground, harness.matrix.backgroundAt(4, 0))
    }

    @Test
    fun shiftAndControlSelectionWork() {
        val activated = ArrayList<String>()
        val list = view(mode = SelectionMode.MULTIPLE, activated = activated)
        val harness = GuiTestHarness(list, columns = 8, rows = 4)
        harness.paint()
        harness.surface.focus.focus(list)
        harness.key(Key.DOWN)
        harness.key(Key.DOWN)
        harness.key(Key.DOWN, Modifiers.SHIFT)
        assertEquals(setOf("beta", "gamma"), list.selection.selectedKeys)
        harness.key(Key.A, Modifiers.CONTROL)
        assertEquals(4, list.selection.selectedKeys.size)
        harness.key(Key.ENTER)
        assertEquals(listOf("gamma"), activated)
    }

    @Test
    fun controlClickTogglesMultipleSelection() {
        val list = view(mode = SelectionMode.MULTIPLE)
        val harness = GuiTestHarness(list, columns = 8, rows = 4)
        harness.paint()
        harness.click(1, 0)
        harness.click(1, 1, modifiers = Modifiers.CONTROL)
        assertEquals(setOf("alpha", "beta"), list.selection.selectedKeys)
        harness.click(1, 1, modifiers = Modifiers.CONTROL)
        assertEquals(setOf("alpha"), list.selection.selectedKeys)
    }

    @Test
    fun clickDoubleClickAndWheelWork() {
        val activated = ArrayList<String>()
        val list = view(activated = activated)
        val harness = GuiTestHarness(list, columns = 8, rows = 2)
        harness.paint()
        assertTrue(harness.click(1, 0))
        assertEquals("alpha", list.selection.cursor)
        harness.click(1, 0)
        assertEquals(listOf("alpha"), activated)
        harness.scroll(0, 1)
        assertTrue(list.scroll.offsetY > 0)
        harness.paint()
        assertTrue(harness.rows().any { it.contains("beta") || it.contains("gamma") })
    }

    @Test
    fun typeAheadWrapsAndTimesOut() {
        val list = view(items = listOf("alpha", "beta", "gamma"))
        val harness = GuiTestHarness(list, columns = 8, rows = 2)
        harness.paint()
        harness.surface.focus.focus(list)
        assertTrue(harness.char('g'))
        assertEquals("gamma", list.selection.cursor)
        harness.advance(600)
        assertTrue(harness.char('a'))
        assertEquals("alpha", list.selection.cursor)
        harness.advance(600)
        assertTrue(harness.char('b'))
        assertEquals("beta", list.selection.cursor)
    }

    @Test
    fun directScrollOffsetResynchronizesRows() {
        val list = view(items = (0 until 30).map { "Item $it" })
        val harness = GuiTestHarness(list, columns = 12, rows = 3)
        harness.paint()
        list.scroll.scrollTo(0, 5)
        harness.paint()
        assertTrue(harness.rows().any { it.contains("Item 5") })
    }

    @Test
    fun emptyAndOneCellRender() {
        val empty = view(items = emptyList())
        val harness = GuiTestHarness(empty, columns = 8, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("empty"))
        assertTrue(empty.focusable)
        val tiny = view(items = listOf("x"))
        val tinyHarness = GuiTestHarness(tiny, columns = 1, rows = 1)
        tinyHarness.paint()
        assertTrue(tinyHarness.click(0, 0))
    }

    @Test
    fun selectionSurvivesStableKeyReorder() {
        val list = view()
        val harness = GuiTestHarness(list, columns = 8, rows = 4)
        harness.paint()
        list.selection.select("gamma")
        harness.paint()
        list.items = listOf("delta", "gamma", "beta", "alpha")
        harness.paint()
        assertTrue(list.selection.isSelected("gamma"))
        assertEquals("gamma", list.selection.cursor)
        assertTrue(harness.rows().any { it.contains("gamma") })
    }
}
