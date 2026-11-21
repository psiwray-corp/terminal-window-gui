package com.psiwray.organization.terminal_window_gui.paint

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextMetricsTest {
    private val metrics = TextMetrics()

    @Test
    fun expandsTabsAndPreservesNewlines() {
        assertEquals(listOf("a   b"), metrics.measure("a\tb", 10).lines)
        assertEquals(listOf("ab  c"), metrics.measure("ab\tc", 10).lines)
        assertEquals(listOf("abcd    e"), metrics.measure("abcd\te", 10).lines)
        assertEquals(listOf("a", "b"), metrics.measure("a\nb", 10).lines)
        assertEquals(listOf("a", ""), metrics.measure("a\n", 10).lines)
        assertEquals(listOf("", ""), metrics.measure("\n", 10).lines)
        assertEquals(emptyList(), metrics.measure("", 10).lines)
    }

    @Test
    fun replacesOtherControlsAndCountsSurrogatePairs() {
        assertEquals(listOf("a b"), metrics.measure("a\u0001b", 10).lines)
        assertEquals(listOf("a b"), metrics.measure("a\rb", 10).lines)
        val layout = metrics.measure("a\uD83D\uDE00b", 10)
        assertEquals(listOf("a\uD83D\uDE00b"), layout.lines)
        assertEquals(3, layout.width)
        assertEquals(1, layout.height)
    }

    @Test
    fun noneTruncatesLongLines() {
        val layout = metrics.measure("abcdef", 3)

        assertEquals(listOf("abc"), layout.lines)
        assertEquals(3, layout.width)
        assertEquals(1, layout.height)
        assertTrue(layout.truncated)
    }

    @Test
    fun widthOneAndZero() {
        val one = metrics.measure("ab", 1)
        assertEquals(listOf("a"), one.lines)
        assertTrue(one.truncated)

        val chars = metrics.measure("ab", 1, TextWrap.CHAR)
        assertEquals(listOf("a", "b"), chars.lines)
        assertFalse(chars.truncated)

        val zero = metrics.measure("ab", 0)
        assertEquals(emptyList(), zero.lines)
        assertTrue(zero.truncated)

        val empty = metrics.measure("", 0)
        assertEquals(emptyList(), empty.lines)
        assertFalse(empty.truncated)
    }

    @Test
    fun charWrapKeepsEveryCodePoint() {
        val layout = metrics.measure("abcdef", 2, TextWrap.CHAR)

        assertEquals(listOf("ab", "cd", "ef"), layout.lines)
        assertEquals(2, layout.width)
        assertEquals(3, layout.height)
        assertFalse(layout.truncated)

        val surrogate = metrics.measure("a\uD83D\uDE00b", 2, TextWrap.CHAR)
        assertEquals(listOf("a\uD83D\uDE00", "b"), surrogate.lines)
    }

    @Test
    fun wordWrapBreaksAtSpacesAndHardBreaksLongWords() {
        assertEquals(
            listOf("hello", "world"),
            metrics.measure("hello world", 5, TextWrap.WORD).lines,
        )
        assertEquals(
            listOf("abc", "def", "gh"),
            metrics.measure("abcdefgh", 3, TextWrap.WORD).lines,
        )
        assertEquals(
            listOf("ab", "cd"),
            metrics.measure("ab cd", 3, TextWrap.WORD).lines,
        )
        assertEquals(
            listOf("a b"),
            metrics.measure("a  b", 3, TextWrap.WORD).lines,
        )
        assertEquals(
            listOf("a", "", "b"),
            metrics.measure("a\n\nb", 10, TextWrap.WORD).lines,
        )
    }

    @Test
    fun maxLinesTruncatesAndSetsFlag() {
        val layout = metrics.measure("a\nb\nc", 10, maxLines = 2)
        assertEquals(listOf("a", "b"), layout.lines)
        assertEquals(1, layout.width)
        assertEquals(2, layout.height)
        assertTrue(layout.truncated)

        val exact = metrics.measure("a\nb", 10, maxLines = 2)
        assertEquals(listOf("a", "b"), exact.lines)
        assertFalse(exact.truncated)

        val zero = metrics.measure("a\nb", 10, maxLines = 0)
        assertEquals(emptyList(), zero.lines)
        assertEquals(0, zero.height)
        assertTrue(zero.truncated)
    }

    @Test
    fun invalidArgumentsAreRejected() {
        assertFailsWith<IllegalArgumentException> { TextMetrics(0) }
        assertFailsWith<IllegalArgumentException> { metrics.measure("a", -1) }
        assertFailsWith<IllegalArgumentException> { metrics.measure("a", 1, maxLines = -1) }
    }
}
