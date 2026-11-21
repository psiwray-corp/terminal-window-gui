package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolbarTest {
    @Test
    fun childrenStayInOrderAndInsideBounds() {
        val first = Button("A")
        val separator = Separator(Orientation.VERTICAL)
        val spacer = Spacer()
        val last = Button("B")
        val toolbar = Toolbar(listOf(first, separator, spacer, last))
        val harness = GuiTestHarness(toolbar, columns = 20, rows = 1)
        harness.paint()

        assertEquals(20, toolbar.bounds.width)
        assertTrue(first.bounds.x == 0)
        assertTrue(first.bounds.right <= separator.bounds.x)
        assertTrue(separator.bounds.width == 1)
        assertTrue(spacer.bounds.x >= separator.bounds.right)
        assertTrue(last.bounds.right <= 20)
        assertTrue(last.bounds.x >= spacer.bounds.x)
    }

    @Test
    fun focusOrderSkipsDisabledAndNonFocusableChildren() {
        val a = Button("A")
        val disabled = Button("B", enabled = false)
        val spacer = Spacer()
        val c = Button("C")
        val toolbar = Toolbar(listOf(a, disabled, spacer, c))
        val harness = GuiTestHarness(toolbar, columns = 20, rows = 1)
        harness.paint()
        assertTrue(harness.surface.focus.focusFirst())
        assertTrue(a.isFocused)
        assertTrue(harness.surface.focus.focusNext())
        assertTrue(c.isFocused)
        assertTrue(harness.surface.focus.focusNext())
        assertTrue(a.isFocused)
    }

    @Test
    fun narrowToolbarShrinksWithoutOverflow() {
        val a = Button("Alpha")
        val b = Button("Beta")
        val toolbar = Toolbar(listOf(a, b))
        val harness = GuiTestHarness(toolbar, columns = 5, rows = 1)
        harness.paint()
        for (child in toolbar.children) {
            assertTrue(child.bounds.x >= 0)
            assertTrue(child.bounds.right <= 5)
        }
    }

    @Test
    fun toolbarMatchesGolden() {
        val toolbar =
            Toolbar(
                listOf(
                    Button("Open"),
                    Separator(Orientation.VERTICAL),
                    Button("Close"),
                    Spacer(),
                    Button("Help"),
                ),
            )
        val harness = GuiTestHarness(toolbar, columns = 40, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("toolbar.txt", harness.rows())
    }
}
