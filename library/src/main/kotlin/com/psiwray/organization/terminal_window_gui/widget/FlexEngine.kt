package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Axis
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.MainAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize
import com.psiwray.organization.terminal_window_gui.layout.Rect

internal object FlexEngine {
    fun layout(
        children: List<Widget>,
        axis: Axis,
        constraints: Constraints,
        mainAxisAlignment: MainAxisAlignment,
        crossAxisAlignment: CrossAxisAlignment,
        mainAxisSize: MainAxisSize,
        gap: Int,
        arrange: Boolean,
    ): IntSize {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        val count = children.size
        val mainMin = if (axis == Axis.HORIZONTAL) constraints.minWidth else constraints.minHeight
        val mainMax = if (axis == Axis.HORIZONTAL) constraints.maxWidth else constraints.maxHeight
        val crossMin = if (axis == Axis.HORIZONTAL) constraints.minHeight else constraints.minWidth
        val crossMax = if (axis == Axis.HORIZONTAL) constraints.maxHeight else constraints.maxWidth
        val mainBounded = mainMax != Int.MAX_VALUE
        val crossBounded = crossMax != Int.MAX_VALUE
        val measureConstraints =
            if (axis == Axis.HORIZONTAL) {
                Constraints(0, Int.MAX_VALUE, 0, crossMax)
            } else {
                Constraints(0, crossMax, 0, Int.MAX_VALUE)
            }

        val preferredMain = IntArray(count)
        val preferredCross = IntArray(count)
        val allocation = IntArray(count)
        val crossAllocation = IntArray(count)
        val isFlex = BooleanArray(count)
        var nonFlexMain = 0
        var totalFlex = 0
        var largestCross = 0

        for (index in 0 until count) {
            val flex = children[index] as? Flexible
            if (flex != null) {
                isFlex[index] = true
                totalFlex += flex.flex
            }
        }
        for (index in 0 until count) {
            if (isFlex[index]) {
                continue
            }
            val size = children[index].measured(measureConstraints)
            preferredMain[index] = axis.main(size)
            preferredCross[index] = axis.cross(size)
            nonFlexMain += preferredMain[index]
            if (preferredCross[index] > largestCross) {
                largestCross = preferredCross[index]
            }
        }
        for (index in 0 until count) {
            if (!isFlex[index]) {
                continue
            }
            val size = children[index].measured(measureConstraints)
            preferredCross[index] = axis.cross(size)
            if (preferredCross[index] > largestCross) {
                largestCross = preferredCross[index]
            }
        }

        val gapTotal = if (count > 1) gap * (count - 1) else 0
        val preferredMainTotal = nonFlexMain + gapTotal
        val desiredMain =
            if (mainAxisSize == MainAxisSize.MAX && mainBounded) {
                mainMax
            } else {
                preferredMainTotal
            }
        val mainExtent = desiredMain.coerceIn(mainMin, mainMax)
        val crossExtent =
            largestCross.coerceAtLeast(crossMin).let { if (crossBounded) it.coerceAtMost(crossMax) else it }
        val availableForChildren = (mainExtent - gapTotal).coerceAtLeast(0)

        if (nonFlexMain > availableForChildren) {
            distributeShrink(preferredMain, isFlex, nonFlexMain, availableForChildren, allocation)
        } else {
            for (index in 0 until count) {
                if (!isFlex[index]) {
                    allocation[index] = preferredMain[index]
                }
            }
            val remaining = availableForChildren - nonFlexMain
            if (totalFlex > 0) {
                var assigned = 0
                var lastFlex = -1
                for (index in 0 until count) {
                    if (!isFlex[index]) {
                        continue
                    }
                    val flex = (children[index] as Flexible).flex
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

        for (index in 0 until count) {
            val cross =
                if (crossAxisAlignment == CrossAxisAlignment.STRETCH) {
                    crossExtent
                } else {
                    preferredCross[index].coerceAtMost(crossExtent)
                }
            crossAllocation[index] = cross
        }

        if (arrange) {
            var usedMain = gapTotal
            for (index in 0 until count) {
                usedMain += allocation[index]
            }
            val free = (mainExtent - usedMain).coerceAtLeast(0)
            var position = 0
            var extraGap = 0
            var remainder = 0
            when (mainAxisAlignment) {
                MainAxisAlignment.START -> {
                    position = 0
                }

                MainAxisAlignment.CENTER -> {
                    position = free / 2
                }

                MainAxisAlignment.END -> {
                    position = free
                }

                MainAxisAlignment.SPACE_BETWEEN -> {
                    if (count > 1) {
                        extraGap = free / (count - 1)
                        remainder = free % (count - 1)
                    } else {
                        position = 0
                    }
                }
            }

            for (index in 0 until count) {
                val childMain = allocation[index]
                val childCross = crossAllocation[index]
                val crossFree = (crossExtent - childCross).coerceAtLeast(0)
                val crossPosition =
                    when (crossAxisAlignment) {
                        CrossAxisAlignment.START -> 0
                        CrossAxisAlignment.CENTER -> crossFree / 2
                        CrossAxisAlignment.END -> crossFree
                        CrossAxisAlignment.STRETCH -> 0
                    }
                val rect =
                    if (axis == Axis.HORIZONTAL) {
                        Rect(position, crossPosition, childMain, childCross)
                    } else {
                        Rect(crossPosition, position, childCross, childMain)
                    }
                children[index].arrange(rect)
                position += childMain
                if (index < count - 1) {
                    position += gap + extraGap
                    if (index == count - 2) {
                        position += remainder
                    }
                }
            }
        }

        return if (axis == Axis.HORIZONTAL) IntSize(mainExtent, crossExtent) else IntSize(crossExtent, mainExtent)
    }

    private fun distributeShrink(
        preferredMain: IntArray,
        isFlex: BooleanArray,
        nonFlexMain: Int,
        available: Int,
        allocation: IntArray,
    ) {
        var assigned = 0
        var lastNonFlex = -1
        for (index in preferredMain.indices) {
            if (isFlex[index]) {
                continue
            }
            val share = preferredMain[index] * available / nonFlexMain
            allocation[index] = share
            assigned += share
            lastNonFlex = index
        }
        if (lastNonFlex >= 0) {
            allocation[lastNonFlex] += available - assigned
        }
    }
}
