package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayConformanceTest {
    @Test
    fun overlaysPaintAtMinimumAndCommonSizes() {
        for ((columns, rows) in listOf(1 to 1, 80 to 24)) {
            val harness = GuiTestHarness(Button("root"), columns = columns, rows = rows)
            val menu = ContextMenu(listOf(MenuItem.CommandItem(Command("run", "Run", action = {}))))
            menu.show(harness.surface.overlays, 0, 0)
            harness.paint()
            assertGrid(harness, columns, rows)
            menu.close()
            val dialog = Dialog("Title", Label("Body"), listOf(DialogAction("OK", default = true)))
            dialog.show(harness.surface.overlays)
            harness.paint()
            assertGrid(harness, columns, rows)
            dialog.close()
            val popover = Popover(Label("tip"))
            popover.show(harness.surface.overlays, Rect(0, 0, 0, 0))
            harness.paint()
            assertGrid(harness, columns, rows)
            popover.dismiss()
            val tooltip = Tooltip("tip")
            harness.surface.overlays.add(tooltip, anchor = OverlayAnchor.Rect(Rect(0, 0, 0, 0)))
            harness.paint()
            assertGrid(harness, columns, rows)
            harness.surface.overlays.clear()
            val manager = ToastManager(harness.surface)
            manager.show("notice")
            harness.paint()
            assertGrid(harness, columns, rows)
            manager.dispose()
        }
    }

    private fun assertGrid(
        harness: GuiTestHarness,
        columns: Int,
        rows: Int,
    ) {
        val structure = harness.rows()
        assertEquals(rows, structure.size)
        assertTrue(structure.all { it.length == columns })
    }
}
