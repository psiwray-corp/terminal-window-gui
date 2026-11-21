package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckboxTest {
    @Test
    fun mouseAndSpaceToggle() {
        val checkbox = Checkbox("Enable")
        val harness = GuiTestHarness(checkbox, columns = 12, rows = 1)
        harness.paint()
        harness.click(0, 0)
        assertEquals(CheckState.CHECKED, checkbox.state)
        assertTrue(checkbox.isFocused)
        assertTrue(harness.key(Key.SPACE))
        assertEquals(CheckState.UNCHECKED, checkbox.state)
        harness.key(Key.SPACE, action = KeyAction.REPEAT, repeat = true)
        assertEquals(CheckState.UNCHECKED, checkbox.state)
    }

    @Test
    fun indeterminateTransitionsFollowTheFlag() {
        val allowed = Checkbox("Mixed", CheckState.INDETERMINATE, allowIndeterminate = true)
        val allowedHarness = GuiTestHarness(allowed, columns = 12, rows = 1)
        allowedHarness.paint()
        allowedHarness.click(0, 0)
        assertEquals(CheckState.CHECKED, allowed.state)

        val denied = Checkbox("Mixed", CheckState.INDETERMINATE)
        val deniedHarness = GuiTestHarness(denied, columns = 12, rows = 1)
        deniedHarness.paint()
        deniedHarness.click(0, 0)
        assertEquals(CheckState.UNCHECKED, denied.state)
    }

    @Test
    fun callbackReceivesEveryToggle() {
        val states = ArrayList<CheckState>()
        val checkbox = Checkbox("Value", onChanged = { states.add(it) })
        val harness = GuiTestHarness(checkbox, columns = 12, rows = 1)
        harness.paint()
        harness.click(0, 0)
        harness.click(0, 0)
        assertEquals(listOf(CheckState.CHECKED, CheckState.UNCHECKED), states)
    }

    @Test
    fun disabledCheckboxReceivesNoFocusOrChange() {
        val checkbox = Checkbox("Disabled", enabled = false)
        val harness = GuiTestHarness(checkbox, columns = 12, rows = 1)
        harness.paint()
        assertFalse(harness.surface.focus.focus(checkbox))
        harness.click(0, 0)
        harness.key(Key.SPACE)
        assertEquals(CheckState.UNCHECKED, checkbox.state)
        assertFalse(checkbox.isFocused)
    }

    @Test
    fun allStatesMatchGoldens() {
        val unchecked = GuiTestHarness(Checkbox("Off"), columns = 12, rows = 1)
        unchecked.paint()
        GoldenAssert.assertRows("checkbox-unchecked.txt", unchecked.rows())

        val checked = GuiTestHarness(Checkbox("On", CheckState.CHECKED), columns = 12, rows = 1)
        checked.paint()
        GoldenAssert.assertRows("checkbox-checked.txt", checked.rows())

        val indeterminate = GuiTestHarness(Checkbox("Mixed", CheckState.INDETERMINATE), columns = 12, rows = 1)
        indeterminate.paint()
        GoldenAssert.assertRows("checkbox-indeterminate.txt", indeterminate.rows())

        val focused = GuiTestHarness(Checkbox("Focus"), columns = 12, rows = 1)
        focused.surface.focus.focus(focused.surface.root)
        focused.paint()
        GoldenAssert.assertRows("checkbox-focused.txt", focused.rows())
    }

    @Test
    fun truncatedLabelUsesEllipsisAndStaysInBounds() {
        val checkbox = Checkbox("A long label")
        val harness = GuiTestHarness(checkbox, columns = 6, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains('…'))
        assertEquals(6, harness.rows()[0].length)
    }
}
