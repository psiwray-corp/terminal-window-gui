package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurfaceFlagsTest {
    @Test
    fun dispatchLeavesPaintDirtyAndLayoutClean() {
        val surface = surface(4, 2)
        surface.resize(4, 2)
        assertTrue(surface.dispatch(GuiKeyEvent(Key.A, KeyAction.PRESS, Modifiers.NONE, false)).not())
        assertFalse(surface.needsLayout)
        assertTrue(surface.needsPaint)
        assertEquals(0, surface.paintCount)
    }

    @Test
    fun paintClearsPaintFlagWithoutDispatching() {
        val surface = surface(4, 2)
        surface.resize(4, 2)
        surface.paint(CellCanvas(TerminalMatrix(4, 2)))
        assertFalse(surface.needsLayout)
        assertFalse(surface.needsPaint)
        assertEquals(1, surface.paintCount)
    }

    @Test
    fun requestPaintDoesNotRequestLayout() {
        val surface = surface(4, 2)
        surface.resize(4, 2)
        surface.paint(CellCanvas(TerminalMatrix(4, 2)))
        surface.requestPaint()
        assertFalse(surface.needsLayout)
        assertTrue(surface.needsPaint)
    }

    @Test
    fun requestLayoutRequestsPaintToo() {
        val surface = surface(4, 2)
        surface.resize(4, 2)
        surface.paint(CellCanvas(TerminalMatrix(4, 2)))
        surface.requestLayout()
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
    }

    @Test
    fun canvasSizeMismatchIsRejected() {
        val surface = surface(4, 2)
        surface.resize(4, 2)
        val wrong = CellCanvas(TerminalMatrix(3, 2))
        assertFailsWith<GuiException> { surface.paint(wrong) }
        assertFailsWith<IllegalArgumentException> { surface.paint(wrong) }
        assertEquals(0, surface.paintCount)
    }

    private fun surface(
        columns: Int,
        rows: Int,
    ): GuiSurface =
        GuiSurface(
            SurfaceWidget(columns, rows),
            Themes.dark(),
            ShapeSets.unicode().resolve(GuiFonts.bundled()),
        )
}
