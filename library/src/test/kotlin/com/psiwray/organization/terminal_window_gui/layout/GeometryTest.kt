package com.psiwray.organization.terminal_window_gui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryTest {
    @Test
    fun intSizeRejectsNegativeDimensions() {
        assertFailsWith<IllegalArgumentException> { IntSize(-1, 0) }
        assertFailsWith<IllegalArgumentException> { IntSize(0, -1) }
        assertEquals(IntSize(0, 0), IntSize.ZERO)
    }

    @Test
    fun rectUsesHalfOpenBounds() {
        val rect = Rect(2, 3, 4, 5)
        assertEquals(6, rect.right)
        assertEquals(8, rect.bottom)
        assertTrue(rect.contains(2, 3))
        assertTrue(rect.contains(5, 7))
        assertFalse(rect.contains(6, 3))
        assertFalse(rect.contains(2, 8))
        assertFalse(rect.contains(1, 3))
    }

    @Test
    fun rectIntersectionAndEmpty() {
        assertEquals(Rect(2, 2, 2, 2), Rect(0, 0, 4, 4).intersect(Rect(2, 2, 4, 4)))
        assertEquals(Rect.EMPTY, Rect(0, 0, 2, 2).intersect(Rect(3, 3, 2, 2)))
        assertTrue(Rect.EMPTY.isEmpty)
        assertFalse(Rect(0, 0, 1, 1).isEmpty)
    }

    @Test
    fun rectInsetAndLocalToParent() {
        assertEquals(Rect(1, 1, 8, 8), Rect(0, 0, 10, 10).inset(1))
        assertEquals(Rect(2, 3, 7, 6), Rect(1, 2, 10, 10).inset(1, 1, 2, 3))
        assertEquals(Rect(2, 2, 0, 0), Rect(0, 0, 1, 1).inset(2))
        assertEquals(Rect(11, 21, 2, 3), Rect(1, 1, 2, 3).localToParent(10, 20))
        assertEquals(Rect(11, 21, 2, 3), Rect(1, 1, 2, 3).localToParent(Rect(10, 20, 80, 24)))
        assertFailsWith<IllegalArgumentException> { Rect(0, 0, 1, 1).inset(-1) }
    }
}
