package com.psiwray.organization.terminal_window_gui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstraintTest {
    @Test
    fun invalidRangesNormalize() {
        val constraints = Constraints(10, 5, -2, -3)
        assertEquals(10, constraints.minWidth)
        assertEquals(10, constraints.maxWidth)
        assertEquals(0, constraints.minHeight)
        assertEquals(0, constraints.maxHeight)
    }

    @Test
    fun constrainClampsBothAxes() {
        val constraints = Constraints(2, 5, 3, 7)
        assertEquals(IntSize(2, 3), constraints.constrain(IntSize(0, 0)))
        assertEquals(IntSize(5, 7), constraints.constrain(IntSize(99, 99)))
        assertEquals(IntSize(4, 6), constraints.constrain(IntSize(4, 6)))
    }

    @Test
    fun loosenTightenAndBounded() {
        val constraints = Constraints(2, 5, 3, 7)
        assertEquals(Constraints(0, 5, 0, 7), constraints.loosen())
        assertEquals(Constraints(5, 5, 7, 7), constraints.tighten())
        assertEquals(Constraints(4, 4, 6, 6), constraints.tighten(IntSize(4, 6)))
        assertTrue(constraints.hasBoundedWidth)
        assertTrue(constraints.hasBoundedHeight)
        assertFalse(Constraints.unbounded().hasBoundedWidth)
        assertFalse(Constraints.unbounded().hasBoundedHeight)
        assertEquals(Constraints(0, 80, 0, 24), Constraints.loose(IntSize(80, 24)))
        assertEquals(Constraints(80, 80, 24, 24), Constraints.tight(IntSize(80, 24)))
    }

    @Test
    fun copyAndEqualityAreValueBased() {
        val constraints = Constraints(1, 2, 3, 4)
        assertEquals(constraints, constraints.copy())
        assertEquals(Constraints(0, 2, 3, 4), constraints.copy(minWidth = 0))
        assertEquals(constraints.hashCode(), Constraints(1, 2, 3, 4).hashCode())
    }
}
