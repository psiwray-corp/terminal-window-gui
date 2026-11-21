package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.input.InMemoryClipboard
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFieldTest {
    @Test
    fun typingNavigationAndSubmitWork() {
        val controller = TextEditingController()
        val field = TextField(controller)
        var submitted = ""
        field.onSubmit = { submitted = it }
        val harness = GuiTestHarness(field, columns = 8, rows = 1)
        harness.paint()
        assertTrue(harness.surface.focus.focus(field))
        harness.char('a')
        harness.char('b')
        harness.key(Key.LEFT)
        harness.char('X')
        assertEquals("aXb", controller.text)
        assertEquals(2, controller.caret)
        assertTrue(harness.key(Key.ENTER))
        assertEquals("aXb", submitted)
    }

    @Test
    fun horizontalScrollFollowsCaret() {
        val controller = TextEditingController("abcdef")
        val field = TextField(controller)
        val harness = GuiTestHarness(field, columns = 4, rows = 1)
        harness.paint()
        harness.surface.focus.focus(field)
        controller.textEnd()
        harness.paint()
        assertEquals(6, controller.caret)
        assertEquals(3, field.scrollOffset)
        assertTrue(harness.rows()[0].contains("def"))
    }

    @Test
    fun maskHidesTextAndDisablesCopy() {
        val clipboard = InMemoryClipboard()
        val controller = TextEditingController("secret")
        val field = TextField(controller, mask = '*')
        val harness = GuiTestHarness(field, columns = 8, rows = 1, clipboard = clipboard)
        harness.paint()
        harness.surface.focus.focus(field)
        controller.selectAll()
        harness.paint()
        assertTrue(harness.rows()[0].startsWith("******"))
        assertTrue(harness.key(Key.C, Modifiers.CONTROL))
        assertEquals(null, clipboard.getText())
    }

    @Test
    fun readOnlyAllowsFocusAndSelectionButRejectsEdits() {
        val controller = TextEditingController("abc")
        val field = TextField(controller, readOnly = true)
        val harness = GuiTestHarness(field, columns = 8, rows = 1)
        harness.paint()
        assertFalse(controller.text != "abc")
        assertTrue(harness.surface.focus.focus(field))
        harness.char('x')
        assertEquals("abc", controller.text)
        harness.drag(0, 0, 1, 0)
        assertFalse(controller.selection.isCollapsed)
        harness.key(Key.LEFT)
        assertTrue(controller.selection.isCollapsed)
    }

    @Test
    fun placeholderShowsOnlyWhenUnfocused() {
        val field = TextField(placeholder = "Search")
        val harness = GuiTestHarness(field, columns = 10, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("Search"))
        harness.surface.focus.focus(field)
        harness.paint()
        assertFalse(harness.rows()[0].contains("Search"))
    }

    @Test
    fun selectionAndCaretUseThemeTokens() {
        val controller = TextEditingController("abc")
        val field = TextField(controller)
        val harness = GuiTestHarness(field, columns = 8, rows = 1)
        harness.paint()
        harness.surface.focus.focus(field)
        controller.selection =
            com.psiwray.organization.terminal_window_gui.text
                .TextSelection(0, 2)
        harness.paint()
        assertEquals(harness.surface.theme.selectionBackground, harness.matrix.backgroundAt(0, 0))
        assertEquals(harness.surface.theme.caret, harness.matrix.backgroundAt(2, 0))
    }

    @Test
    fun tabMovesFocusWithoutInserting() {
        val first = TextField()
        val second = TextField()
        val harness = GuiTestHarness(Row(children = listOf(first, second)), columns = 12, rows = 1)
        harness.paint()
        harness.surface.focus.focus(first)
        assertTrue(harness.key(Key.TAB))
        assertTrue(second.isFocused)
        assertFalse(first.isFocused)
        assertTrue(harness.key(Key.TAB, Modifiers.SHIFT))
        assertTrue(first.isFocused)
        assertEquals("", first.controller.text)
    }
}
