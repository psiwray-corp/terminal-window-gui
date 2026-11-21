package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class WrapLayoutTest {
    @Test
    fun wrapsAtTheAvailableWidthAndUsesRunHeights() {
        val children = List(3) { FixedSizeWidget(4, 1) }
        val wrap = Wrap(gap = 1, runGap = 1, children = children)
        val size = wrap.layout(Constraints.loose(IntSize(10, 10)))

        assertEquals(IntSize(9, 3), size)
        assertEquals(Rect(0, 0, 4, 1), children[0].bounds)
        assertEquals(Rect(5, 0, 4, 1), children[1].bounds)
        assertEquals(Rect(0, 2, 4, 1), children[2].bounds)
    }

    @Test
    fun eachRunUsesItsTallestChild() {
        val first = FixedSizeWidget(2, 3, 'a')
        val second = FixedSizeWidget(2, 1, 'b')
        val third = FixedSizeWidget(3, 1, 'c')
        val wrap = Wrap(runGap = 1, children = listOf(first, second, third))
        val size = wrap.layout(Constraints.loose(IntSize(5, 10)))

        assertEquals(IntSize(4, 5), size)
        assertEquals(Rect(0, 0, 2, 3), first.bounds)
        assertEquals(Rect(2, 0, 2, 1), second.bounds)
        assertEquals(Rect(0, 4, 3, 1), third.bounds)
    }

    @Test
    fun crossAxisAlignmentPositionsInsideTheRun() {
        val short = FixedSizeWidget(1, 1)
        val tall = FixedSizeWidget(1, 3)
        val centered =
            Wrap(
                crossAxisAlignment = CrossAxisAlignment.CENTER,
                children = listOf(short, tall),
            )
        centered.layout(Constraints.tight(IntSize(5, 3)))
        assertEquals(Rect(0, 1, 1, 1), short.bounds)
        assertEquals(Rect(1, 0, 1, 3), tall.bounds)

        val stretched =
            Wrap(
                crossAxisAlignment = CrossAxisAlignment.STRETCH,
                children = listOf(FixedSizeWidget(1, 1), FixedSizeWidget(1, 3)),
            )
        stretched.layout(Constraints.tight(IntSize(5, 3)))
        assertEquals(Rect(0, 0, 1, 3), stretched.children[0].bounds)
    }
}
