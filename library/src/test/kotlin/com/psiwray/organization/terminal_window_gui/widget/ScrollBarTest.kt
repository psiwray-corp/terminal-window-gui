package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.internal.ScrollThumb
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrollBarTest {
    @Test
    fun thumbLengthAndPositionCoverTheRange() {
        assertEquals(10, ScrollThumb.length(10, 10, 10))
        assertEquals(5, ScrollThumb.length(10, 20, 10))
        assertEquals(0, ScrollThumb.position(0, 10, 10, 5))
        assertEquals(3, ScrollThumb.position(5, 10, 10, 5))
        assertEquals(5, ScrollThumb.position(10, 10, 10, 5))
        assertEquals(0, ScrollThumb.offsetForPosition(0, 10, 10, 5))
        assertEquals(6, ScrollThumb.offsetForPosition(3, 10, 10, 5))
        assertEquals(10, ScrollThumb.offsetForPosition(5, 10, 10, 5))
    }

    @Test
    fun autoHidesWhenContentFitsAndNeverAlwaysHide() {
        val fits = ScrollModel(contentHeight = 5, viewportHeight = 5)
        val auto = ScrollBar(fits)
        assertTrue(auto.hidden)
        assertFalse(auto.shown)

        val always = ScrollBar(fits, visibility = ScrollBarVisibility.ALWAYS)
        assertFalse(always.hidden)

        val never =
            ScrollBar(ScrollModel(contentHeight = 20, viewportHeight = 5), visibility = ScrollBarVisibility.NEVER)
        assertTrue(never.hidden)
    }

    @Test
    fun arrowsPageAndKeyboardChangeTheModel() {
        val model = ScrollModel(contentHeight = 100, viewportHeight = 10)
        val bar = ScrollBar(model)
        val harness = GuiTestHarness(bar, columns = 1, rows = 10)
        harness.paint()
        assertEquals(1, bar.bounds.width)
        assertEquals(10, bar.bounds.height)
        GoldenAssert.assertRows("scrollbar-vertical.txt", harness.rows())

        model.scrollTo(0, 5)
        harness.click(0, 9)
        assertEquals(6, model.offsetY)
        harness.click(0, 0)
        assertEquals(5, model.offsetY)

        harness.click(0, 8)
        assertEquals(15, model.offsetY)

        assertTrue(harness.surface.focus.focus(bar))
        assertTrue(harness.key(Key.DOWN))
        assertEquals(16, model.offsetY)
        assertTrue(harness.key(Key.PAGE_UP))
        assertEquals(6, model.offsetY)
    }

    @Test
    fun thumbDragMapsCellMovementToOffset() {
        val model = ScrollModel(contentHeight = 100, viewportHeight = 10)
        val bar = ScrollBar(model)
        val harness = GuiTestHarness(bar, columns = 1, rows = 10)
        harness.paint()
        assertTrue(harness.drag(0, 1, 0, 5))
        assertTrue(model.offsetY > 0)
    }
}
