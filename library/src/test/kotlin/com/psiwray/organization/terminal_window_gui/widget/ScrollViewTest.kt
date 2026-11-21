package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrollViewTest {
    @Test
    fun contentIsMeasuredOnTheScrollAxisAndClipped() {
        val content = RowsWidget(4, 20)
        val model = ScrollModel()
        val view = ScrollView(model, child = content)
        val harness = GuiTestHarness(view, columns = 4, rows = 5)
        harness.paint()
        assertEquals(4, model.viewportWidth)
        assertEquals(5, model.viewportHeight)
        assertEquals(4, model.contentWidth)
        assertEquals(20, model.contentHeight)
        assertEquals("0000", harness.rows()[0])
        assertEquals("4444", harness.rows()[4])
    }

    @Test
    fun paintingAndPointerEventsShiftByTheScrollOffset() {
        val content = RowsWidget(4, 20)
        val model = ScrollModel()
        val view = ScrollView(model, child = content)
        val harness = GuiTestHarness(view, columns = 4, rows = 5)
        harness.paint()
        model.scrollTo(0, 3)
        harness.surface.requestPaint()
        harness.paint()
        assertEquals("3333", harness.rows()[0])
        assertEquals("7777", harness.rows()[4])
        harness.move(0, 0)
        assertEquals(3, content.lastLocalY)
    }

    @Test
    fun deepScrollStillHitTestsChildrenAtTheViewportOrigin() {
        val content = RowsWidget(4, 20)
        val view = ScrollView(child = content)
        val harness = GuiTestHarness(view, columns = 4, rows = 5)
        harness.paint()
        view.model.scrollTo(0, 12)
        harness.paint()
        harness.move(0, 0)
        assertEquals(12, content.lastLocalY)
    }

    @Test
    fun wheelAndKeyboardScrollTheModel() {
        val model = ScrollModel()
        val view = ScrollView(model, child = RowsWidget(4, 20))
        val harness = GuiTestHarness(view, columns = 4, rows = 5)
        harness.paint()
        harness.scroll(0, 2, 0, 0)
        assertEquals(2, model.offsetY)
        harness.paint()
        assertTrue(harness.surface.focus.focus(view))
        assertTrue(harness.key(Key.DOWN))
        assertEquals(3, model.offsetY)
        assertTrue(harness.key(Key.PAGE_DOWN))
        assertEquals(8, model.offsetY)
        assertTrue(harness.key(Key.HOME))
        assertEquals(0, model.offsetY)
        assertTrue(harness.key(Key.END))
        assertEquals(15, model.offsetY)
    }

    @Test
    fun tabFocusScrollsOffscreenWidgetsIntoView() {
        val buttons = (0 until 10).map { Button("B$it") }
        val view = ScrollView(Column(children = buttons))
        val harness = GuiTestHarness(view, columns = 10, rows = 5)
        harness.paint()

        repeat(7) { assertTrue(harness.key(Key.TAB)) }
        assertEquals(2, view.model.offsetY)
        assertTrue(harness.surface.needsPaint)
    }

    @Test
    fun nestedScrollViewsRevealTheFocusedWidget() {
        val buttons = (0 until 10).map { Button("B$it") }
        val inner = ScrollView(Column(children = buttons))
        val page =
            Column(
                children =
                    listOf(
                        SizedBox(height = 10),
                        SizedBox(height = 5, child = inner),
                    ),
            )
        val outer = ScrollView(page)
        val harness = GuiTestHarness(outer, columns = 10, rows = 6)
        harness.paint()

        repeat(7) { assertTrue(harness.key(Key.TAB)) }
        assertTrue(inner.model.offsetY > 0)
        assertTrue(outer.model.offsetY > 0)
    }

    @Test
    fun nestedScrollConsumesFirstThenChainsAtItsBoundary() {
        val inner = ScrollView(RowsWidget(4, 20))
        val page =
            Column(
                children =
                    listOf(
                        SizedBox(height = 10),
                        SizedBox(height = 4, child = inner),
                        SizedBox(height = 10),
                    ),
            )
        val outer = ScrollView(page)
        val harness = GuiTestHarness(outer, columns = 10, rows = 10)
        harness.paint()
        outer.model.scrollTo(0, 10)
        harness.paint()
        harness.scroll(0, 2, 1, 1)
        assertEquals(2, inner.model.offsetY)
        val outerBefore = outer.model.offsetY
        inner.model.scrollTo(0, inner.model.maxOffsetY)
        harness.scroll(0, 2, 1, 1)
        assertTrue(outer.model.offsetY > outerBefore)
    }

    @Test
    fun resizeUpdatesTheViewport() {
        val model = ScrollModel()
        val view = ScrollView(model, child = RowsWidget(4, 20))
        val harness = GuiTestHarness(view, columns = 4, rows = 5)
        harness.paint()
        harness.resize(6, 3)
        harness.paint()
        assertEquals(6, model.viewportWidth)
        assertEquals(3, model.viewportHeight)
        assertEquals(17, model.maxOffsetY)
    }
}
