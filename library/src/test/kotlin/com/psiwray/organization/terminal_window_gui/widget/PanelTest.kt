package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelTest {
    @Test
    fun singleDoubleAndRoundedBordersMatchGoldens() {
        for (style in listOf(BoxStyle.SINGLE, BoxStyle.DOUBLE, BoxStyle.ROUNDED)) {
            val harness = displayHarness(Panel(title = "Hi", style = style), columns = 7, rows = 3)
            harness.paint()
            GoldenAssert.assertRows("panel-${style.name.lowercase()}.txt", harness.rows())
        }
    }

    @Test
    fun asciiShapeSetRendersAsciiBorders() {
        val harness =
            displayHarness(
                Panel(title = "Hi"),
                columns = 7,
                rows = 3,
                shapes = ShapeSets.ascii().resolve(GuiFonts.bundled()),
            )
        harness.paint()
        GoldenAssert.assertRows("panel-ascii.txt", harness.rows())
    }

    @Test
    fun titleTruncatesWithEllipsis() {
        val harness = displayHarness(Panel(title = "long title"), columns = 6, rows = 3)
        harness.paint()
        val resolved = harness.surface.shapes[Shape.ELLIPSIS]
        val row = harness.matrix
        var found = false
        var column = 0
        while (column < 6) {
            if (row.codePointAt(column, 0) == resolved) {
                found = true
            }
            column++
        }
        assertTrue(found)
    }

    @Test
    fun titleAndChildArePaintedInsideTheBorder() {
        val panel = Panel(title = "Hi", child = Label("x"))
        val harness = displayHarness(panel, columns = 8, rows = 4)
        harness.paint()
        assertEquals('x'.code, harness.matrix.codePointAt(1, 1))
    }

    @Test
    fun nestedPanelKeepsItsContentBackgroundAndBottomBorder() {
        val panel = Panel(title = "Hi", child = Label("x"))
        val root = Column(children = listOf(SizedBox(height = 3), panel))
        val harness = GuiTestHarness(root, columns = 8, rows = 6)
        harness.paint()
        assertEquals(harness.surface.theme.surface, harness.matrix.backgroundAt(2, panel.bounds.y + 1))
        assertEquals(
            harness.surface.shapes[Shape.BOX_SINGLE_BOTTOM_LEFT],
            harness.matrix.codePointAt(0, panel.bounds.bottom - 1),
        )
    }

    @Test
    fun focusChangeRepaintsFocusedBorder() {
        val panel = Panel("Hi")
        val harness = displayHarness(panel, columns = 7, rows = 3)
        harness.paint()
        assertEquals(harness.surface.theme.border, harness.matrix.foregroundAt(0, 0))

        assertTrue(harness.key(Key.TAB))
        assertTrue(harness.surface.needsPaint)
        harness.paint()
        assertEquals(harness.surface.theme.borderFocused, harness.matrix.foregroundAt(0, 0))
    }

    @Test
    fun focusedPanelIsVisibleInMonochrome() {
        val panel = Panel(title = "Focus")
        val harness = displayHarness(panel, columns = 9, rows = 3, theme = Themes.highContrast())
        harness.paint()
        assertTrue(harness.surface.focus.focus(panel))
        harness.surface.requestPaint()
        harness.paint()
        assertTrue(harness.rows().isNotEmpty())
    }
}
