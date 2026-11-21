package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MenuBarTest {
    @Test
    fun f10OpensAndEscapeRestoresFocus() {
        var runs = 0
        val command = Command("open", "Open", shortcut = KeyChord(Key.O, Modifiers.CONTROL)) { runs++ }
        val bar = MenuBar(listOf(Menu("File", 'F', listOf(MenuItem.CommandItem(command)))))
        val content = Button("content")
        val shell = AppShell(menuBar = bar, content = content)
        val harness = GuiTestHarness(shell, columns = 40, rows = 10)
        harness.paint()
        assertTrue(harness.surface.focus.focus(content))
        assertTrue(harness.key(Key.F10))
        harness.paint()
        assertEquals(0, bar.activeIndex)
        assertEquals(1, harness.surface.overlays.entries.size)
        assertTrue(
            harness.surface.focus.focused
                ?.let { it === bar.popupMenu } == true,
        )
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(-1, bar.activeIndex)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
        assertEquals(content, harness.surface.focus.focused)
        assertEquals(0, runs)
    }

    @Test
    fun mnemonicOpensAndCommandRunsExactlyOnce() {
        var runs = 0
        val command = Command("open", "Open") { runs++ }
        val bar = MenuBar(listOf(Menu("File", 'F', listOf(MenuItem.CommandItem(command)))))
        val harness = GuiTestHarness(bar, columns = 30, rows = 8)
        harness.paint()
        assertTrue(harness.key(Key.F, Modifiers.ALT))
        harness.paint()
        assertEquals(0, bar.activeIndex)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, runs)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun leftRightSwitchOpenMenusAndDisabledItemsAreSkipped() {
        var fileRuns = 0
        var editRuns = 0
        val disabled = Command("disabled", "Disabled", enabled = { false }) { fileRuns++ }
        val first = Command("first", "First") { fileRuns++ }
        val bar =
            MenuBar(
                listOf(
                    Menu("File", 'F', listOf(MenuItem.CommandItem(disabled), MenuItem.CommandItem(first))),
                    Menu("Edit", 'E', listOf(MenuItem.CommandItem(Command("edit", "Edit") { editRuns++ }))),
                ),
            )
        val harness = GuiTestHarness(bar, columns = 40, rows = 8)
        harness.paint()
        assertTrue(harness.key(Key.F10))
        assertTrue(harness.key(Key.RIGHT))
        assertEquals(1, bar.activeIndex)
        assertTrue(harness.key(Key.LEFT))
        assertEquals(0, bar.activeIndex)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, fileRuns)
        assertEquals(0, editRuns)
    }

    @Test
    fun mouseClickOpensAndHoverSwitchesMenu() {
        val bar = MenuBar(listOf(Menu("File"), Menu("Edit"), Menu("View")))
        val harness = GuiTestHarness(bar, columns = 30, rows = 6)
        harness.paint()
        assertTrue(harness.click(2, 0))
        assertEquals(0, bar.activeIndex)
        harness.move(8, 0)
        assertEquals(1, bar.activeIndex)
        harness.move(14, 0)
        assertEquals(2, bar.activeIndex)
    }

    @Test
    fun clickingAnotherTitleWhileOpenSwitchesMenus() {
        val bar = MenuBar(listOf(Menu("File"), Menu("Edit")))
        val harness = GuiTestHarness(bar, columns = 30, rows = 6)
        harness.paint()
        assertTrue(harness.click(2, 0))
        assertEquals(0, bar.activeIndex)
        assertTrue(harness.move(8, 0))
        assertEquals(1, bar.activeIndex)
    }

    @Test
    fun popupShowsMarkersShortcutsAndScrollsForTallMenus() {
        val checked = Command("checked", "Checked", checked = { true }) {}
        val radio = Command("radio", "Radio", checked = { true }) {}
        val submenu = MenuItem.Submenu("More", listOf(MenuItem.CommandItem(Command("nested", "Nested") {})))
        val items =
            listOf(
                MenuItem.CommandItem(checked),
                MenuItem.CommandItem(radio, radio = true),
                MenuItem.Separator,
                submenu,
            ) +
                (0 until 8).map {
                    MenuItem.CommandItem(
                        Command("item$it", "Item $it", shortcut = KeyChord(Key.A), action = {}),
                    )
                }
        val popup = MenuPopup(items)
        val harness = GuiTestHarness(Button("root"), columns = 24, rows = 6)
        val entry = popup.open(harness.surface.overlays, anchor = OverlayAnchor.Rect(Rect(0, 0, 1, 1)))
        harness.paint()
        assertTrue(entry.screenRect.height <= 6)
        val rows = harness.rows()
        assertTrue(rows.any { it.contains("Checked") })
        assertTrue(popup.scrollOffset >= 0)
        assertTrue(popup.focusedIndex >= 0)
    }
}
