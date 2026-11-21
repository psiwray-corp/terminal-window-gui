package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RadioGroupTest {
    @Test
    fun initialSelectionIsExactlyOneAndEmptyGroupIsAllowed() {
        val group =
            RadioGroup(
                selectedKey = "b",
                children =
                    listOf(
                        RadioButton("a", "A"),
                        RadioButton("b", "B"),
                        RadioButton("c", "C"),
                    ),
            )
        val harness = GuiTestHarness(group, columns = 20, rows = 3)
        harness.paint()
        assertEquals("b", group.selectedKey)
        assertEquals(1, group.children.count { (it as RadioButton).isSelected })
        assertNull(RadioGroup().selectedKey)
    }

    @Test
    fun arrowsSelectAndSkipDisabledButtons() {
        val a = RadioButton("a", "A")
        val b = RadioButton("b", "B", enabled = false)
        val c = RadioButton("c", "C")
        val group = RadioGroup(selectedKey = "a", children = listOf(a, b, c))
        val harness = GuiTestHarness(group, columns = 20, rows = 3)
        harness.paint()
        assertTrue(harness.surface.focus.focus(a))
        assertTrue(harness.key(Key.DOWN))
        assertEquals("c", group.selectedKey)
        assertTrue(c.isFocused)
        assertTrue(harness.key(Key.UP))
        assertEquals("a", group.selectedKey)
        assertTrue(a.isFocused)
    }

    @Test
    fun spaceSelectsFocusedRadioAndRovingTabUsesActiveChild() {
        val a = RadioButton("a", "A")
        val b = RadioButton("b", "B")
        val group = RadioGroup(selectedKey = "a", children = listOf(a, b))
        val harness = GuiTestHarness(group, columns = 20, rows = 2)
        harness.paint()
        harness.click(0, 1)
        assertEquals("b", group.selectedKey)
        assertTrue(b.isFocused)

        assertTrue(harness.surface.focus.focus(a))
        assertTrue(harness.key(Key.RIGHT))
        assertEquals("b", group.selectedKey)
        assertTrue(b.isFocused)

        assertTrue(harness.surface.focus.focusNext())
        assertTrue(b.isFocused)
        assertFalse(a.isFocused)
    }

    @Test
    fun disabledGroupRejectsInteraction() {
        val a = RadioButton("a", "A")
        val group = RadioGroup(selectedKey = "a", enabled = false, children = listOf(a))
        val harness = GuiTestHarness(group, columns = 20, rows = 1)
        harness.paint()
        assertFalse(harness.surface.focus.focus(a))
        harness.click(0, 0)
        assertEquals("a", group.selectedKey)
    }

    @Test
    fun selectedAndUnselectedRowsMatchGoldens() {
        val a = RadioButton("a", "Alpha")
        val b = RadioButton("b", "Beta")
        val group = RadioGroup(selectedKey = "a", children = listOf(a, b))
        val harness = GuiTestHarness(group, columns = 16, rows = 2)
        harness.paint()
        GoldenAssert.assertRows("radio-group.txt", harness.rows())
    }

    @Test
    fun changedCallbackFiresForUserSelection() {
        val changes = ArrayList<String>()
        val group =
            RadioGroup(
                selectedKey = "a",
                onChanged = { changes.add(it) },
                children = listOf(RadioButton("a", "A"), RadioButton("b", "B")),
            )
        val harness = GuiTestHarness(group, columns = 10, rows = 2)
        harness.paint()
        harness.click(0, 1)
        assertEquals(listOf("b"), changes)
    }
}
