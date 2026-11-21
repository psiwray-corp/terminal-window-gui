package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MenuPopupTest {
    @Test
    fun submenuOpensAndClosesIndependently() {
        val popup =
            MenuPopup(
                listOf(
                    MenuItem.CommandItem(Command("first", "First") {}),
                    MenuItem.Submenu("More", listOf(MenuItem.CommandItem(Command("nested", "Nested") {}))),
                ),
            )
        val bar = MenuBar(emptyList())
        val harness = GuiTestHarness(bar, columns = 30, rows = 10)
        // The standalone popup is opened directly so this test does not depend
        // on menubar activation.
        popup.open(harness.surface.overlays, OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        popup.focusLastItem()
        harness.paint()
        assertTrue(harness.key(Key.RIGHT))
        assertTrue(popup.submenu != null)
        assertEquals(2, harness.surface.overlays.entries.size)
        assertTrue(harness.key(Key.ESCAPE))
        assertTrue(popup.submenu == null)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun popupScrollKeepsFocusedItemVisible() {
        val items = (0 until 20).map { MenuItem.CommandItem(Command("item$it", "Item $it") {}) }
        val popup = MenuPopup(items)
        val harness = GuiTestHarness(Button("root"), columns = 20, rows = 5)
        popup.open(harness.surface.overlays, OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        popup.focusLastItem()
        harness.paint()
        assertTrue(popup.focusedIndex >= popup.scrollOffset)
        assertTrue(popup.focusedIndex < popup.scrollOffset + 3)
        assertEquals(1, harness.surface.overlays.entries.size)
    }

    @Test
    fun submenuCommandClosesWholeMenuAndRunsOnce() {
        var runs = 0
        val popup =
            MenuPopup(
                listOf(
                    MenuItem.Submenu(
                        "More",
                        listOf(MenuItem.CommandItem(Command("nested", "Nested") { runs++ })),
                    ),
                ),
            )
        val harness = GuiTestHarness(Button("root"), columns = 20, rows = 8)
        popup.open(harness.surface.overlays, OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        popup.focusFirstItem()
        assertTrue(harness.key(Key.RIGHT))
        assertTrue(popup.submenu != null)
        assertTrue(
            popup.submenu!!
                .entry!!
                .screenRect.right <= 20,
        )
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, runs)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun commandWithoutShortcutFitsItsLabel() {
        val popup = MenuPopup(listOf(MenuItem.CommandItem(Command("help", "Keyboard help") {})))
        val harness = GuiTestHarness(Button("root"), columns = 20, rows = 3)
        popup.open(harness.surface.overlays, OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        harness.paint()
        assertTrue(harness.rows().any { it.contains("Keyboard help") })
    }

    @Test
    fun disabledCommandDoesNotRun() {
        var runs = 0
        val command = Command("disabled", "Disabled", enabled = { false }) { runs++ }
        val popup = MenuPopup(listOf(MenuItem.CommandItem(command)))
        val harness = GuiTestHarness(Button("root"), columns = 20, rows = 5)
        popup.open(harness.surface.overlays, OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        assertTrue(!popup.activateFocused())
        assertEquals(0, runs)
    }
}
