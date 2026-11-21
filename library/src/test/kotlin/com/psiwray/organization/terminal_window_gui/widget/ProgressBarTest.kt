package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressBarTest {
    @Test
    fun halfValueFillsHalfTheCells() {
        val harness = displayHarness(ProgressBar(value = 0.5), columns = 8, rows = 1)
        harness.paint()
        val full = harness.surface.shapes[Shape.PROGRESS_FULL]
        val empty = harness.surface.shapes[Shape.PROGRESS_EMPTY]
        var column = 0
        while (column < 4) {
            assertEquals(full, harness.matrix.codePointAt(column, 0))
            column++
        }
        while (column < 8) {
            assertEquals(empty, harness.matrix.codePointAt(column, 0))
            column++
        }
        GoldenAssert.assertRows("progress-half.txt", harness.rows())
    }

    @Test
    fun partialCellsUseEighthBlocks() {
        val harness = displayHarness(ProgressBar(value = 0.05), columns = 10, rows = 1)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.PROGRESS_1_2], harness.matrix.codePointAt(0, 0))
    }

    @Test
    fun asciiCatalogUsesHashFallback() {
        val harness =
            displayHarness(
                ProgressBar(value = 1.0),
                columns = 3,
                rows = 1,
                shapes = ShapeSets.ascii().resolve(GuiFonts.bundled()),
            )
        harness.paint()
        assertEquals('#'.code, harness.matrix.codePointAt(0, 0))
    }

    @Test
    fun percentageLabelIsOptional() {
        val harness = displayHarness(ProgressBar(value = 0.5, showPercent = true), columns = 8, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("%"))
    }

    @Test
    fun indeterminateRunsOnTheFakeClockAndStopsWhenDetached() {
        val bar = ProgressBar(indeterminate = true)
        val harness = displayHarness(bar, columns = 8, rows = 1)
        assertTrue(harness.advance(100))
        assertEquals(1, bar.frame)
        bar.detachTree()
        assertFalse(harness.advance(100))
    }

    @Test
    fun invalidValueIsRejected() {
        try {
            ProgressBar(value = 1.1)
            error("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
