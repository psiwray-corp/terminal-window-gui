package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TabsTest {
    @Test
    fun activeMarkerAndContentSwitch() {
        val first = TextField("first")
        val second = Label("second")
        val view =
            TabView(
                listOf(
                    Tab("one", "One", first),
                    Tab("two", "Two", second),
                ),
            )
        val harness = GuiTestHarness(view, columns = 30, rows = 5)
        harness.paint()
        assertTrue(harness.rows()[0].startsWith(">One< Two "))
        assertSame(first, view.content)
        assertTrue(harness.rows()[1].contains("first"))

        view.activeIndex = 1
        harness.paint()
        assertTrue(harness.rows()[0].startsWith(" One >Two<"))
        assertSame(second, view.content)
        assertTrue(harness.rows()[1].contains("second"))
        assertEquals(null, first.parent)
    }

    @Test
    fun inactiveContentIsNotMeasuredOrFocusable() {
        val counting = CountingWidget()
        val view =
            TabView(
                listOf(
                    Tab("one", "One", Label("one")),
                    Tab("two", "Two", counting),
                ),
            )
        val harness = GuiTestHarness(view, columns = 30, rows = 5)
        harness.paint()
        assertEquals(0, counting.measureCount)
        assertTrue(
            com.psiwray.organization.terminal_window_gui.input.FocusTraversal
                .order(view)
                .none { it === counting },
        )

        view.activeIndex = 1
        harness.paint()
        assertTrue(counting.measureCount > 0)
        assertEquals(
            1,
            com.psiwray.organization.terminal_window_gui.input.FocusTraversal.order(view).count {
                it ===
                    counting
            },
        )
    }

    @Test
    fun keyboardAndMouseActivateTabs() {
        val changed = ArrayList<Int>()
        val view =
            TabView(
                listOf(
                    Tab("one", "One", Label("one")),
                    Tab("two", "Two", Label("two")),
                    Tab("three", "Three", Label("three")),
                ),
                onChanged = { changed.add(it) },
            )
        val harness = GuiTestHarness(view, columns = 40, rows = 4)
        harness.paint()
        assertTrue(harness.surface.focus.focus(view.tabBar))
        assertTrue(harness.key(Key.RIGHT))
        assertEquals(1, view.activeIndex)
        assertEquals(listOf(1), changed)
        assertTrue(harness.click(12, 0))
        assertEquals(2, view.activeIndex)
        assertEquals(listOf(1, 2), changed)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(listOf(1, 2, 2), changed)
    }

    @Test
    fun disabledTabsAreSkipped() {
        val view =
            TabView(
                listOf(
                    Tab("one", "One", Label("one")),
                    Tab("two", "Two", Label("two"), enabled = false),
                    Tab("three", "Three", Label("three")),
                ),
            )
        val harness = GuiTestHarness(view, columns = 40, rows = 4)
        harness.paint()
        assertTrue(harness.surface.focus.focus(view.tabBar))
        assertTrue(harness.key(Key.RIGHT))
        assertEquals(2, view.activeIndex)
        harness.click(7, 0)
        assertEquals(2, view.activeIndex)
    }

    @Test
    fun overflowScrollsActiveTabIntoView() {
        val tabs = (0 until 10).map { Tab("k$it", "Tab$it", Label("body$it")) }
        val view = TabView(tabs, activeIndex = 9)
        val harness = GuiTestHarness(view, columns = 20, rows = 4)
        harness.paint()
        assertTrue(view.tabBar.scroll.maxOffsetX > 0)
        assertTrue(view.tabBar.scroll.offsetX > 0)
        assertEquals("Tab9", view.activeTab?.title)
        assertTrue(
            harness.rows()[0].contains(
                view.tabBar.scroll.offsetX
                    .toString(),
            ) || harness.rows()[0].isNotEmpty(),
        )
        assertTrue(harness.rows()[0].contains("Tab9"))

        view.tabBar.scroll.home()
        harness.paint()
        assertEquals(0, view.tabBar.scroll.offsetX)
        assertTrue(harness.click(19, 0))
        assertTrue(view.tabBar.scroll.offsetX > 0)
    }

    @Test
    fun focusIsSavedAndRestoredPerTab() {
        val first = TextField("first")
        val other = TextField("other")
        val view =
            TabView(
                listOf(
                    Tab("one", "One", first),
                    Tab("two", "Two", other),
                ),
            )
        val harness = GuiTestHarness(view, columns = 30, rows = 5)
        harness.paint()
        assertTrue(harness.surface.focus.focus(first))
        view.activeIndex = 1
        harness.paint()
        assertTrue(harness.surface.focus.focus(other))
        view.activeIndex = 0
        harness.paint()
        assertSame(first, harness.focusPath().last())
    }

    @Test
    fun mouseSwitchRestoresFocusFromPreviousTab() {
        val first = TextField("first")
        val view =
            TabView(
                listOf(
                    Tab("one", "One", first),
                    Tab("two", "Two", Label("two")),
                ),
            )
        val harness = GuiTestHarness(view, columns = 30, rows = 5)
        harness.paint()
        assertTrue(harness.surface.focus.focus(first))
        harness.click(7, 0)
        harness.paint()
        assertEquals(1, view.activeIndex)
        harness.click(1, 0)
        harness.paint()
        assertEquals(0, view.activeIndex)
        assertSame(first, harness.focusPath().last())
    }

    @Test
    fun ctrlTabCyclesFromContent() {
        val view =
            TabView(
                listOf(
                    Tab("one", "One", TextField("one")),
                    Tab("two", "Two", TextField("two")),
                ),
            )
        val harness = GuiTestHarness(view, columns = 30, rows = 5)
        harness.paint()
        assertTrue(harness.surface.focus.focusFirst())
        assertTrue(harness.key(Key.TAB, Modifiers.CONTROL))
        assertEquals(1, view.activeIndex)
    }

    @Test
    fun tabsAliasConstructsTabView() {
        val alias: Tabs = Tabs(listOf(Tab("one", "One", Label("one"))))
        val harness = GuiTestHarness(alias, columns = 20, rows = 3)
        harness.paint()
        assertTrue(harness.rows()[0].startsWith(">One<"))
    }

    @Test
    fun ctrlTabCyclesWithoutFocus() {
        val view =
            TabView(
                listOf(
                    Tab("one", "One", Label("one")),
                    Tab("two", "Two", Label("two")),
                ),
            )
        val harness = GuiTestHarness(view, columns = 20, rows = 3)
        harness.paint()
        assertTrue(harness.key(Key.TAB, Modifiers.CONTROL))
        assertEquals(1, view.activeIndex)
    }

    @Test
    fun tabsMatchGolden() {
        val view =
            TabView(
                listOf(
                    Tab("one", "One", Label("first tab")),
                    Tab("two", "Two", Label("second tab")),
                    Tab("three", "Three", Label("third tab")),
                ),
            )
        val harness = GuiTestHarness(view, columns = 40, rows = 5)
        harness.paint()
        GoldenAssert.assertRows("tabs-40x5.txt", harness.rows())
    }

    private class CountingWidget : Widget() {
        var measureCount: Int = 0
            private set

        init {
            focusable = true
        }

        override fun measure(constraints: Constraints): IntSize {
            measureCount++
            return constraints.constrain(IntSize(4, 1))
        }

        override fun paint(context: PaintContext) {}
    }
}
