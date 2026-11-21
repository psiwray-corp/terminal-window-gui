package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextMenuTest {
    @Test
    fun opensAtCellRunsCommandAndRestoresFocus() {
        var runs = 0
        val menu = ContextMenu(listOf(MenuItem.CommandItem(Command("run", "Run") { runs++ })))
        val focusTarget = Button("focus")
        val harness = GuiTestHarness(focusTarget, columns = 30, rows = 8)
        harness.paint()
        assertTrue(harness.surface.focus.focus(focusTarget))
        val entry = menu.show(harness.surface.overlays, 5, 2)
        harness.paint()
        assertEquals(5, entry.screenRect.x)
        assertEquals(2, entry.screenRect.y)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, runs)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        assertEquals(focusTarget, harness.surface.focus.focused)
    }

    @Test
    fun directHostAddFocusesMenuForKeyboard() {
        var runs = 0
        val menu = ContextMenu(listOf(MenuItem.CommandItem(Command("run", "Run") { runs++ })))
        val harness = GuiTestHarness(Button("focus"), columns = 30, rows = 8)
        harness.paint()
        harness.surface.overlays.add(
            menu,
            anchor =
                com.psiwray.organization.terminal_window_gui.input.OverlayAnchor.Rect(
                    com.psiwray.organization.terminal_window_gui.layout
                        .Rect(1, 1, 1, 1),
                ),
        )
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, runs)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun mouseActivationRunsCommand() {
        var runs = 0
        val menu = ContextMenu(listOf(MenuItem.CommandItem(Command("run", "Run") { runs++ })))
        val harness = GuiTestHarness(Button("focus"), columns = 30, rows = 8)
        harness.paint()
        val entry = menu.show(harness.surface.overlays, 2, 2)
        harness.paint()
        assertTrue(harness.click(entry.screenRect.x + 2, entry.screenRect.y))
        assertEquals(1, runs)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun outsideClickDismissesOnlyTopmostNonModalLayer() {
        val menu = ContextMenu(listOf(MenuItem.CommandItem(Command("run", "Run") {})))
        val harness = GuiTestHarness(Button("focus"), columns = 30, rows = 8)
        harness.paint()
        menu.show(harness.surface.overlays, 2, 2, dismissPolicy = DismissPolicy.CLICK_OUTSIDE)
        harness.paint()
        assertTrue(
            harness.surface.overlays.entries
                .isNotEmpty(),
        )
        harness.click(25, 6)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }
}
