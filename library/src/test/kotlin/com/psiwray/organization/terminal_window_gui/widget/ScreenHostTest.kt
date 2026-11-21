package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.GridSize
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.command.Keymap
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScreenHostTest {
    @Test
    fun initialScreenEntersAndReportsTitle() {
        var entered = 0
        val screen = Screen("a", "Alpha", Label("alpha"), onEnter = { entered++ })
        val host = ScreenHost(listOf(screen), "a")
        val harness = GuiTestHarness(host, columns = 12, rows = 3)
        harness.paint()
        assertEquals(1, entered)
        assertSame(screen, host.current)
        assertEquals("Alpha", host.currentTitle)
        assertEquals("alpha", harness.rows()[0].trimEnd())
    }

    @Test
    fun pushAndPopSwitchLifecycleAndContent() {
        var aEnter = 0
        var aExit = 0
        var bEnter = 0
        var bExit = 0
        val a = Screen("a", "A", Label("aaa"), onEnter = { aEnter++ }, onExit = { aExit++ })
        val b = Screen("b", "B", Label("bbb"), onEnter = { bEnter++ }, onExit = { bExit++ })
        val host = ScreenHost(listOf(a, b), "a")
        val harness = GuiTestHarness(host, columns = 12, rows = 3)
        harness.paint()
        assertEquals(1, aEnter)
        host.push(b)
        harness.paint()
        assertEquals(1, aExit)
        assertEquals(1, bEnter)
        assertEquals("B", host.currentTitle)
        assertEquals("bbb", harness.rows()[0].trimEnd())
        assertEquals(null, a.content.parent)
        assertFalse(a.content.visible && a.content.parent != null)

        assertTrue(host.pop())
        harness.paint()
        assertEquals(1, bExit)
        assertEquals(2, aEnter)
        assertEquals("aaa", harness.rows()[0].trimEnd())
        assertFalse(host.pop())
    }

    @Test
    fun popRestoresSavedFocus() {
        val aField = TextField("a")
        val bField = TextField("b")
        val a = Screen("a", "A", aField)
        val b = Screen("b", "B", bField)
        val host = ScreenHost(listOf(a, b), "a")
        val harness = GuiTestHarness(host, columns = 20, rows = 4)
        harness.paint()
        assertTrue(harness.surface.focus.focus(aField))
        host.push(b)
        harness.paint()
        assertTrue(harness.surface.focus.focus(bField))
        assertEquals(bField, harness.focusPath().last())
        assertTrue(host.pop())
        harness.paint()
        assertSame(aField, harness.focusPath().last())
    }

    @Test
    fun rejectedBackDoesNotPopAndAcceptedBackPops() {
        val a = Screen("a", "A", Label("a"))
        val b = Screen("b", "B", Label("b"), onBack = { false })
        val host = ScreenHost(listOf(a, b), "a")
        val harness = GuiTestHarness(host, columns = 10, rows = 3)
        host.push(b)
        harness.paint()
        harness.key(Key.ESCAPE)
        assertSame(b, host.current)
        b.onBack = { true }
        harness.key(Key.ESCAPE)
        assertSame(a, host.current)
    }

    @Test
    fun resizeCallbackRunsBeforeContentLayout() {
        val sizes = ArrayList<GridSize>()
        val content = Label("x")
        val screen = Screen("a", "A", content, onResize = { sizes.add(it) })
        val host = ScreenHost(listOf(screen), "a")
        val harness = GuiTestHarness(host, columns = 20, rows = 5)
        harness.paint()
        assertTrue(sizes.isNotEmpty())
        assertEquals(GridSize(20, 5), sizes.last())
        harness.resize(40, 10)
        harness.paint()
        assertEquals(GridSize(40, 10), sizes.last())
        assertEquals(40, content.bounds.width)
        assertEquals(10, content.bounds.height)
    }

    @Test
    fun activeScreenKeymapExecutesAndInactiveDoesNot() {
        var executed = 0
        val keymap =
            Keymap().bind(
                KeyChord(Key.S, Modifiers.CONTROL),
                Command(id = "save") { executed++ },
            )
        val a = Screen("a", "A", Label("a"))
        val b = Screen("b", "B", Label("b"), keymap = keymap)
        val host = ScreenHost(listOf(a, b), "a")
        val harness = GuiTestHarness(host, columns = 10, rows = 3)
        harness.key(Key.S, Modifiers.CONTROL)
        assertEquals(0, executed)
        host.push(b)
        harness.paint()
        assertTrue(harness.key(Key.S, Modifiers.CONTROL))
        assertEquals(1, executed)
        assertSame(keymap, host.currentKeymap)
    }

    @Test
    fun screenAcceptsCommandMapAndTwoArgumentResize() {
        val screen =
            Screen(
                "a",
                "A",
                Label("a"),
                mapOf(KeyChord(Key.S, Modifiers.CONTROL) to Command(id = "save") {}),
            )
        val host = ScreenHost(listOf(screen), "a")
        val harness = GuiTestHarness(host, columns = 10, rows = 3)
        harness.paint()
        assertTrue(harness.key(Key.S, Modifiers.CONTROL))
    }

    @Test
    fun replaceKeepsStackDepthAndLifecycle() {
        var bExit = 0
        var cEnter = 0
        val a = Screen("a", "A", Label("a"))
        val b = Screen("b", "B", Label("b"), onExit = { bExit++ })
        val c = Screen("c", "C", Label("ccc"), onEnter = { cEnter++ })
        val host = ScreenHost(listOf(a, b, c), "a")
        val harness = GuiTestHarness(host, columns = 10, rows = 3)
        host.push(b)
        host.replace(c)
        harness.paint()
        assertEquals(2, host.stack.size)
        assertSame(c, host.current)
        assertEquals(1, bExit)
        assertEquals(1, cEnter)
        assertEquals("ccc", harness.rows()[0].trimEnd())
    }

    @Test
    fun missingInitialKeyFallsBackToFirstAndOneCellWorks() {
        val a = Screen("a", "A", Label("a"))
        val b = Screen("b", "B", Label("b"))
        val host = ScreenHost(listOf(a, b), "missing")
        assertSame(a, host.current)
        val harness = GuiTestHarness(host, columns = 1, rows = 1)
        harness.paint()
        assertEquals(1, harness.rows()[0].length)
    }

    @Test
    fun screenHostMatchesGolden() {
        val a = Screen("a", "A", Label("alpha"))
        val b = Screen("b", "B", Label("beta"))
        val host = ScreenHost(listOf(a, b), "b")
        val harness = GuiTestHarness(host, columns = 20, rows = 3)
        harness.paint()
        GoldenAssert.assertRows("screen-host-20x3.txt", harness.rows())
    }
}
