package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridLayoutTest {
    @Test
    fun childrenArePlacedRowMajorInTracks() {
        val children = List(4) { FixedSizeWidget(1, 1) }
        val grid =
            Grid(
                columns = listOf(GridTrack.Fixed(3), GridTrack.Fixed(2)),
                rows = listOf(GridTrack.Fixed(1), GridTrack.Fixed(2)),
                gap = 1,
                children = children,
            )
        grid.layout(Constraints.tight(IntSize(6, 4)))

        assertEquals(Rect(0, 0, 1, 1), children[0].bounds)
        assertEquals(Rect(4, 0, 1, 1), children[1].bounds)
        assertEquals(Rect(0, 2, 1, 1), children[2].bounds)
        assertEquals(Rect(4, 2, 1, 1), children[3].bounds)
    }

    @Test
    fun fractionAndFlexTracksDistributeExactly() {
        val fractionChildren = List(2) { FixedSizeWidget(1, 1) }
        val fraction =
            Grid(
                columns = listOf(GridTrack.Fraction(0.5f), GridTrack.Fixed(2)),
                gap = 1,
                children = fractionChildren,
            )
        fraction.layout(Constraints.tight(IntSize(10, 2)))
        assertEquals(Rect(0, 0, 1, 1), fractionChildren[0].bounds)
        assertEquals(Rect(6, 0, 1, 1), fractionChildren[1].bounds)

        val flexChildren = List(3) { FixedSizeWidget(1, 1) }
        val flex =
            Grid(
                columns = GridTrack.uniformFlex(3),
                rows = listOf(GridTrack.Fixed(1)),
                children = flexChildren,
            )
        flex.layout(Constraints.tight(IntSize(10, 1)))
        assertEquals(listOf(0, 3, 6), flexChildren.map { it.bounds.x })
        assertEquals(listOf(1, 1, 1), flexChildren.map { it.bounds.width })
    }

    @Test
    fun omittedRowsUseContentHeightAndGaps() {
        val children = listOf(FixedSizeWidget(1, 2), FixedSizeWidget(1, 1), FixedSizeWidget(1, 3))
        val grid =
            Grid(
                columns = listOf(GridTrack.Fixed(2), GridTrack.Fixed(2)),
                gap = 1,
                children = children,
            )
        val size = grid.layout(Constraints.loose(IntSize(10, 10)))
        assertEquals(IntSize(5, 6), size)
        assertEquals(Rect(0, 0, 1, 2), children[0].bounds)
        assertEquals(Rect(3, 0, 1, 1), children[1].bounds)
        assertEquals(Rect(0, 3, 1, 3), children[2].bounds)
    }

    @Test
    fun oversizedChildrenAreClampedToTheirCell() {
        val child = FixedSizeWidget(10, 10)
        val grid =
            Grid(
                columns = listOf(GridTrack.Fixed(2)),
                rows = listOf(GridTrack.Fixed(1)),
                children = listOf(child),
            )
        grid.layout(Constraints.tight(IntSize(2, 1)))
        assertEquals(Rect(0, 0, 2, 1), child.bounds)
    }

    @Test
    fun explicitCapacityIsValidated() {
        assertFailsWith<IllegalArgumentException> {
            Grid(
                columns = listOf(GridTrack.Fixed(1)),
                rows = listOf(GridTrack.Fixed(1)),
                children = listOf(FixedSizeWidget(1, 1), FixedSizeWidget(1, 1)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Grid(columns = emptyList(), children = listOf(FixedSizeWidget(1, 1)))
        }
    }
}
