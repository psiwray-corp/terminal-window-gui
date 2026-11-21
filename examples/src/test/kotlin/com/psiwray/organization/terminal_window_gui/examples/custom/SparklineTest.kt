package com.psiwray.organization.terminal_window_gui.examples.custom

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import kotlin.test.Test
import kotlin.test.assertEquals

class SparklineTest {
    @Test
    fun unicodeGolden() {
        val font = GuiFonts.bundled()
        val surface =
            GuiSurface(
                Sparkline(listOf(0.0, 0.125, 0.25, 0.5, 1.0)),
                Themes.dark(),
                ShapeSets.unicode().resolve(font),
            )
        surface.resize(5, 1)
        val matrix = TerminalMatrix(5, 1)
        surface.paint(CellCanvas(matrix))
        val expected =
            checkNotNull(javaClass.getResourceAsStream("/golden/sparkline.txt"))
                .bufferedReader()
                .use { it.readText().trimEnd('\n', '\r') }
        val actual =
            buildString {
                for (column in 0 until matrix.columns) {
                    appendCodePoint(matrix.codePointAt(column, 0))
                }
            }
        assertEquals(expected, actual)
    }

    @Test
    fun asciiFallbackUsesPublicShapes() {
        val font = GuiFonts.bundled()
        val surface =
            GuiSurface(
                Sparkline(listOf(0.0, 0.25, 1.0)),
                Themes.dark(),
                ShapeSets.unicode().resolve(font),
            )
        surface.resize(3, 1)
        surface.setShapes(ShapeSets.ascii().resolve(font))
        val matrix = TerminalMatrix(3, 1)
        surface.paint(CellCanvas(matrix))
        assertEquals(0x20, matrix.codePointAt(0, 0))
        assertEquals(0x23, matrix.codePointAt(1, 0))
        assertEquals(0x23, matrix.codePointAt(2, 0))
    }
}
