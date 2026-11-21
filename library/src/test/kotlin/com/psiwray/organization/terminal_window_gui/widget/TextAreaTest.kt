package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextAreaTest {
    @Test
    fun wrappingMapsCaretAndRendersLineNumbers() {
        val controller = TextEditingController("one two three")
        val area =
            TextArea(controller, wrap = TextAreaWrap.WORD, lineNumbers = LineNumbers(enabled = true, minDigits = 2))
        val harness = GuiTestHarness(area, columns = 10, rows = 4)
        harness.paint()
        harness.surface.focus.focus(area)
        assertTrue(harness.rows().any { it.contains("one two") })
        assertTrue(harness.rows().any { it.contains("three") })
        assertTrue(harness.rows().first().startsWith(" 1 "))
        controller.textEnd()
        harness.paint()
        assertTrue(area.scroll.offsetY >= 0)
    }

    @Test
    fun charWrapAndEnterInsertNewline() {
        val controller = TextEditingController()
        val area = TextArea(controller, wrap = TextAreaWrap.CHAR)
        val harness = GuiTestHarness(area, columns = 4, rows = 4)
        harness.paint()
        harness.surface.focus.focus(area)
        harness.char('a')
        harness.char('b')
        harness.key(Key.ENTER)
        harness.char('c')
        assertEquals("ab\nc", controller.text)
        assertEquals(4, controller.caret)
    }

    @Test
    fun verticalMovementUsesVisualRows() {
        val controller = TextEditingController("abcdefgh")
        val area = TextArea(controller, wrap = TextAreaWrap.CHAR)
        val harness = GuiTestHarness(area, columns = 3, rows = 4)
        harness.paint()
        harness.surface.focus.focus(area)
        controller.textEnd()
        harness.key(Key.UP)
        assertEquals(5, controller.caret)
    }

    @Test
    fun readOnlyRejectsEditsButScrolls() {
        val controller = TextEditingController("a\nb\nc\nd")
        val area = TextArea(controller, readOnly = true)
        val harness = GuiTestHarness(area, columns = 4, rows = 2)
        harness.paint()
        harness.surface.focus.focus(area)
        harness.char('x')
        harness.key(Key.BACKSPACE)
        assertEquals("a\nb\nc\nd", controller.text)
        assertTrue(harness.scroll(0, -1))
        assertTrue(area.scroll.offsetY < 2)
    }

    @Test
    fun noWrapScrollsHorizontally() {
        val area = TextArea(TextEditingController("abcdefghij"), wrap = TextAreaWrap.NONE)
        val harness = GuiTestHarness(area, columns = 4, rows = 2)
        harness.paint()
        assertTrue(harness.scroll(-1, 0))
        assertTrue(area.scroll.offsetX < 6)
    }

    @Test
    fun tripleClickSelectsLogicalLine() {
        val controller = TextEditingController("one two\nthree")
        val area = TextArea(controller)
        val harness = GuiTestHarness(area, columns = 10, rows = 2)
        harness.paint()
        harness.click(1, 0)
        harness.click(1, 0)
        harness.click(1, 0)
        assertEquals("one two", controller.selectedText)
    }

    @Test
    fun resizeRelayoutsWrappedRows() {
        val controller = TextEditingController("one two three")
        val area = TextArea(controller, wrap = TextAreaWrap.WORD)
        val harness = GuiTestHarness(area, columns = 10, rows = 3)
        harness.paint()
        assertTrue(harness.rows()[0].contains("one two"))
        harness.resize(5, 3)
        harness.paint()
        assertFalse(harness.rows()[0].contains("one two"))
        assertTrue(harness.rows().any { it.contains("one") })
    }

    @Test
    fun initialTextAndBooleanLineNumberConstructorsCompile() {
        val plain = TextArea("plain")
        assertEquals("plain", plain.controller.text)
        val area = TextArea("hello", lineNumbers = true)
        assertEquals("hello", area.controller.text)
        assertTrue(area.lineNumbers.enabled)
    }

    @Test
    fun mouseDoubleClickSelectsWord() {
        val controller = TextEditingController("one two")
        val area = TextArea(controller)
        val harness = GuiTestHarness(area, columns = 8, rows = 2)
        harness.paint()
        harness.click(1, 0)
        harness.click(1, 0)
        assertEquals("one", controller.selectedText)
        assertFalse(controller.selection.isCollapsed)
    }
}
