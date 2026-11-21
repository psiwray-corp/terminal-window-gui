package com.psiwray.organization.terminal_window_gui.paint

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CellCanvasTest {
    @Test
    fun writesOnlyInsideMatrix() {
        val matrix = TerminalMatrix(6, 3)
        val canvas = CellCanvas(matrix)

        canvas.set(0, 0, 'A'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(-1, 0, 'B'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(6, 0, 'C'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(0, 3, 'D'.code, Colors.WHITE, Colors.BLACK)

        assertEquals("A     ", row(matrix, 0))
        assertEquals("      ", row(matrix, 1))
    }

    @Test
    fun originAndClipTranslateToScreenCells() {
        val matrix = TerminalMatrix(6, 3)
        val canvas = CellCanvas(matrix)

        canvas.pushClip(Rect(2, 1, 2, 2))
        canvas.pushOrigin(2, 1)
        canvas.set(0, 0, 'A'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(2, 0, 'B'.code, Colors.WHITE, Colors.BLACK)
        canvas.popOrigin()
        canvas.set(0, 0, 'C'.code, Colors.WHITE, Colors.BLACK)
        canvas.popClip()

        assertEquals("      ", row(matrix, 0))
        assertEquals("  A   ", row(matrix, 1))
        assertEquals("      ", row(matrix, 2))
    }

    @Test
    fun nestedOriginsAccumulateAndClipIntersects() {
        val matrix = TerminalMatrix(8, 5)
        val canvas = CellCanvas(matrix)

        canvas.pushOrigin(2, 1)
        canvas.pushClip(Rect(1, 1, 2, 2))
        canvas.set(0, 0, 'X'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(1, 1, 'Y'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(2, 1, 'Z'.code, Colors.WHITE, Colors.BLACK)
        canvas.set(3, 1, 'W'.code, Colors.WHITE, Colors.BLACK)
        canvas.popClip()
        canvas.popOrigin()

        assertEquals("        ", row(matrix, 0))
        assertEquals("        ", row(matrix, 1))
        assertEquals("   YZ   ", row(matrix, 2))
        assertEquals("        ", row(matrix, 3))
    }

    @Test
    fun textHandlesTabsNewlinesAndRightEdge() {
        val matrix = TerminalMatrix(12, 2)
        val canvas = CellCanvas(matrix)

        assertEquals(5, canvas.text(0, 0, "a\tb", Colors.WHITE, Colors.BLACK))
        assertEquals("a   b       ", row(matrix, 0))

        val second = TerminalMatrix(5, 2)
        val secondCanvas = CellCanvas(second)
        secondCanvas.pushClip(Rect(0, 0, 5, 1))
        assertEquals(0, secondCanvas.text(0, 0, "ab\ncd", Colors.WHITE, Colors.BLACK))
        assertEquals("ab   ", row(second, 0))
        assertEquals("     ", row(second, 1))
        secondCanvas.popClip()
    }

    @Test
    fun surrogatePairsOccupyOneCell() {
        val matrix = TerminalMatrix(5, 1)
        val canvas = CellCanvas(matrix)

        assertEquals(3, canvas.text(0, 0, "a\uD83D\uDE00b", Colors.WHITE, Colors.BLACK))
        assertEquals('a'.code, matrix.codePointAt(0, 0))
        assertEquals(0x1F600, matrix.codePointAt(1, 0))
        assertEquals('b'.code, matrix.codePointAt(2, 0))
    }

    @Test
    fun textSkipsLeftClipAndStopsAtRightClip() {
        val matrix = TerminalMatrix(6, 1)
        val canvas = CellCanvas(matrix)

        canvas.pushClip(Rect(2, 0, 3, 1))
        assertEquals(5, canvas.text(0, 0, "abcde", Colors.WHITE, Colors.BLACK))
        assertEquals("  cde ", row(matrix, 0))
        canvas.popClip()
    }

    @Test
    fun fillLinesAndOriginCompose() {
        val matrix = TerminalMatrix(6, 4)
        val canvas = CellCanvas(matrix)

        canvas.pushOrigin(1, 1)
        canvas.hline(0, 0, 3, '-'.code, Colors.WHITE, Colors.BLACK)
        canvas.vline(4, 0, 2, '|'.code, Colors.WHITE, Colors.BLACK)
        canvas.fill(Rect(0, 2, 2, 1), '#'.code, Colors.WHITE, Colors.BLACK)
        canvas.popOrigin()

        assertEquals("      ", row(matrix, 0))
        assertEquals(" --- |", row(matrix, 1))
        assertEquals("     |", row(matrix, 2))
        assertEquals(" ##   ", row(matrix, 3))
    }

    @Test
    fun fillOutsideMatrixIsClipped() {
        val matrix = TerminalMatrix(3, 2)
        val canvas = CellCanvas(matrix)

        canvas.fill(Rect(-20, -20, 100, 100), 'X'.code, Colors.WHITE, Colors.BLACK)

        assertEquals("XXX", row(matrix, 0))
        assertEquals("XXX", row(matrix, 1))
    }

    @Test
    fun stackErrorsAreReported() {
        val canvas = CellCanvas(TerminalMatrix(2, 2))
        assertFailsWith<IllegalStateException> { canvas.popClip() }
        assertFailsWith<IllegalStateException> { canvas.popOrigin() }

        repeat(32) {
            canvas.pushClip(Rect(0, 0, 2, 2))
        }
        assertFailsWith<IllegalStateException> {
            canvas.pushClip(Rect(0, 0, 2, 2))
        }
        repeat(32) {
            canvas.popClip()
        }

        canvas.pushOrigin(1, 1)
        canvas.popOrigin()
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
