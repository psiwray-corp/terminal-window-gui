package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PaintFailureTest {
    @Test
    fun failedPaintPreservesBothFlagsForRetry() {
        val root = ThrowSurfaceWidget()
        root.fail = true
        val surface =
            GuiSurface(
                root,
                Themes.dark(),
                ShapeSets.unicode().resolve(GuiFonts.bundled()),
            )
        surface.resize(3, 1)
        assertFailsWith<IllegalStateException> {
            surface.paint(CellCanvas(TerminalMatrix(3, 1)))
        }
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
        assertEquals(0, surface.paintCount)

        root.fail = false
        surface.paint(CellCanvas(TerminalMatrix(3, 1)))
        assertEquals(1, surface.paintCount)
        assertEquals(false, surface.needsLayout)
        assertEquals(false, surface.needsPaint)
    }

    @Test
    fun failedFirstPaintKeepsLayoutDirtyWhenLayoutAlsoThrows() {
        val root = ThrowingLayoutWidget()
        val surface =
            GuiSurface(
                root,
                Themes.dark(),
                ShapeSets.unicode().resolve(GuiFonts.bundled()),
            )
        surface.resize(3, 1)
        assertFailsWith<IllegalStateException> {
            surface.paint(CellCanvas(TerminalMatrix(3, 1)))
        }
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
    }
}

private class ThrowingLayoutWidget : SurfaceWidget(3, 1) {
    override fun measure(
        constraints: com.psiwray.organization.terminal_window_gui.layout.Constraints,
    ): com.psiwray.organization.terminal_window_gui.layout.IntSize = throw IllegalStateException("layout failure")
}
