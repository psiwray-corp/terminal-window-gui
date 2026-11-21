package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitPaneTest {
    @Test
    fun horizontalSplitAllocatesOneDividerCell() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second)
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        assertTrue(pane.dividerVisible)
        assertEquals(10, pane.dividerPosition)
        assertEquals(10, first.bounds.width)
        assertEquals(1, second.bounds.x - first.bounds.right)
        assertEquals(9, second.bounds.width)
        assertEquals(5, first.bounds.height)
    }

    @Test
    fun verticalSplitStacksPanes() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.VERTICAL, first, second)
        val harness = GuiTestHarness(pane, columns = 20, rows = 10)
        harness.paint()
        assertTrue(pane.dividerVisible)
        assertEquals(5, pane.dividerPosition)
        assertEquals(5, first.bounds.height)
        assertEquals(6, second.bounds.y)
        assertEquals(4, second.bounds.height)
    }

    @Test
    fun minimaClampRatioAtBothEnds() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second, ratio = 0.0, minFirst = 3, minSecond = 4)
        val harness = GuiTestHarness(pane, columns = 20, rows = 4)
        harness.paint()
        assertEquals(3, first.bounds.width)
        assertEquals(16, second.bounds.width)

        pane.ratio = 1.0
        harness.paint()
        assertEquals(15, first.bounds.width)
        assertEquals(4, second.bounds.width)
    }

    @Test
    fun tinySplitHidesDividerWithoutNegativePanes() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second)
        val harness = GuiTestHarness(pane, columns = 2, rows = 2)
        harness.paint()
        assertFalse(pane.dividerVisible)
        assertEquals(0, second.bounds.x - first.bounds.right)
        assertTrue(first.bounds.width >= 0)
        assertTrue(second.bounds.width >= 0)
        assertFalse(pane.focusable)

        harness.resize(1, 1)
        harness.paint()
        assertFalse(pane.dividerVisible)
        assertTrue(first.bounds.width >= 0)
        assertTrue(second.bounds.width >= 0)
    }

    @Test
    fun dragMovesDividerAndDoubleClickResets() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second)
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        assertTrue(harness.drag(10, 1, 15, 1))
        harness.paint()
        assertEquals(15, pane.dividerPosition)
        assertEquals(15, first.bounds.width)

        harness.click(15, 1)
        harness.click(15, 1)
        harness.paint()
        assertEquals(0.5, pane.ratio)
        assertEquals(10, pane.dividerPosition)
    }

    @Test
    fun keyboardMovesDividerAndClampsAtEnds() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second, minFirst = 2, minSecond = 2)
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        assertTrue(pane.focusable)
        assertTrue(harness.surface.focus.focus(pane))
        assertTrue(harness.key(Key.RIGHT))
        harness.paint()
        assertEquals(11, pane.dividerPosition)
        assertTrue(harness.key(Key.HOME))
        harness.paint()
        assertEquals(2, pane.dividerPosition)
        assertTrue(harness.key(Key.END))
        harness.paint()
        assertEquals(17, pane.dividerPosition)
        assertEquals(2, pane.secondExtent)
    }

    @Test
    fun resizePreservesRatioAndClamps() {
        val first = Label("first")
        val second = Label("second")
        val pane = SplitPane(SplitOrientation.HORIZONTAL, first, second, ratio = 0.75, minSecond = 5)
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        assertEquals(0.75, pane.ratio)
        assertEquals(14, pane.firstExtent)

        harness.resize(10, 5)
        harness.paint()
        assertEquals(0.75, pane.ratio)
        assertEquals(4, pane.firstExtent)
        assertEquals(5, pane.secondExtent)

        harness.resize(20, 5)
        harness.paint()
        assertEquals(14, pane.firstExtent)
    }

    @Test
    fun splitPaneMatchesGolden() {
        val pane = SplitPane(SplitOrientation.HORIZONTAL, Label("left"), Label("right"))
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        GoldenAssert.assertRows("split-pane-20x5.txt", harness.rows())
    }

    @Test
    fun splitBackgroundIsSurfaceAndSurvivesDividerDrag() {
        val pane = SplitPane(SplitOrientation.HORIZONTAL, Label("left"), Label("right"))
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        assertEquals(harness.surface.theme.surface, harness.matrix.backgroundAt(0, 0))
        harness.drag(10, 1, 15, 1)
        harness.paint()
        assertEquals(harness.surface.theme.surface, harness.matrix.backgroundAt(0, 0))
    }

    @Test
    fun draggingPastTheLeftEdgeClampsWithoutThrowing() {
        val pane = SplitPane(SplitOrientation.HORIZONTAL, Label("left"), Label("right"))
        val harness = GuiTestHarness(pane, columns = 20, rows = 5)
        harness.paint()
        harness.drag(10, 1, -1, 1)
        harness.paint()
        assertEquals(1, pane.dividerPosition)
    }

    @Test
    fun nestedSplitsStayInsideBounds() {
        val pane =
            SplitPane(
                SplitOrientation.HORIZONTAL,
                Label("left"),
                SplitPane(SplitOrientation.VERTICAL, Label("top"), Label("bottom")),
            )
        val harness = GuiTestHarness(pane, columns = 30, rows = 10)
        harness.paint()
        assertTrue(pane.children.all { it.bounds.x >= 0 && it.bounds.y >= 0 })
        assertTrue(pane.children.all { it.bounds.right <= 30 && it.bounds.bottom <= 10 })
    }
}
