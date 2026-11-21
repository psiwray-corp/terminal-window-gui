package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResizeTest {
    @Test
    fun oneByOneResizeDoesNotPaintAndLayoutsOnDispatch() {
        assertResize(1, 1)
    }

    @Test
    fun eightyByTwentyFourResizeDoesNotPaintAndLayoutsOnDispatch() {
        assertResize(80, 24)
    }

    @Test
    fun twoHundredFortyBySixtySevenResizeDoesNotPaintAndLayoutsOnDispatch() {
        assertResize(240, 67)
    }

    @Test
    fun nonPositiveResizeIsRejected() {
        val surface = surface()
        assertFailsWith<GuiException> { surface.resize(0, 1) }
        assertFailsWith<GuiException> { surface.resize(1, 0) }
        assertFailsWith<GuiException> { surface.resize(-1, 1) }
        assertEquals(GridSize.ZERO, surface.size)
    }

    private fun assertResize(
        columns: Int,
        rows: Int,
    ) {
        val root = SurfaceWidget(columns, rows, consumeEvents = true)
        val surface = surface(root)
        surface.resize(columns, rows)
        assertEquals(GridSize(columns, rows), surface.size)
        assertEquals(Rect(0, 0, columns, rows), root.bounds)
        assertEquals(Rect(0, 0, columns, rows), surface.overlays.screen)
        assertEquals(0, root.paintCount)
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
        val layoutBefore = surface.layoutCount
        assertTrue(surface.dispatch(GuiKeyEvent(Key.A, KeyAction.PRESS, Modifiers.NONE, false)))
        assertEquals(layoutBefore + 1, surface.layoutCount)
        assertEquals(Rect(0, 0, columns, rows), root.bounds)
        assertEquals(0, root.paintCount)
    }

    private fun surface(root: SurfaceWidget = SurfaceWidget(1, 1)): GuiSurface =
        GuiSurface(root, Themes.dark(), ShapeSets.unicode().resolve(GuiFonts.bundled()))
}
