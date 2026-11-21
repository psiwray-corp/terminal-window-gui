package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchFieldTest {
    @Test
    fun clearButtonEmptyFocusesFieldAndSubmitSearches() {
        val controller = TextEditingController()
        var searched = ""
        val search = SearchField(controller, onSearch = { searched = it })
        val harness = GuiTestHarness(search, columns = 10, rows = 1)
        harness.paint()
        harness.surface.focus.focus(search.field)
        harness.char('a')
        harness.char('b')
        harness.key(Key.ENTER)
        assertEquals("ab", searched)
        assertTrue(harness.click(9, 0))
        assertEquals("", controller.text)
        assertTrue(search.field.isFocused)
    }

    @Test
    fun clearButtonParticipatesInTabOrder() {
        val search = SearchField()
        val harness = GuiTestHarness(search, columns = 10, rows = 1)
        harness.paint()
        harness.surface.focus.focus(search.field)
        assertTrue(harness.key(Key.TAB))
        assertTrue(harness.surface.focus.focused !== search.field)
        assertTrue(harness.key(Key.SPACE))
        assertTrue(search.field.isFocused)
    }
}
