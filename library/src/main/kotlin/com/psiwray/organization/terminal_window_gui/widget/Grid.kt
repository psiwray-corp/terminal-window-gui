package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Alignment
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.roundToInt

/**
 * Row-major grid with fixed, fractional, and flex tracks.
 *
 * If [rows] is empty, the row count is derived from [columns] and each row uses
 * the tallest child content height. Children never overlap adjacent cells.
 *
 * @property columns Column tracks.
 * @property rows Row tracks, or empty to derive content rows.
 * @property gap Cells between tracks.
 * @property alignment Child alignment inside its cell.
 * @since 1.0.0
 */
public class Grid(
    public val columns: List<GridTrack>,
    public val rows: List<GridTrack> = emptyList(),
    public val gap: Int = 0,
    children: List<Widget> = emptyList(),
    public val alignment: Alignment = Alignment.TOP_START,
) : MultiChildWidget() {
    init {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        if (children.isNotEmpty() && columns.isEmpty()) {
            throw IllegalArgumentException("a non-empty Grid needs at least one column")
        }
        if (rows.isNotEmpty() && children.size > columns.size * rows.size) {
            throw IllegalArgumentException("children exceed the explicit grid capacity")
        }
        for (child in children) {
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        if (columns.isEmpty()) {
            return constraints.constrain(IntSize.ZERO)
        }
        val columnWidths =
            resolveTracks(
                tracks = columns,
                available = if (constraints.hasBoundedWidth) constraints.maxWidth else null,
                gap = gap,
            )
        val gridWidth = columnWidths.sum() + gap * (columnWidths.size - 1)
        if (rows.isNotEmpty()) {
            val rowHeights =
                resolveTracks(
                    tracks = rows,
                    available = if (constraints.hasBoundedHeight) constraints.maxHeight else null,
                    gap = gap,
                )
            val gridHeight = rowHeights.sum() + gap * (rowHeights.size - 1)
            measureCells(columnWidths, rowHeights, tightHeight = true)
            return constraints.constrain(IntSize(gridWidth, gridHeight))
        }

        val rowCount = (childList.size + columns.size - 1) / columns.size
        val rowHeights = IntArray(rowCount)
        for (index in childList.indices) {
            val row = index / columns.size
            val column = index % columns.size
            val width = columnWidths[column]
            val size =
                childList[index].measured(
                    Constraints(width, width, 0, Int.MAX_VALUE),
                )
            if (size.height > rowHeights[row]) {
                rowHeights[row] = size.height
            }
        }
        val gridHeight = rowHeights.sum() + if (rowCount > 1) gap * (rowCount - 1) else 0
        return constraints.constrain(IntSize(gridWidth, gridHeight))
    }

    override fun arrangeChildren() {
        if (columns.isEmpty() || childList.isEmpty()) {
            return
        }
        val content = contentRect
        val columnWidths = resolveTracks(columns, content.width, gap)
        val rowHeights =
            if (rows.isNotEmpty()) {
                resolveTracks(rows, content.height, gap)
            } else {
                resolveContentRows(columnWidths, content.width)
            }
        var rowTop = 0
        for (row in rowHeights.indices) {
            val rowHeight = rowHeights[row]
            var columnLeft = 0
            for (column in columns.indices) {
                val index = row * columns.size + column
                if (index >= childList.size) {
                    break
                }
                val cellWidth = columnWidths[column]
                val cellHeight = rowHeight
                val child = childList[index]
                val childSize = child.measured(Constraints.loose(IntSize(cellWidth, cellHeight)))
                val x = columnLeft + alignment.offsetX(cellWidth, childSize.width)
                val y = rowTop + alignment.offsetY(cellHeight, childSize.height)
                child.arrange(Rect(x, y, childSize.width, childSize.height))
                columnLeft += cellWidth + gap
            }
            rowTop += rowHeight + gap
        }
    }

    override fun paint(context: PaintContext) {}

    private fun measureCells(
        columnWidths: IntArray,
        rowHeights: IntArray,
        tightHeight: Boolean,
    ) {
        for (index in childList.indices) {
            val row = index / columnWidths.size
            val column = index % columnWidths.size
            if (row >= rowHeights.size) {
                return
            }
            val width = columnWidths[column]
            val height = rowHeights[row]
            childList[index].measured(
                if (tightHeight) {
                    Constraints(width, width, height, height)
                } else {
                    Constraints(width, width, 0, Int.MAX_VALUE)
                },
            )
        }
    }

    private fun resolveContentRows(
        columnWidths: IntArray,
        availableWidth: Int,
    ): IntArray {
        val rowCount = if (childList.isEmpty()) 0 else (childList.size + columns.size - 1) / columns.size
        val rowHeights = IntArray(rowCount)
        for (index in childList.indices) {
            val row = index / columns.size
            val column = index % columns.size
            val width = minOf(columnWidths[column], availableWidth)
            val size = childList[index].measured(Constraints(width, width, 0, Int.MAX_VALUE))
            if (size.height > rowHeights[row]) {
                rowHeights[row] = size.height
            }
        }
        return rowHeights
    }

    private fun resolveTracks(
        tracks: List<GridTrack>,
        available: Int?,
        gap: Int,
    ): IntArray {
        val sizes = IntArray(tracks.size)
        val gapTotal = if (tracks.size > 1) gap * (tracks.size - 1) else 0
        val availableForTracks =
            if (available == null) {
                var fixed = 0
                for (track in tracks) {
                    if (track is GridTrack.Fixed) {
                        fixed += track.cells
                    }
                }
                fixed
            } else {
                (available - gapTotal).coerceAtLeast(0)
            }

        var fixedSum = 0
        for (index in tracks.indices) {
            val track = tracks[index]
            if (track is GridTrack.Fixed) {
                sizes[index] = track.cells
                fixedSum += track.cells
            }
        }

        var fractionSum = 0
        for (index in tracks.indices) {
            val track = tracks[index]
            if (track is GridTrack.Fraction) {
                val value = (availableForTracks * track.fraction).roundToInt().coerceAtLeast(0)
                sizes[index] = value
                fractionSum += value
            }
        }

        val remaining = (availableForTracks - fixedSum - fractionSum).coerceAtLeast(0)
        var totalFlex = 0
        for (track in tracks) {
            if (track is GridTrack.Flex) {
                totalFlex += track.weight
            }
        }
        if (totalFlex > 0 && remaining > 0) {
            var assigned = 0
            var lastFlex = -1
            for (index in tracks.indices) {
                val track = tracks[index]
                if (track is GridTrack.Flex) {
                    val share = remaining * track.weight / totalFlex
                    sizes[index] = share
                    assigned += share
                    lastFlex = index
                }
            }
            if (lastFlex >= 0) {
                sizes[lastFlex] += remaining - assigned
            }
        }
        return sizes
    }
}
