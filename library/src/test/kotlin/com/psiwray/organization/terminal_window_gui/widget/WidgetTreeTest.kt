package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WidgetTreeTest {
    @Test
    fun childrenUseParentContentCoordinates() {
        val first = FixedSizeWidget(2, 1)
        val second = FixedSizeWidget(3, 2)
        val row = Row(children = listOf(first, second))

        val size = row.layout(Constraints.tight(IntSize(10, 10)))

        assertEquals(IntSize(10, 10), size)
        assertEquals(Rect(0, 0, 2, 1), first.bounds)
        assertEquals(Rect(2, 0, 3, 2), second.bounds)
        assertSame(row, first.parent)
        assertSame(row, second.parent)
        assertEquals(Rect(0, 0, 10, 10), row.contentRect)
    }

    @Test
    fun multiChildMutationsMaintainTreeAndView() {
        val row = Row()
        val first = FixedSizeWidget(1, 1)
        val second = FixedSizeWidget(1, 1)
        row.add(first)
        row.insert(0, second)
        assertEquals(listOf(second, first), row.children)
        assertSame(row, second.parent)
        assertTrue(row.remove(first))
        assertEquals(listOf(second), row.children)
        assertEquals(null, first.parent)

        row.clear()
        assertTrue(row.children.isEmpty())
        assertEquals(null, second.parent)
    }

    @Test
    fun attachAndDetachFollowTheTree() {
        val child = TrackingWidget()
        val row = Row(children = listOf(child))
        var invalidations = 0
        row.setSurfaceInvalidation { invalidations++ }

        row.layout(Constraints.tight(IntSize(4, 2)))
        assertEquals(1, child.attachCount)
        assertEquals(0, child.detachCount)

        row.detachTree()
        assertEquals(1, child.detachCount)
        row.attachTree()
        assertEquals(2, child.attachCount)
        assertTrue(invalidations >= 0)
    }

    @Test
    fun invalidationRebuildsMeasurementAndMarksSurface() {
        val child = FixedSizeWidget(2, 1)
        val root = Row(children = listOf(child))
        var invalidations = 0
        root.setSurfaceInvalidation { invalidations++ }

        root.layout(Constraints.tight(IntSize(8, 2)))
        val firstMeasureCount = child.measureCount
        root.layout(Constraints.tight(IntSize(8, 2)))
        assertEquals(firstMeasureCount, child.measureCount)

        child.invalidateLayout()
        root.layout(Constraints.tight(IntSize(8, 2)))
        assertTrue(child.measureCount > firstMeasureCount)
        assertTrue(invalidations > 0)
    }

    @Test
    fun boundsChangesAreReported() {
        val child = BoundsRecordingWidget(2, 1)
        val root = Row(children = listOf(child))
        root.layout(Constraints.tight(IntSize(8, 2)))
        child.arrange(Rect(1, 2, 3, 4))
        assertEquals(Rect(0, 0, 2, 1), child.changes.last().first)
        assertEquals(Rect(1, 2, 3, 4), child.changes.last().second)
    }

    @Test
    fun paddingClipsAndPaintsInLocalCoordinates() {
        val child = FixedSizeWidget(10, 10, 'x')
        val root = Padding(1, child)
        root.layout(Constraints.tight(IntSize(5, 5)))
        val matrix = testMatrix(5, 5)

        root.render(testContext(matrix))

        assertEquals(Rect(0, 0, 3, 3), child.bounds)
        assertNotEquals('x'.code, matrix.codePointAt(0, 0))
        assertEquals('x'.code, matrix.codePointAt(1, 1))
        assertNotEquals('x'.code, matrix.codePointAt(4, 4))
        assertEquals(1, child.paintCount)
    }

    @Test
    fun containersClipChildPaintToContentRect() {
        val root = SizedBox(width = 5, height = 1, child = SpillWidget())
        root.layout(Constraints.loose(IntSize(10, 1)))
        val matrix = testMatrix(10, 1)
        root.render(testContext(matrix))
        assertEquals('A'.code, matrix.codePointAt(0, 0))
        assertEquals('B'.code, matrix.codePointAt(1, 0))
        for (column in 2 until 10) {
            assertNotEquals('Z'.code, matrix.codePointAt(column, 0))
        }
    }

    @Test
    fun zeroAreaWidgetsAreNotPainted() {
        val child = FixedSizeWidget(1, 1, 'x')
        val root = SizedBox(0, 0, child)
        root.layout(Constraints.loose(IntSize(10, 10)))
        root.render(testContext(testMatrix(10, 10)))
        assertEquals(0, child.paintCount)
    }

    @Test
    fun wrapperLayoutsUseContentBoxes() {
        val child = FixedSizeWidget(2, 2, 'x')
        val root = Padding(Insets(1, 2, 3, 4), child)
        val size = root.layout(Constraints.loose(IntSize(20, 20)))
        assertEquals(IntSize(6, 8), size)
        assertEquals(Rect(0, 0, 2, 2), child.bounds)
        assertEquals(Rect(1, 2, 2, 2), root.contentRect)

        val centered = Center(FixedSizeWidget(2, 2))
        centered.layout(Constraints.tight(IntSize(10, 10)))
        assertEquals(Rect(4, 4, 2, 2), centered.child?.bounds)

        val sized = SizedBox(width = 7, height = 3)
        sized.layout(Constraints.loose(IntSize(20, 20)))
        assertEquals(Rect(0, 0, 7, 3), sized.bounds)

        val constrained = ConstrainedBox(minWidth = 5, minHeight = 4)
        constrained.layout(Constraints.loose(IntSize(20, 20)))
        assertEquals(IntSize(5, 4), constrained.bounds.toSize())

        val fractional = FractionallySizedBox(0.5f, 0.5f)
        fractional.layout(Constraints.loose(IntSize(10, 10)))
        assertEquals(Rect(0, 0, 5, 5), fractional.bounds)
    }
}

private fun Rect.toSize(): IntSize = IntSize(width, height)
