package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PopoverTest {
    @Test
    fun flipsAboveAndClampsAtScreenEdge() {
        val popover = Popover(Label("popover"), placement = OverlayPlacement.AUTO)
        val harness = GuiTestHarness(Button("root"), columns = 20, rows = 10)
        val entry = popover.show(harness.surface.overlays, Rect(18, 9, 1, 1))
        harness.paint()
        assertTrue(entry.screenRect.y < 9)
        assertTrue(entry.screenRect.right <= 20)
        assertEquals(13, entry.screenRect.x)
    }

    @Test
    fun escapeAndOutsideClickDismissAndRestoreFocus() {
        val focusTarget = Button("focus")
        val harness = GuiTestHarness(focusTarget, columns = 30, rows = 8)
        harness.paint()
        assertTrue(harness.surface.focus.focus(focusTarget))
        val popover = Popover(Label("tip"))
        popover.show(harness.surface.overlays, 2, 2)
        harness.paint()
        assertTrue(harness.key(Key.ESCAPE))
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        assertEquals(focusTarget, harness.surface.focus.focused)

        popover.show(harness.surface.overlays, 2, 2, dismissPolicy = DismissPolicy.CLICK_OUTSIDE)
        harness.paint()
        harness.click(25, 7)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun nonePolicyKeepsPopoverOnOutsideClickAndEscape() {
        val popover = Popover(Label("tip"), dismissPolicy = DismissPolicy.NONE)
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 8)
        popover.show(harness.surface.overlays, 2, 2)
        harness.paint()
        harness.click(25, 7)
        assertTrue(
            harness.surface.overlays.entries
                .isNotEmpty(),
        )
        harness.key(Key.ESCAPE)
        assertTrue(
            harness.surface.overlays.entries
                .isNotEmpty(),
        )
        popover.dismiss()
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }
}
