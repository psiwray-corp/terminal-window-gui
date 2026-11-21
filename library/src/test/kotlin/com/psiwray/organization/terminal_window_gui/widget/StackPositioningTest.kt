package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Length
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StackPositioningTest {
    @Test
    fun stackSizesToNonPositionedChildren() {
        val base = FixedSizeWidget(4, 2)
        val positioned =
            Positioned(
                left = Length.Cells(1),
                top = Length.Cells(1),
                width = Length.Cells(2),
                height = Length.Cells(1),
                child = FixedSizeWidget(1, 1),
            )
        val stack = Stack(children = listOf(base, positioned))

        stack.layout(Constraints.tight(IntSize(10, 5)))

        assertEquals(Rect(0, 0, 4, 2), base.bounds)
        assertEquals(Rect(1, 1, 2, 1), positioned.bounds)
    }

    @Test
    fun positionedChildrenDoNotContributeIntrinsicSize() {
        val base = FixedSizeWidget(2, 1)
        val positioned =
            Positioned(
                width = Length.Fill,
                height = Length.Fill,
                child = FixedSizeWidget(10, 10),
            )
        val stack = Stack(children = listOf(base, positioned))
        val size = stack.layout(Constraints.loose(IntSize(10, 5)))
        assertEquals(IntSize(2, 1), size)
        assertEquals(Rect(0, 0, 2, 1), base.bounds)
        assertEquals(Rect(0, 0, 2, 1), positioned.bounds)
    }

    @Test
    fun positionedEdgePairsDeriveExtent() {
        val positioned =
            Positioned(
                left = Length.Cells(1),
                right = Length.Cells(2),
                top = Length.Cells(0),
                bottom = Length.Cells(0),
                child = FixedSizeWidget(1, 1),
            )
        val stack = Stack(children = listOf(positioned))
        stack.layout(Constraints.tight(IntSize(10, 5)))

        assertEquals(Rect(1, 0, 7, 5), positioned.bounds)
        assertEquals(Rect(0, 0, 7, 5), positioned.child?.bounds)
    }

    @Test
    fun fractionsRoundAndFillUsesParentExtent() {
        val fractional =
            Positioned(
                left = Length.Fraction(0.25f),
                top = Length.Fraction(0.25f),
                width = Length.Fraction(0.5f),
                height = Length.Fraction(0.5f),
                child = FixedSizeWidget(1, 1),
            )
        val fill =
            Positioned(
                width = Length.Fill,
                height = Length.Fill,
                child = FixedSizeWidget(1, 1),
            )
        val content =
            Positioned(
                width = Length.Content,
                height = Length.Content,
                child = FixedSizeWidget(2, 3),
            )
        val stack = Stack(children = listOf(fractional, fill, content))
        stack.layout(Constraints.tight(IntSize(10, 10)))

        assertEquals(Rect(3, 3, 5, 5), fractional.bounds)
        assertEquals(Rect(0, 0, 10, 10), fill.bounds)
        assertEquals(Rect(0, 0, 2, 3), content.bounds)
    }

    @Test
    fun explicitSizeWithEdgePairThrows() {
        assertFailsWith<IllegalArgumentException> {
            Positioned(
                left = Length.Cells(0),
                right = Length.Cells(1),
                width = Length.Cells(2),
                child = FixedSizeWidget(1, 1),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Positioned(
                top = Length.Cells(0),
                bottom = Length.Cells(1),
                height = Length.Cells(2),
                child = FixedSizeWidget(1, 1),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Positioned(width = Length.Cells(-1), child = FixedSizeWidget(1, 1))
        }
    }

    @Test
    fun onlyPositionedChildrenNeedBoundedConstraints() {
        val stack =
            Stack(
                children =
                    listOf(
                        Positioned(width = Length.Fill, height = Length.Fill, child = FixedSizeWidget(1, 1)),
                    ),
            )
        assertFailsWith<IllegalArgumentException> {
            stack.layout(Constraints(0, Int.MAX_VALUE, 0, 10))
        }
        val size = stack.layout(Constraints.tight(IntSize(10, 5)))
        assertEquals(IntSize(10, 5), size)
        assertEquals(Rect(0, 0, 10, 5), stack.children[0].bounds)
    }

    @Test
    fun expandTightensNonPositionedChildren() {
        val child = FixedSizeWidget(1, 1)
        val stack = Stack(fit = StackFit.EXPAND, children = listOf(child))
        stack.layout(Constraints.tight(IntSize(10, 5)))
        assertEquals(Rect(0, 0, 10, 5), child.bounds)
    }

    @Test
    fun laterChildrenPaintOnTop() {
        val lower = FixedSizeWidget(3, 1, 'a')
        val upper = FixedSizeWidget(2, 1, 'b')
        val stack = Stack(children = listOf(lower, upper))
        stack.layout(Constraints.tight(IntSize(3, 1)))
        val matrix = testMatrix(3, 1)
        stack.render(testContext(matrix))
        assertEquals('b'.code, matrix.codePointAt(0, 0))
        assertEquals('b'.code, matrix.codePointAt(1, 0))
        assertEquals('a'.code, matrix.codePointAt(2, 0))
    }
}
