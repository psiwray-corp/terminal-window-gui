package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.widget.ColumnWidth
import com.psiwray.organization.terminal_window_gui.widget.TableColumn
import kotlin.math.floor

/**
 * Resolved column widths and cell offsets for one table viewport.
 *
 * @property widths Column widths in cells.
 * @property offsets Column starts in content coordinates.
 * @property contentWidth Sum of [widths].
 */
internal class ColumnMetrics(
    val widths: IntArray,
    val offsets: IntArray,
    val contentWidth: Int,
)

internal object ColumnLayout {
    fun measure(
        columns: List<TableColumn<*>>,
        viewportWidth: Int,
    ): ColumnMetrics {
        val count = columns.size
        val widths = IntArray(count)
        val minimum = IntArray(count)
        for (index in 0 until count) {
            val width = columns[index].width
            minimum[index] =
                when (width) {
                    is ColumnWidth.Fixed -> width.cells.coerceAtLeast(1)

                    is ColumnWidth.Flex,
                    is ColumnWidth.Fraction,
                    ColumnWidth.Content,
                    -> TextPainter.codePointCount(columns[index].title).coerceAtLeast(1)
                }
            widths[index] = minimum[index]
        }
        val minimumTotal = minimum.sum()
        if (minimumTotal >= viewportWidth) {
            return metrics(widths)
        }

        var fixedTotal = 0
        var fractionTotal = 0
        val flexIndexes = ArrayList<Int>()
        for (index in 0 until count) {
            when (val width = columns[index].width) {
                is ColumnWidth.Fixed -> {
                    fixedTotal += widths[index]
                }

                ColumnWidth.Content -> {
                    fixedTotal += widths[index]
                }

                is ColumnWidth.Fraction -> {
                    val cells = floor(viewportWidth * width.fraction).toInt().coerceIn(1, viewportWidth)
                    widths[index] = cells
                    fractionTotal += cells
                }

                is ColumnWidth.Flex -> {
                    flexIndexes.add(index)
                }
            }
        }

        if (flexIndexes.isNotEmpty()) {
            var weightTotal = 0.0
            for (index in flexIndexes) {
                weightTotal += (columns[index].width as ColumnWidth.Flex).weight
            }
            val remaining = (viewportWidth - fixedTotal - fractionTotal).coerceAtLeast(flexIndexes.size)
            var used = 0
            for (index in flexIndexes) {
                val weight = (columns[index].width as ColumnWidth.Flex).weight
                val cells = floor(remaining * weight / weightTotal).toInt().coerceAtLeast(1)
                widths[index] = cells
                used += cells
            }
            var leftover = remaining - used
            var cursor = 0
            while (leftover > 0 && flexIndexes.isNotEmpty()) {
                widths[flexIndexes[cursor % flexIndexes.size]]++
                cursor++
                leftover--
            }
        }
        return metrics(widths)
    }

    private fun metrics(widths: IntArray): ColumnMetrics {
        val offsets = IntArray(widths.size)
        var start = 0
        var total = 0
        for (index in widths.indices) {
            offsets[index] = start
            start += widths[index]
            total += widths[index]
        }
        return ColumnMetrics(widths, offsets, total)
    }
}
