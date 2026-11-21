package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ShapeCatalogGoldenTest {
    @Test
    fun unicodeCatalogMatchesGolden() {
        assertCatalog(ShapeSets.unicode(), "/golden/shapes-unicode.txt")
    }

    @Test
    fun asciiCatalogMatchesGolden() {
        assertCatalog(ShapeSets.ascii(), "/golden/shapes-ascii.txt")
    }

    private fun assertCatalog(
        set: ShapeSet,
        resource: String,
    ) {
        val resolved = set.resolve(GuiFonts.bundled())
        val actual = catalog(resolved)
        val expected =
            assertNotNull(
                ShapeCatalogGoldenTest::class.java.getResourceAsStream(resource),
                "missing golden resource $resource",
            ).bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertEquals(expected, actual)
    }

    private fun catalog(resolved: ResolvedShapeSet): String {
        val matrix = TerminalMatrix(8, 16)
        val canvas = CellCanvas(matrix)
        val builder = StringBuilder()
        for (shape in Shape.entries) {
            canvas.fill(
                Rect(0, 0, 8, 16),
                ' '.code,
                Colors.WHITE,
                Colors.BLACK,
            )
            canvas.set(0, 0, resolved[shape], Colors.WHITE, Colors.BLACK)
            builder.append(shape.name).append('\n')
            for (row in 0 until 16) {
                for (column in 0 until 8) {
                    val codePoint = matrix.codePointAt(column, row)
                    if (codePoint == ' '.code) {
                        builder.append('.')
                    } else {
                        builder.appendCodePoint(codePoint)
                    }
                }
                builder.append('\n')
            }
            builder.append('\n')
        }
        return builder.toString()
    }
}
