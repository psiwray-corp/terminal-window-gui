package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppShellTest {
    @Test
    fun menuContentAndStatusOccupyExpectedRows() {
        val menu = Label("M")
        val content = Label("C")
        val status = Label("S")
        val shell = AppShell(menu, content, status)
        val harness = GuiTestHarness(shell, columns = 10, rows = 5)
        harness.paint()
        assertEquals(0, menu.bounds.y)
        assertEquals(1, menu.bounds.height)
        assertEquals(1, content.bounds.y)
        assertEquals(3, content.bounds.height)
        assertEquals(4, status.bounds.y)
        assertEquals(1, status.bounds.height)
        assertEquals("M         ", harness.rows()[0])
        assertEquals("S         ", harness.rows()[4])
    }

    @Test
    fun contentBackgroundUsesSurfaceNotTheRootBackground() {
        val shell = AppShell(content = Label("C"))
        val harness = GuiTestHarness(shell, columns = 10, rows = 4)
        harness.paint()
        assertEquals(harness.surface.theme.surface, harness.matrix.backgroundAt(5, 1))
    }

    @Test
    fun optionalBarsReleaseRows() {
        val content = Label("C")
        val shell = AppShell(content = content)
        val harness = GuiTestHarness(shell, columns = 10, rows = 4)
        harness.paint()
        assertEquals(0, content.bounds.y)
        assertEquals(4, content.bounds.height)

        val status = Label("S")
        shell.statusBar = status
        harness.paint()
        assertEquals(0, content.bounds.y)
        assertEquals(3, content.bounds.height)
        assertEquals(3, status.bounds.y)
        assertEquals(1, status.bounds.height)
    }

    @Test
    fun settingMenuReplacesChildren() {
        val content = Label("C")
        val oldMenu = Label("A")
        val newMenu = Label("B")
        val shell = AppShell(oldMenu, content)
        val harness = GuiTestHarness(shell, columns = 10, rows = 4)
        harness.paint()
        shell.menuBar = newMenu
        harness.paint()
        assertSame(newMenu, shell.menuBar)
        assertEquals(null, oldMenu.parent)
        assertEquals(0, newMenu.bounds.y)
        assertEquals(1, content.bounds.y)
    }

    @Test
    fun oneCellShellDoesNotCrash() {
        val menu = Label("M")
        val content = Label("C")
        val status = Label("S")
        val shell = AppShell(menu, content, status)
        val harness = GuiTestHarness(shell, columns = 1, rows = 1)
        harness.paint()
        assertEquals(1, harness.rows()[0].length)
        assertEquals(0, menu.bounds.y)
        assertEquals(0, content.bounds.height)
    }

    @Test
    fun appShellMatchesGolden() {
        val shell = AppShell(Label("File Edit"), Label("content"), Label("ready"))
        val harness = GuiTestHarness(shell, columns = 20, rows = 4)
        harness.paint()
        GoldenAssert.assertRows("app-shell-20x4.txt", harness.rows())
    }
}
