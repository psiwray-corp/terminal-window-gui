package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.MainAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals

class RowColumnLayoutTest {
    @Test
    fun flexDistributionAndRemainder() {
        val first = Expanded(child = FixedSizeWidget(1, 1))
        val second = Flexible(child = FixedSizeWidget(1, 1))
        val row =
            Row(
                gap = 1,
                children = listOf(FixedSizeWidget(2, 1), first, second),
            )

        row.layout(Constraints.tight(IntSize(10, 1)))

        assertEquals(Rect(0, 0, 2, 1), row.children[0].bounds)
        assertEquals(Rect(3, 0, 3, 1), first.bounds)
        assertEquals(Rect(7, 0, 3, 1), second.bounds)
        assertEquals(Rect(0, 0, 3, 1), first.child?.bounds)
        assertEquals(Rect(0, 0, 1, 1), second.child?.bounds)

        val thirds = List(3) { Expanded(child = FixedSizeWidget(1, 1)) }
        val remainder = Row(children = thirds)
        remainder.layout(Constraints.tight(IntSize(10, 1)))
        assertEquals(listOf(3, 3, 4), thirds.map { it.bounds.width })
    }

    @Test
    fun nestedContainerChildrenPaintAtTheParentOffset() {
        val panel = Panel("P", child = Label("body"))
        val row =
            Row(
                gap = 1,
                children = listOf(FixedSizeWidget(5, 1), SizedBox(width = 5, child = panel)),
            )
        val harness = GuiTestHarness(row, columns = 20, rows = 3)
        harness.paint()
        assertEquals(0x250C, harness.matrix.codePointAt(6, 0))
        assertEquals('P'.code, harness.matrix.codePointAt(8, 0))
    }

    @Test
    fun gapAndAlignmentAreApplied() {
        val first = FixedSizeWidget(3, 2)
        val second = FixedSizeWidget(1, 1)
        val row =
            Row(
                mainAxisAlignment = MainAxisAlignment.CENTER,
                crossAxisAlignment = CrossAxisAlignment.CENTER,
                gap = 1,
                children = listOf(first, second),
            )

        row.layout(Constraints.tight(IntSize(10, 5)))

        assertEquals(Rect(2, 1, 3, 2), first.bounds)
        assertEquals(Rect(6, 2, 1, 1), second.bounds)
    }

    @Test
    fun spaceBetweenDistributesRemainingCells() {
        val first = FixedSizeWidget(2, 1)
        val second = FixedSizeWidget(2, 1)
        val row = Row(mainAxisAlignment = MainAxisAlignment.SPACE_BETWEEN, children = listOf(first, second))
        row.layout(Constraints.tight(IntSize(10, 1)))
        assertEquals(Rect(0, 0, 2, 1), first.bounds)
        assertEquals(Rect(8, 0, 2, 1), second.bounds)
    }

    @Test
    fun crossAxisStretchFillsTheCrossExtent() {
        val child = FixedSizeWidget(1, 1)
        val row = Row(crossAxisAlignment = CrossAxisAlignment.STRETCH, children = listOf(child))
        row.layout(Constraints.tight(IntSize(10, 5)))
        assertEquals(Rect(0, 0, 1, 5), child.bounds)
    }

    @Test
    fun columnUsesTheSameFlexAlgorithm() {
        val first = FixedSizeWidget(1, 1)
        val expanded = Expanded(child = FixedSizeWidget(1, 1))
        val column = Column(gap = 1, children = listOf(first, expanded))
        column.layout(Constraints.tight(IntSize(1, 10)))
        assertEquals(Rect(0, 0, 1, 1), first.bounds)
        assertEquals(Rect(0, 2, 1, 8), expanded.bounds)
    }

    @Test
    fun insufficientSpaceShrinksNonFlexChildrenWithoutOverlap() {
        val first = FixedSizeWidget(3, 1)
        val second = FixedSizeWidget(3, 1)
        val row = Row(children = listOf(first, second))
        row.layout(Constraints.tight(IntSize(4, 1)))
        assertEquals(Rect(0, 0, 2, 1), first.bounds)
        assertEquals(Rect(2, 0, 2, 1), second.bounds)
    }

    @Test
    fun spacerTakesOnlyTheFlexSpace() {
        val left = FixedSizeWidget(2, 1)
        val spacer = Spacer()
        val right = FixedSizeWidget(2, 1)
        val row = Row(children = listOf(left, spacer, right))
        row.layout(Constraints.tight(IntSize(10, 1)))
        assertEquals(Rect(0, 0, 2, 1), left.bounds)
        assertEquals(Rect(2, 0, 6, 0), spacer.bounds)
        assertEquals(Rect(8, 0, 2, 1), right.bounds)
    }

    @Test
    fun mainAxisMinimumUsesPreferredSize() {
        val row = Row(mainAxisSize = MainAxisSize.MIN, children = listOf(FixedSizeWidget(2, 1)))
        val size = row.layout(Constraints.loose(IntSize(10, 10)))
        assertEquals(IntSize(2, 1), size)
        assertEquals(Rect(0, 0, 2, 1), row.children[0].bounds)
    }

    @Test
    fun repeatedLayoutIsIdempotent() {
        val child = Expanded(child = FixedSizeWidget(1, 1))
        val row = Row(gap = 1, children = listOf(FixedSizeWidget(2, 2), child))
        row.layout(Constraints.tight(IntSize(10, 4)))
        val first = row.children.map { it.bounds }
        row.layout(Constraints.tight(IntSize(10, 4)))
        assertEquals(first, row.children.map { it.bounds })
    }
}
