package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwitchTest {
    @Test
    fun spaceEnterAndMouseToggle() {
        val switch = Switch("Wi-Fi")
        val changes = ArrayList<Boolean>()
        switch.onChanged = { changes.add(it) }
        val harness = GuiTestHarness(switch, columns = 14, rows = 1)
        harness.paint()
        harness.click(0, 0)
        assertTrue(switch.checked)
        assertTrue(switch.isFocused)
        assertTrue(harness.key(Key.SPACE))
        assertFalse(switch.checked)
        assertTrue(harness.key(Key.ENTER))
        assertTrue(switch.checked)
        assertEquals(listOf(true, false, true), changes)
    }

    @Test
    fun leftAndRightSelectOffAndOn() {
        val switch = Switch("Mode", checked = true)
        val harness = GuiTestHarness(switch, columns = 14, rows = 1)
        harness.paint()
        harness.surface.focus.focus(switch)
        assertTrue(harness.key(Key.LEFT))
        assertFalse(switch.checked)
        assertTrue(harness.key(Key.RIGHT))
        assertTrue(switch.checked)
    }

    @Test
    fun disabledSwitchReceivesNoFocusOrChange() {
        val switch = Switch("Disabled", enabled = false)
        val harness = GuiTestHarness(switch, columns = 14, rows = 1)
        harness.paint()
        assertFalse(harness.surface.focus.focus(switch))
        harness.click(0, 0)
        harness.key(Key.SPACE)
        assertFalse(switch.checked)
        assertFalse(switch.isFocused)
    }

    @Test
    fun onOffAndFocusMatchGoldens() {
        val off = GuiTestHarness(Switch("Off"), columns = 12, rows = 1)
        off.paint()
        GoldenAssert.assertRows("switch-off.txt", off.rows())

        val on = GuiTestHarness(Switch("On", checked = true), columns = 12, rows = 1)
        on.paint()
        GoldenAssert.assertRows("switch-on.txt", on.rows())

        val focused = GuiTestHarness(Switch("Focus"), columns = 12, rows = 1)
        focused.surface.focus.focus(focused.surface.root)
        focused.paint()
        GoldenAssert.assertRows("switch-focused.txt", focused.rows())
    }

    @Test
    fun longLabelTruncatesInsideBounds() {
        val harness = GuiTestHarness(Switch("A long label"), columns = 8, rows = 1)
        harness.paint()
        assertEquals(8, harness.rows()[0].length)
        assertTrue(harness.rows()[0].contains('…'))
    }
}
