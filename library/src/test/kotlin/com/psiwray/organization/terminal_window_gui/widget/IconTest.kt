package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class IconTest {
    @Test
    fun semanticShapeUsesResolvedCodePoint() {
        val harness = displayHarness(Icon(Shape.CHECK), columns = 1, rows = 1)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.CHECK], harness.matrix.codePointAt(0, 0))
        GoldenAssert.assertRows("icon-check.txt", harness.rows())
    }

    @Test
    fun explicitGlyphIsWrittenDirectly() {
        val harness = displayHarness(Icon.glyph('X'), columns = 1, rows = 1)
        harness.paint()
        assertEquals('X'.code, harness.matrix.codePointAt(0, 0))
    }

    @Test
    fun asciiCatalogRendersFallbackShape() {
        val harness =
            displayHarness(
                Icon(Shape.CHECK),
                columns = 1,
                rows = 1,
                shapes = ShapeSets.ascii().resolve(GuiFonts.bundled()),
            )
        harness.paint()
        assertEquals('x'.code, harness.matrix.codePointAt(0, 0))
    }

    @Test
    fun colorsAndEmptyIconAreSafe() {
        val harness =
            displayHarness(
                Icon(Shape.DOT, foreground = Colors.WHITE, background = Colors.BLACK),
                columns = 1,
                rows = 1,
            )
        harness.paint()
        assertEquals(Colors.WHITE, harness.matrix.foregroundAt(0, 0))
        assertEquals(Colors.BLACK, harness.matrix.backgroundAt(0, 0))
    }
}
