package com.psiwray.organization.terminal_window_gui.paint

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import kotlin.test.Test
import kotlin.test.assertEquals

class PaintContextTest {
    private val shapes = ShapeSets.unicode().resolve(GuiFonts.bundled())

    @Test
    fun shapesUseLocalOrigins() {
        val matrix = TerminalMatrix(8, 4)
        val context = PaintContext(CellCanvas(matrix), shapes)

        context.pushOrigin(2, 1)
        context.shape(Shape.CHECK, 0, 0, Colors.WHITE, Colors.BLACK)
        context.pushOrigin(1, 1)
        context.shape(Shape.CROSS, 0, 0, Colors.WHITE, Colors.BLACK)
        context.popOrigin()
        context.popOrigin()

        assertEquals(0x2713, matrix.codePointAt(2, 1))
        assertEquals(0x2717, matrix.codePointAt(3, 2))
    }

    @Test
    fun localClipTranslatesAndStopsText() {
        val matrix = TerminalMatrix(6, 3)
        val context = PaintContext(CellCanvas(matrix), shapes)

        context.pushOrigin(1, 1)
        context.pushClip(Rect(0, 0, 2, 2))
        assertEquals(2, context.text(0, 0, "XYZ", Colors.WHITE, Colors.BLACK))
        context.popClip()
        context.popOrigin()

        assertEquals("      ", row(matrix, 0))
        assertEquals(" XY   ", row(matrix, 1))
    }

    @Test
    fun nestedClipsIntersectInScreenSpace() {
        val matrix = TerminalMatrix(8, 4)
        val context = PaintContext(CellCanvas(matrix), shapes)

        context.pushOrigin(2, 1)
        context.pushClip(Rect(1, 1, 3, 2))
        context.pushOrigin(1, 0)
        context.pushClip(Rect(0, 1, 1, 1))
        context.shape(Shape.DOT, 0, 1, Colors.WHITE, Colors.BLACK)
        context.popClip()
        context.popOrigin()
        context.popClip()
        context.popOrigin()

        assertEquals(0x2022, matrix.codePointAt(3, 2))
    }

    @Test
    fun fillWritesSpacesWithColors() {
        val matrix = TerminalMatrix(4, 3)
        val context = PaintContext(CellCanvas(matrix), shapes)
        val fg = Colors.rgb(1, 2, 3)
        val bg = Colors.rgb(4, 5, 6)

        context.fill(Rect(1, 0, 2, 2), fg, bg)

        for (row in 0..1) {
            for (column in 1..2) {
                assertEquals(' '.code, matrix.codePointAt(column, row))
                assertEquals(fg, matrix.foregroundAt(column, row))
                assertEquals(bg, matrix.backgroundAt(column, row))
            }
        }
        assertEquals(Colors.WHITE, matrix.foregroundAt(0, 0))
    }

    private fun row(
        matrix: TerminalMatrix,
        row: Int,
    ): String {
        val builder = StringBuilder(matrix.columns)
        for (column in 0 until matrix.columns) {
            builder.appendCodePoint(matrix.codePointAt(column, row))
        }
        return builder.toString()
    }
}
