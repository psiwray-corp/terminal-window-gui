package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimumSizeTest {
    @Test
    fun everyControlPaintsInsideAOneCellSurface() {
        val controls =
            listOf<Widget>(
                Button("Long"),
                Checkbox("Long"),
                RadioGroup(children = listOf(RadioButton("a", "Long"))),
                Switch("Long"),
                Toolbar(listOf(Button("Long"))),
            )
        for (control in controls) {
            val harness = GuiTestHarness(control, columns = 1, rows = 1)
            harness.paint()
            assertEquals(1, harness.rows()[0].length)
        }
    }

    @Test
    fun asciiShapesPaintAllToggleFamilies() {
        val shapes = ShapeSets.ascii().resolve(GuiFonts.bundled())
        val controls =
            listOf<Widget>(
                Checkbox("On", CheckState.CHECKED),
                RadioGroup(children = listOf(RadioButton("a", "On"))),
                Switch("On", checked = true),
                Toolbar(listOf(Button("Run"), Separator(Orientation.VERTICAL))),
            )
        for (control in controls) {
            val harness = GuiTestHarness(control, shapes = shapes, columns = 12, rows = 1)
            harness.paint()
            assertEquals(12, harness.rows()[0].length)
        }
    }
}
