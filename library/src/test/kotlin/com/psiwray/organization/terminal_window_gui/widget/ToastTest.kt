package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToastTest {
    @Test
    fun stacksToMaximumAndAutoDismisses() {
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        val manager = ToastManager(harness.surface, maxVisible = 3, defaultDurationMillis = 3000)
        manager.show("one")
        manager.show("two", Severity.WARNING)
        manager.show("three", Severity.ERROR)
        manager.show("four")
        harness.paint()
        assertEquals(3, manager.toasts.size)
        assertTrue(manager.toasts.none { it.message == "one" })
        assertEquals(1, harness.surface.overlays.entries.size)
        harness.advance(2999)
        assertEquals(3, manager.toasts.size)
        harness.advance(1)
        assertTrue(manager.toasts.isEmpty())
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        manager.dispose()
    }

    @Test
    fun manualCloseDismissesOneToast() {
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        val manager = ToastManager(harness.surface)
        val toast = manager.show("close me")
        harness.paint()
        val entry =
            harness.surface.overlays.entries
                .single()
        harness.click(entry.screenRect.x + toast.bounds.width - 1, entry.screenRect.y)
        assertTrue(manager.toasts.isEmpty())
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        manager.dispose()
    }

    @Test
    fun clearCancelsTimers() {
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        val manager = ToastManager(harness.surface)
        manager.show("a")
        manager.show("b")
        manager.clear()
        harness.advance(5000)
        assertTrue(manager.toasts.isEmpty())
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }
}
