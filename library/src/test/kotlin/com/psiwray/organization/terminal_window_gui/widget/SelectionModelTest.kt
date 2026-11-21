package com.psiwray.organization.terminal_window_gui.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectionModelTest {
    @Test
    fun singleModeKeepsCursorSelected() {
        val model = SelectionModel(SelectionMode.SINGLE)
        model.keys = listOf("a", "b", "c")
        model.select("b")
        assertEquals(setOf("b"), model.selectedKeys)
        assertEquals("b", model.cursor)
        model.setCursor("c")
        assertEquals(setOf("c"), model.selectedKeys)
        model.toggle("a")
        assertEquals(setOf("a"), model.selectedKeys)
    }

    @Test
    fun multipleModeTogglesAndSelectsRanges() {
        val model = SelectionModel(SelectionMode.MULTIPLE)
        model.keys = listOf("a", "b", "c", "d", "e")
        model.select("b")
        model.toggle("d")
        assertEquals(setOf("b", "d"), model.selectedKeys)
        model.select("e", range = true)
        assertEquals(setOf("d", "e"), model.selectedKeys)
        model.select("a", additive = true)
        assertEquals(setOf("d", "e", "a"), model.selectedKeys)
        model.clear()
        assertTrue(model.selectedKeys.isEmpty())
    }

    @Test
    fun noneModeTracksOnlyCursor() {
        val model = SelectionModel(SelectionMode.NONE)
        model.keys = listOf("a", "b")
        model.select("a")
        model.toggle("b")
        assertTrue(model.selectedKeys.isEmpty())
        assertEquals("b", model.cursor)
    }

    @Test
    fun cursorMovementClampsAndExtends() {
        val model = SelectionModel(SelectionMode.MULTIPLE)
        model.keys = listOf("a", "b", "c")
        model.select("a")
        assertTrue(model.moveCursor(2))
        assertEquals("c", model.cursor)
        assertFalse(model.moveCursor(1))
        assertEquals("c", model.cursor)
        model.setCursor("a")
        assertTrue(model.moveCursor(2, extend = true))
        assertEquals(setOf("a", "b", "c"), model.selectedKeys)
    }

    @Test
    fun stableKeysSurviveReorderingAndRetention() {
        val model = SelectionModel(SelectionMode.MULTIPLE)
        model.keys = listOf("a", "b", "c")
        model.select("b")
        model.keys = listOf("c", "b", "a")
        assertEquals(setOf("b"), model.selectedKeys)
        model.retain(listOf("b", "c"))
        assertEquals(setOf("b"), model.selectedKeys)
        model.retain(listOf("c"))
        assertTrue(model.selectedKeys.isEmpty())
        assertEquals("c", model.cursor)
    }

    @Test
    fun selectAllUsesOrderAndNormalizesSingleMode() {
        val model = SelectionModel(SelectionMode.MULTIPLE)
        model.selectAll(listOf("a", "b", "c"))
        assertEquals(setOf("a", "b", "c"), model.selectedKeys)
        model.mode = SelectionMode.SINGLE
        assertEquals(1, model.selectedKeys.size)
        assertEquals(model.cursor, model.selectedKeys.first())
    }
}
