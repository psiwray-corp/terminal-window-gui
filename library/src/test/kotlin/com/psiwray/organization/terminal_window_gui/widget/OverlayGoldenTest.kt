package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test

class OverlayGoldenTest {
    @Test
    fun menuBarOpenGolden() {
        val bar =
            MenuBar(
                Menu(
                    "File",
                    'F',
                    listOf(
                        MenuItem.CommandItem(Command("open", "Open", shortcut = KeyChord(Key.O, Modifiers.CONTROL)) {}),
                        MenuItem.CommandItem(Command("save", "Save", shortcut = KeyChord(Key.S, Modifiers.CONTROL)) {}),
                        MenuItem.Separator,
                        MenuItem.Submenu("More", listOf(MenuItem.CommandItem(Command("nested", "Nested") {}))),
                    ),
                ),
                Menu("Edit", 'E', emptyList()),
            )
        val harness = GuiTestHarness(bar, columns = 40, rows = 8)
        harness.paint()
        harness.key(Key.F10)
        harness.paint()
        GoldenAssert.assertRows("menu-bar-open-40x8.txt", harness.rows())
    }

    @Test
    fun dialogGolden() {
        val dialog =
            Dialog(
                "Confirm",
                Label("Delete this item?"),
                listOf(DialogAction("Cancel", cancel = true), DialogAction("Delete", default = true)),
            )
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 12)
        dialog.show(harness.surface.overlays)
        harness.paint()
        GoldenAssert.assertRows("dialog-40x12.txt", harness.rows())
    }

    @Test
    fun toastGolden() {
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        val manager = ToastManager(harness.surface)
        manager.show("Saved")
        manager.show("Disk almost full", Severity.WARNING)
        harness.paint()
        GoldenAssert.assertRows("toast-40x10.txt", harness.rows())
    }

    @Test
    fun tooltipGolden() {
        val target = Button("hover")
        val harness = GuiTestHarness(Column(children = listOf(target)), columns = 30, rows = 6)
        val controller = TooltipController(harness.surface, 0)
        controller.register(target, Tooltip("helpful tip", maxWidth = 12))
        harness.move(1, 0)
        harness.advance(0)
        harness.paint()
        GoldenAssert.assertRows("tooltip-30x6.txt", harness.rows())
    }

    @Test
    fun popoverGolden() {
        val popover =
            Popover(
                Label("popover"),
                placement = com.psiwray.organization.terminal_window_gui.input.OverlayPlacement.ABOVE,
            )
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 8)
        popover.show(harness.surface.overlays, OverlayAnchor.Rect(Rect(20, 7, 1, 1)))
        harness.paint()
        GoldenAssert.assertRows("popover-30x8.txt", harness.rows())
    }
}
