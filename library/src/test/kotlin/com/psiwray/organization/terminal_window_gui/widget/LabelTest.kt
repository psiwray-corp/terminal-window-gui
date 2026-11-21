package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.paint.TextWrap
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabelTest {
    @Test
    fun plainLabelMatchesGolden() {
        val harness = displayHarness(Label("hello"), columns = 6, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("label-plain.txt", harness.rows())
    }

    @Test
    fun wordWrapAndAlignment() {
        val wrapped = displayHarness(Label("one two three", wrap = TextWrap.WORD), columns = 7, rows = 2)
        wrapped.paint()
        assertEquals("one two", wrapped.rows()[0])
        assertEquals("three  ", wrapped.rows()[1])
        val centered = displayHarness(Label("ab", alignment = TextAlignment.CENTER), columns = 6, rows = 1)
        centered.paint()
        assertEquals("  ab  ", centered.rows()[0])
        val right = displayHarness(Label("ab", alignment = TextAlignment.RIGHT), columns = 6, rows = 1)
        right.paint()
        assertEquals("    ab", right.rows()[0])
    }

    @Test
    fun clippedLastLineEndsWithEllipsis() {
        val label = Label("hello world", wrap = TextWrap.WORD, maxLines = 1)
        val harness = displayHarness(label, columns = 6, rows = 1)
        harness.paint()
        val ellipsis = harness.surface.shapes[Shape.ELLIPSIS]
        assertEquals(ellipsis, harness.matrix.codePointAt(5, 0))
    }

    @Test
    fun surrogatePairsAreNotSplit() {
        val text = "A\uD83D\uDE00B"
        val harness = displayHarness(Label(text), columns = 3, rows = 1)
        harness.paint()
        assertEquals('A'.code, harness.matrix.codePointAt(0, 0))
        assertEquals(0x1F600, harness.matrix.codePointAt(1, 0))
        assertEquals('B'.code, harness.matrix.codePointAt(2, 0))
    }

    @Test
    fun foregroundAndBackgroundApply() {
        val label = Label("x", foreground = Colors.rgb(255, 0, 0), background = Colors.rgb(0, 0, 255))
        val harness = displayHarness(label, columns = 2, rows = 1)
        harness.paint()
        assertEquals(Colors.rgb(255, 0, 0), harness.matrix.foregroundAt(0, 0))
        assertEquals(Colors.rgb(0, 0, 255), harness.matrix.backgroundAt(0, 0))
    }

    @Test
    fun emptyLabelIsAtLeastOneCell() {
        val harness = displayHarness(Label(), columns = 1, rows = 1)
        harness.paint()
        assertTrue(harness.rows().size == 1)
    }
}
