package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BadgeTest {
    @Test
    fun badgeMatchesGolden() {
        val harness = displayHarness(Badge("NEW"), columns = 3, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("badge-new.txt", harness.rows())
        assertEquals("NEW", harness.rows()[0])
    }

    @Test
    fun narrowBadgeTruncatesWithEllipsis() {
        val harness = displayHarness(Badge("NEWS"), columns = 3, rows = 1)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.ELLIPSIS], harness.matrix.codePointAt(2, 0))
    }

    @Test
    fun explicitColorsApply() {
        val foreground = Colors.rgb(10, 20, 30)
        val background = Colors.rgb(100, 110, 120)
        val harness =
            displayHarness(Badge("x", background = background, foreground = foreground), columns = 1, rows = 1)
        harness.paint()
        assertEquals(foreground, harness.matrix.foregroundAt(0, 0))
        assertEquals(background, harness.matrix.backgroundAt(0, 0))
    }

    @Test
    fun oneByOneBadgeIsValid() {
        val harness = displayHarness(Badge("x"), columns = 1, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].isNotEmpty())
    }
}
