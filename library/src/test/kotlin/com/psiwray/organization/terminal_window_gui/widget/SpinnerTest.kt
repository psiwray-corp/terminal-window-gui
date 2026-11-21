package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpinnerTest {
    @Test
    fun framesAdvanceWithFakeClockAndStopOnDetach() {
        val spinner = Spinner()
        val harness = GuiTestHarness(spinner, columns = 1, rows = 1)
        spinner.start()
        assertTrue(harness.advance(100))
        assertEquals(1, spinner.frame)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.SPINNER_1], harness.matrix.codePointAt(0, 0))
        spinner.detachTree()
        assertFalse(harness.advance(100))
    }

    @Test
    fun everyFrameAdvancesAndWrapsAfterFourSteps() {
        val spinner = Spinner()
        val harness = GuiTestHarness(spinner, columns = 1, rows = 1)
        val seen = ArrayList<Int>()
        var step = 0
        while (step < 4) {
            assertTrue(harness.advance(100))
            harness.paint()
            seen.add(harness.matrix.codePointAt(0, 0))
            step++
        }
        assertEquals(4, seen.toSet().size)
        assertEquals(0, spinner.frame % 4)
    }

    @Test
    fun explicitFrameSelectsASpinShape() {
        val spinner = Spinner(frame = 2)
        val harness = GuiTestHarness(spinner, columns = 1, rows = 1)
        harness.paint()
        assertEquals(harness.surface.shapes[Shape.SPINNER_2], harness.matrix.codePointAt(0, 0))
        GoldenAssert.assertRows("spinner-2.txt", harness.rows())
    }
}
