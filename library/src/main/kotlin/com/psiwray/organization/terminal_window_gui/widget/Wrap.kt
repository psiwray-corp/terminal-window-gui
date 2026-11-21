package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Left-to-right wrapping row layout.
 *
 * Children are measured with the available width, packed into runs, and each
 * run uses its tallest child height. No virtualization is performed.
 *
 * @property gap Cells between children in a run.
 * @property runGap Rows between runs.
 * @property crossAxisAlignment Alignment within each run's height.
 * @since 1.0.0
 */
public class Wrap(
    public val gap: Int = 0,
    public val runGap: Int = 0,
    public val crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.START,
    children: List<Widget> = emptyList(),
) : MultiChildWidget() {
    init {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        require(runGap >= 0) { "runGap must be non-negative, got $runGap" }
        for (child in children) {
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val childConstraints = Constraints(0, availableWidth, 0, Int.MAX_VALUE)
        var runWidth = 0
        var runHeight = 0
        var widestRun = 0
        var totalHeight = 0
        var runs = 0
        var runHasChild = false

        for (child in childList) {
            val size = child.measured(childConstraints)
            if (runHasChild && runWidth + gap + size.width > availableWidth) {
                widestRun = maxOf(widestRun, runWidth)
                totalHeight += runHeight
                runs++
                runWidth = 0
                runHeight = 0
                runHasChild = false
            }
            runWidth += if (runHasChild) gap + size.width else size.width
            runHeight = maxOf(runHeight, size.height)
            runHasChild = true
        }
        if (runHasChild) {
            widestRun = maxOf(widestRun, runWidth)
            totalHeight += runHeight
            runs++
        }
        val totalWithRunGaps = totalHeight + if (runs > 1) runGap * (runs - 1) else 0
        return constraints.constrain(IntSize(widestRun, totalWithRunGaps))
    }

    override fun arrangeChildren() {
        val content = contentRect
        val availableWidth = content.width
        val childConstraints = Constraints(0, availableWidth, 0, Int.MAX_VALUE)
        val sizes = ArrayList<IntSize>(childList.size)
        for (child in childList) {
            sizes.add(child.measured(childConstraints))
        }

        var index = 0
        var runTop = 0
        while (index < childList.size) {
            var runEnd = index
            var runWidth = 0
            var runHeight = 0
            while (runEnd < childList.size) {
                val size = sizes[runEnd]
                val nextWidth = if (runEnd == index) size.width else runWidth + gap + size.width
                if (runEnd > index && nextWidth > availableWidth) {
                    break
                }
                runWidth = nextWidth
                runHeight = maxOf(runHeight, size.height)
                runEnd++
            }
            var x = 0
            for (childIndex in index until runEnd) {
                val child = childList[childIndex]
                val size = sizes[childIndex]
                val height = if (crossAxisAlignment == CrossAxisAlignment.STRETCH) runHeight else size.height
                val y =
                    when (crossAxisAlignment) {
                        CrossAxisAlignment.START, CrossAxisAlignment.STRETCH -> 0
                        CrossAxisAlignment.CENTER -> (runHeight - height).coerceAtLeast(0) / 2
                        CrossAxisAlignment.END -> (runHeight - height).coerceAtLeast(0)
                    }
                child.arrange(Rect(x, runTop + y, size.width, height))
                x += size.width + gap
            }
            runTop += runHeight + runGap
            index = runEnd
        }
    }

    override fun paint(context: PaintContext) {}
}
