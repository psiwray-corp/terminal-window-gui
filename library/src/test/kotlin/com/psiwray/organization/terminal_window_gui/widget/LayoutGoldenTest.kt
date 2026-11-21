package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Length
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutGoldenTest {
    @Test
    fun oneByOneMatchesGolden() {
        assertGolden("1x1", 1, 1)
    }

    @Test
    fun eightyByTwentyFourMatchesGolden() {
        assertGolden("80x24", 80, 24)
    }

    @Test
    fun twoFortyBySixtySevenMatchesGolden() {
        assertGolden("240x67", 240, 67)
    }

    private fun assertGolden(
        name: String,
        columns: Int,
        rows: Int,
    ) {
        val actual = renderFixture(columns, rows)
        val file = File("src/test/resources/golden/layout-$name.txt")
        if (System.getenv("UPDATE_GOLDENS") == "true") {
            file.parentFile.mkdirs()
            file.writeText(actual)
            return
        }
        require(file.isFile) { "missing golden ${file.absolutePath}" }
        assertEquals(file.readText(), actual)
    }

    private fun renderFixture(
        columns: Int,
        rows: Int,
    ): String {
        val root = fixture()
        root.layout(Constraints.tight(IntSize(columns, rows)))
        val matrix = testMatrix(columns, rows)
        root.render(testContext(matrix))
        val builder = StringBuilder((columns + 1) * rows)
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val codePoint = matrix.codePointAt(column, row)
                when {
                    codePoint == 0 -> builder.append('.')
                    codePoint == ' '.code -> builder.append(' ')
                    else -> builder.appendCodePoint(codePoint)
                }
            }
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun fixture(): Widget =
        Column(
            gap = 1,
            children =
                listOf(
                    Row(
                        gap = 1,
                        children =
                            listOf(
                                PatternWidget(3, 2, 0),
                                Expanded(child = PatternWidget(1, 1, 3)),
                                PatternWidget(2, 1, 6),
                            ),
                    ),
                    Padding(
                        insets = Insets.only(left = 1, right = 1),
                        child = PatternWidget(5, 2, 9),
                    ),
                    Stack(
                        children =
                            listOf(
                                PatternWidget(4, 2, 12),
                                Positioned(
                                    left = Length.Cells(1),
                                    top = Length.Cells(1),
                                    width = Length.Cells(2),
                                    height = Length.Cells(1),
                                    child = PatternWidget(2, 1, 15),
                                ),
                            ),
                    ),
                    Grid(
                        columns = listOf(GridTrack.Fixed(2), GridTrack.Fixed(2)),
                        rows = listOf(GridTrack.Fixed(1), GridTrack.Fixed(1)),
                        gap = 1,
                        children =
                            listOf(
                                PatternWidget(2, 1, 18),
                                PatternWidget(2, 1, 19),
                                PatternWidget(2, 1, 20),
                                PatternWidget(2, 1, 21),
                            ),
                    ),
                    Wrap(
                        gap = 1,
                        runGap = 1,
                        children =
                            listOf(
                                PatternWidget(3, 1, 22),
                                PatternWidget(3, 1, 23),
                                PatternWidget(3, 1, 24),
                            ),
                    ),
                ),
        )

    private class PatternWidget(
        private val preferredWidth: Int,
        private val preferredHeight: Int,
        private val seed: Int,
    ) : Widget() {
        override fun measure(constraints: Constraints): IntSize =
            constraints.constrain(IntSize(preferredWidth, preferredHeight))

        override fun paint(context: PaintContext) {
            for (row in 0 until bounds.height) {
                for (column in 0 until bounds.width) {
                    val codePoint = 'A'.code + ((column + row + seed) % 26)
                    context.canvas.set(column, row, codePoint, Colors.WHITE, Colors.BLACK)
                }
            }
        }
    }
}
