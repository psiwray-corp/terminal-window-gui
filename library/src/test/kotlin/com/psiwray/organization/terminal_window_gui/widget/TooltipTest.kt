package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TooltipTest {
    @Test
    fun showsAfterDelayAndHidesOnMoveAway() {
        val target = Button("hover")
        val tooltip = Tooltip("helpful tip")
        val harness = GuiTestHarness(Column(children = listOf(target)), columns = 30, rows = 8)
        harness.paint()
        val controller = TooltipController(harness.surface, 600)
        controller.register(target, tooltip)
        harness.move(1, 0)
        harness.advance(599)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        harness.advance(1)
        harness.paint()
        assertEquals(1, harness.surface.overlays.entries.size)
        assertTrue(harness.rows().any { it.contains("helpful tip") })
        harness.move(10, 7)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        controller.dispose()
    }

    @Test
    fun clickOutsideHidesTooltip() {
        val target = Button("hover")
        val harness = GuiTestHarness(Column(children = listOf(target)), columns = 20, rows = 6)
        val controller = TooltipController(harness.surface, 0)
        controller.register(target, Tooltip("tip"))
        harness.move(1, 0)
        harness.advance(0)
        harness.paint()
        assertEquals(1, harness.surface.overlays.entries.size)
        harness.click(19, 5)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        controller.dispose()
    }

    @Test
    fun focusLossHidesTooltip() {
        val first = Button("first")
        val second = Button("second")
        val harness = GuiTestHarness(Column(children = listOf(first, second)), columns = 20, rows = 5)
        harness.paint()
        val controller = TooltipController(harness.surface, 600)
        controller.register(first, Tooltip("tip"))
        assertTrue(harness.surface.focus.focus(first))
        harness.advance(600)
        assertEquals(1, harness.surface.overlays.entries.size)
        assertTrue(harness.surface.focus.focus(second))
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        controller.dispose()
    }

    @Test
    fun wrapsAtMaxWidthAndFlipsAtBottom() {
        val target = Button("x")
        val tooltip = Tooltip("alpha beta gamma delta", maxWidth = 8)
        val harness = GuiTestHarness(target, columns = 20, rows = 6)
        harness.paint()
        val controller = TooltipController(harness.surface, 0)
        controller.register(target, tooltip)
        harness.move(19, 5)
        harness.advance(0)
        harness.paint()
        val entry =
            harness.surface.overlays.entries
                .last()
        assertTrue(entry.screenRect.width <= 8)
        assertTrue(entry.screenRect.height > 1)
        assertTrue(entry.screenRect.bottom <= 6)
        controller.dispose()
    }

    @Test
    fun onlyOneTooltipExistsAtATime() {
        val first = Button("first")
        val second = Button("second")
        val harness = GuiTestHarness(Column(children = listOf(first, second)), columns = 20, rows = 5)
        harness.paint()
        val controller = TooltipController(harness.surface, 0)
        controller.register(first, Tooltip("one"))
        controller.register(second, Tooltip("two"))
        harness.move(1, 0)
        harness.advance(0)
        harness.paint()
        assertEquals(1, harness.surface.overlays.entries.size)
        assertTrue(harness.rows().any { it.contains("one") })
        harness.move(5, 1)
        harness.advance(0)
        harness.paint()
        assertEquals(1, harness.surface.overlays.entries.size)
        assertTrue(harness.rows().any { it.contains("two") })
        controller.dispose()
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }
}
