package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Alignment
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * How non-positioned Stack children are measured.
 *
 * @since 1.0.0
 */
public enum class StackFit {
    /** Measure with loose constraints. @since 1.0.0 */
    LOOSE,

    /** Tighten to the stack content box. @since 1.0.0 */
    EXPAND,
}

/**
 * Overlays children in order; later children paint on top.
 *
 * Size is the largest non-positioned child, constrained by the parent.
 * Positioned children do not contribute to intrinsic size.
 *
 * @property fit How non-positioned children are measured.
 * @property alignment Placement for non-positioned children.
 * @since 1.0.0
 */
public class Stack(
    public val fit: StackFit = StackFit.LOOSE,
    public val alignment: Alignment = Alignment.TOP_START,
    children: List<Widget> = emptyList(),
) : MultiChildWidget() {
    init {
        for (child in children) {
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        var hasPositioned = false
        var largestWidth = 0
        var largestHeight = 0
        val childConstraints =
            if (fit == StackFit.EXPAND && constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                constraints.tighten()
            } else {
                constraints.loosen()
            }
        for (child in childList) {
            if (child is Positioned) {
                hasPositioned = true
                continue
            }
            val size = child.measured(childConstraints)
            if (size.width > largestWidth) {
                largestWidth = size.width
            }
            if (size.height > largestHeight) {
                largestHeight = size.height
            }
        }
        if (largestWidth == 0 && largestHeight == 0) {
            if (!hasPositioned) {
                return constraints.constrain(IntSize.ZERO)
            }
            if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
                throw IllegalArgumentException("a Stack with only positioned children needs bounded constraints")
            }
            return constraints.constrain(IntSize(constraints.maxWidth, constraints.maxHeight))
        }
        return constraints.constrain(IntSize(largestWidth, largestHeight))
    }

    override fun arrangeChildren() {
        val content = IntSize(contentRect.width, contentRect.height)
        for (child in childList) {
            if (child is Positioned) {
                child.arrange(child.resolveRect(content))
                continue
            }
            val size =
                child.measured(
                    if (fit == StackFit.EXPAND) {
                        Constraints.tight(content)
                    } else {
                        Constraints.loose(content)
                    },
                )
            val width = if (fit == StackFit.EXPAND) content.width else size.width
            val height = if (fit == StackFit.EXPAND) content.height else size.height
            val x = if (fit == StackFit.EXPAND) 0 else alignment.offsetX(content.width, width)
            val y = if (fit == StackFit.EXPAND) 0 else alignment.offsetY(content.height, height)
            child.arrange(Rect(x, y, width, height))
        }
    }

    override fun paint(context: PaintContext) {}
}
