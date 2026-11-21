package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import kotlin.test.Test
import kotlin.test.assertTrue

class SeparatorTest {
    @Test
    fun horizontalRuleMatchesGolden() {
        val harness = displayHarness(Separator(), columns = 5, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("separator-horizontal.txt", harness.rows())
    }

    @Test
    fun horizontalLabelIsCentered() {
        val harness = displayHarness(Separator(label = "x"), columns = 5, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("separator-labeled.txt", harness.rows())
        assertTrue(harness.rows()[0].contains("x"))
    }

    @Test
    fun verticalAndAsciiRulesRender() {
        val vertical = displayHarness(Separator(orientation = Orientation.VERTICAL), columns = 1, rows = 3)
        vertical.paint()
        GoldenAssert.assertRows("separator-vertical.txt", vertical.rows())
        val ascii =
            displayHarness(
                Separator(),
                columns = 5,
                rows = 1,
                shapes = ShapeSets.ascii().resolve(GuiFonts.bundled()),
            )
        ascii.paint()
        GoldenAssert.assertRows("separator-ascii.txt", ascii.rows())
    }

    @Test
    fun oneByOneSeparatorIsValid() {
        val harness = displayHarness(Separator(), columns = 1, rows = 1)
        harness.paint()
        assertTrue(harness.rows().single().isNotEmpty())
    }
}
