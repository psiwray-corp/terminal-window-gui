package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.paint.TextWrap
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RichLabelTest {
    @Test
    fun spanColorsFlowThroughPainting() {
        val label =
            RichLabel(
                spans =
                    listOf(
                        TextSpan("red", Colors.rgb(255, 0, 0)),
                        TextSpan("green", Colors.rgb(0, 255, 0)),
                    ),
            )
        val harness = displayHarness(label, columns = 8, rows = 1)
        harness.paint()
        assertEquals(Colors.rgb(255, 0, 0), harness.matrix.foregroundAt(0, 0))
        assertEquals(Colors.rgb(255, 0, 0), harness.matrix.foregroundAt(2, 0))
        assertEquals(Colors.rgb(0, 255, 0), harness.matrix.foregroundAt(3, 0))
        assertEquals(Colors.rgb(0, 255, 0), harness.matrix.foregroundAt(7, 0))
        GoldenAssert.assertRows("richlabel-spans.txt", harness.rows())
    }

    @Test
    fun spansWrapAsWords() {
        val label =
            RichLabel(
                spans = listOf(TextSpan("one "), TextSpan("two three", Colors.rgb(255, 0, 0))),
                wrap = TextWrap.WORD,
            )
        val harness = displayHarness(label, columns = 7, rows = 2)
        harness.paint()
        assertEquals("one two", harness.rows()[0])
        assertEquals("three  ", harness.rows()[1])
        assertEquals(Colors.rgb(255, 0, 0), harness.matrix.foregroundAt(4, 0))
    }

    @Test
    fun truncatedLastLineUsesEllipsisShape() {
        val label = RichLabel(spans = listOf(TextSpan("hello world")), maxLines = 1)
        val harness = displayHarness(label, columns = 6, rows = 1)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.ELLIPSIS], harness.matrix.codePointAt(5, 0))
    }

    @Test
    fun surrogateSpansRemainWhole() {
        val label = RichLabel(spans = listOf(TextSpan("A\uD83D\uDE00B")))
        val harness = displayHarness(label, columns = 3, rows = 1)
        harness.paint()
        assertEquals(0x1F600, harness.matrix.codePointAt(1, 0))
        assertTrue(harness.rows()[0].codePointCount(0, harness.rows()[0].length) == 3)
    }
}
