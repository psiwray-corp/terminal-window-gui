package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.max

/**
 * One-row toolbar of buttons, separators, and spacers.
 *
 * Children are arranged left to right with [gap] cells between them and a
 * height of one row. Separators occupy one cell regardless of orientation;
 * [Flexible] children share the remaining width. When space is insufficient,
 * fixed children shrink proportionally instead of painting past the toolbar.
 *
 * @property gap Cells between children.
 * @since 1.0.0
 */
public class Toolbar(
    children: List<Widget>,
    public val gap: Int = 1,
) : MultiChildWidget() {
    /**
     * Creates a toolbar from vararg children.
     *
     * @since 1.0.0
     */
    public constructor(vararg children: Widget) : this(children.toList())

    init {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        for (child in children) {
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        if (childList.isEmpty()) {
            return constraints.constrain(IntSize.ZERO)
        }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else preferredWidth()
        return constraints.constrain(IntSize(max(1, width), 1))
    }

    override fun arrangeChildren() {
        val count = childList.size
        if (count == 0) {
            return
        }
        val width = contentRect.width
        val nominalGap = gap * (count - 1)
        val actualGap = if (width >= nominalGap) gap else 0
        val available = (width - actualGap * (count - 1)).coerceAtLeast(0)
        val preferred = IntArray(count)
        val allocation = IntArray(count)
        val flexible = BooleanArray(count)
        var fixedTotal = 0
        var totalFlex = 0

        for (index in 0 until count) {
            val child = childList[index]
            if (child is Flexible) {
                flexible[index] = true
                totalFlex += child.flex
                continue
            }
            val preferredWidth =
                if (child is Separator) {
                    1
                } else {
                    child.measured(Constraints(0, Int.MAX_VALUE, 1, 1)).width
                }
            preferred[index] = preferredWidth
            fixedTotal += preferredWidth
        }

        if (fixedTotal > available) {
            var assigned = 0
            var lastFixed = -1
            for (index in 0 until count) {
                if (flexible[index]) {
                    continue
                }
                val share = preferred[index] * available / fixedTotal
                allocation[index] = share
                assigned += share
                lastFixed = index
            }
            if (lastFixed >= 0) {
                allocation[lastFixed] += available - assigned
            }
        } else {
            for (index in 0 until count) {
                if (!flexible[index]) {
                    allocation[index] = preferred[index]
                }
            }
            val remaining = available - fixedTotal
            if (totalFlex > 0) {
                var assigned = 0
                var lastFlex = -1
                for (index in 0 until count) {
                    if (!flexible[index]) {
                        continue
                    }
                    val flex = (childList[index] as Flexible).flex
                    val share = remaining * flex / totalFlex
                    allocation[index] = share
                    assigned += share
                    lastFlex = index
                }
                if (lastFlex >= 0) {
                    allocation[lastFlex] += remaining - assigned
                }
            }
        }

        var x = 0
        for (index in 0 until count) {
            childList[index].arrange(Rect(x, 0, allocation[index], 1))
            x += allocation[index]
            if (index < count - 1) {
                x += actualGap
            }
        }
    }

    override fun paint(context: PaintContext) {}

    private fun preferredWidth(): Int {
        var width = 0
        for (child in childList) {
            width +=
                if (child is Separator) {
                    1
                } else {
                    child.measured(Constraints(0, Int.MAX_VALUE, 1, 1)).width
                }
        }
        if (childList.size > 1) {
            width += gap * (childList.size - 1)
        }
        return width
    }
}
