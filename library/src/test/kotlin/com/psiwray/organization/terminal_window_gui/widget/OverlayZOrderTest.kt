package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayZOrderTest {
    @Test
    fun clickInsideTopOverlayDoesNotReachRootOrLowerLayer() {
        var rootRuns = 0
        var lowerRuns = 0
        var upperRuns = 0
        val root = Button("root") { rootRuns++ }
        val harness = GuiTestHarness(root, columns = 30, rows = 8)
        harness.paint()
        val lower = Popover(Button("lower") { lowerRuns++ })
        val upper = Popover(Button("upper") { upperRuns++ })
        lower.show(harness.surface.overlays, OverlayAnchor.Rect(Rect(2, 2, 8, 1)), dismissPolicy = DismissPolicy.NONE)
        upper.show(harness.surface.overlays, OverlayAnchor.Rect(Rect(2, 2, 8, 1)), dismissPolicy = DismissPolicy.NONE)
        harness.paint()
        harness.click(3, 3)
        assertEquals(0, rootRuns)
        assertEquals(0, lowerRuns)
        assertEquals(1, upperRuns)
    }

    @Test
    fun modalOverlayBlocksOutsideClicksAndRootKeys() {
        var rootRuns = 0
        val root = Button("root") { rootRuns++ }
        val harness = GuiTestHarness(root, columns = 30, rows = 8)
        harness.paint()
        val dialog = Dialog("Modal", Label("body"), listOf(DialogAction("OK")))
        dialog.show(harness.surface.overlays)
        harness.paint()
        harness.click(0, 0)
        assertTrue(
            harness.surface.overlays.entries
                .isNotEmpty(),
        )
        harness.key(com.psiwray.organization.terminal_window.input.Key.SPACE)
        assertEquals(0, rootRuns)
        dialog.close()
    }
}
