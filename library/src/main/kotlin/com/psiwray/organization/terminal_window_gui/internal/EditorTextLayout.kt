package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.paint.TextWrap
import com.psiwray.organization.terminal_window_gui.text.Caret

/**
 * Cached visual rows and source mapping for editable text.
 *
 * Layout runs only when text, width, or wrap changes. Paint and pointer code
 * read the rows and never re-wrap.
 */
internal class EditorTextLayout(
    val text: String,
    val width: Int,
    val wrap: TextWrap,
) {
    data class Cell(
        val charIndex: Int,
        val codePoint: Int,
    )

    data class Row(
        val logicalLine: Int,
        val startChar: Int,
        val endChar: Int,
        val cells: List<Cell>,
        val firstOfLine: Boolean,
    )

    data class Position(
        val row: Int,
        val column: Int,
    )

    val rows: List<Row>
    val logicalLineCount: Int
    val contentWidth: Int
    private val positions: Map<Int, Position>

    init {
        val logical = logicalLines(text)
        logicalLineCount = logical.size
        val built = ArrayList<Row>()
        val map = HashMap<Int, Position>()
        var logicalIndex = 0
        for (line in logical) {
            val lineRows =
                when (wrap) {
                    TextWrap.NONE -> listOf(line.cells)
                    TextWrap.CHAR -> characterRows(line.cells, width)
                    TextWrap.WORD -> wordRows(line.cells, width)
                }
            var first = true
            for (cells in lineRows) {
                val rowIndex = built.size
                val start =
                    if (cells.isEmpty()) {
                        line.startChar
                    } else {
                        cells.first().charIndex
                    }
                val end =
                    if (cells.isEmpty()) {
                        line.endChar
                    } else {
                        val last = cells.last()
                        last.charIndex + Character.charCount(last.codePoint)
                    }
                val row = Row(logicalIndex, start, end, cells, first)
                built.add(row)
                map[start] = Position(rowIndex, 0)
                var column = 0
                for (cell in cells) {
                    map.putIfAbsent(cell.charIndex, Position(rowIndex, column))
                    column++
                }
                map[end] = Position(rowIndex, cells.size)
                first = false
            }
            logicalIndex++
        }
        rows = built
        positions = map
        contentWidth = rows.maxOfOrNull { it.cells.size } ?: 0
    }

    fun positionFor(charIndex: Int): Position {
        val index = Caret.snap(text, charIndex)
        positions[index]?.let { return it }
        if (rows.isEmpty()) {
            return Position(0, 0)
        }
        var rowIndex = 0
        while (rowIndex < rows.size) {
            val row = rows[rowIndex]
            if (index <= row.endChar || rowIndex == rows.lastIndex) {
                val column =
                    if (index <= row.startChar) {
                        0
                    } else {
                        var count = 0
                        for (cell in row.cells) {
                            if (cell.charIndex >= index) {
                                break
                            }
                            count++
                        }
                        count
                    }
                return Position(rowIndex, column)
            }
            rowIndex++
        }
        return Position(rows.lastIndex, rows.last().cells.size)
    }

    fun rowStart(rowIndex: Int): Int = rows[rowIndex.coerceIn(0, rows.lastIndex)].startChar

    fun rowEnd(rowIndex: Int): Int = rows[rowIndex.coerceIn(0, rows.lastIndex)].endChar

    fun charIndexAt(
        rowIndex: Int,
        column: Int,
    ): Int {
        if (rows.isEmpty()) {
            return 0
        }
        val row = rows[rowIndex.coerceIn(0, rows.lastIndex)]
        if (column <= 0) {
            return row.startChar
        }
        if (column >= row.cells.size) {
            return row.endChar
        }
        return row.cells[column].charIndex
    }

    fun rowOfLogicalLine(logicalLine: Int): Int {
        var index = 0
        while (index < rows.size) {
            if (rows[index].logicalLine == logicalLine) {
                return index
            }
            index++
        }
        return rows.lastIndex.coerceAtLeast(0)
    }

    private data class LogicalLine(
        val startChar: Int,
        val endChar: Int,
        val cells: List<Cell>,
    )

    private fun logicalLines(text: String): List<LogicalLine> {
        val result = ArrayList<LogicalLine>()
        val cells = ArrayList<Cell>()
        var lineStart = 0
        var index = 0
        var column = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (codePoint == '\n'.code) {
                result.add(LogicalLine(lineStart, index, ArrayList(cells)))
                cells.clear()
                index++
                lineStart = index
                column = 0
                continue
            }
            when {
                codePoint == '\t'.code -> {
                    val spaces = TAB_WIDTH - column % TAB_WIDTH
                    repeat(spaces) {
                        cells.add(Cell(index, SPACE))
                    }
                    column += spaces
                }

                codePoint in 0x0000..0x001F -> {
                    cells.add(Cell(index, SPACE))
                    column++
                }

                codePoint in 0xD800..0xDFFF -> {
                    cells.add(Cell(index, REPLACEMENT))
                    column++
                }

                else -> {
                    cells.add(Cell(index, codePoint))
                    column++
                }
            }
            index += Character.charCount(codePoint)
        }
        result.add(LogicalLine(lineStart, text.length, cells))
        return result
    }

    private fun characterRows(
        cells: List<Cell>,
        width: Int,
    ): List<List<Cell>> {
        if (cells.isEmpty()) {
            return listOf(emptyList())
        }
        val maxWidth = width.coerceAtLeast(1)
        val result = ArrayList<List<Cell>>()
        var index = 0
        while (index < cells.size) {
            val end = (index + maxWidth).coerceAtMost(cells.size)
            result.add(ArrayList(cells.subList(index, end)))
            index = end
        }
        return result
    }

    private fun wordRows(
        cells: List<Cell>,
        width: Int,
    ): List<List<Cell>> {
        if (cells.isEmpty()) {
            return listOf(emptyList())
        }
        val maxWidth = width.coerceAtLeast(1)
        val result = ArrayList<List<Cell>>()
        val current = ArrayList<Cell>()
        var index = 0
        while (index < cells.size) {
            while (index < cells.size && cells[index].codePoint == SPACE) {
                index++
            }
            if (index >= cells.size) {
                break
            }
            val wordStart = index
            while (index < cells.size && cells[index].codePoint != SPACE) {
                index++
            }
            val word = cells.subList(wordStart, index)
            if (current.isNotEmpty()) {
                if (current.size + 1 + word.size <= maxWidth) {
                    val spaceIndex = (wordStart - 1).coerceAtLeast(0)
                    current.add(Cell(cells[spaceIndex].charIndex, SPACE))
                    current.addAll(word)
                    continue
                }
                result.add(ArrayList(current))
                current.clear()
            }
            if (word.size <= maxWidth) {
                current.addAll(word)
            } else {
                var offset = 0
                while (offset + maxWidth < word.size) {
                    result.add(ArrayList(word.subList(offset, offset + maxWidth)))
                    offset += maxWidth
                }
                current.addAll(word.subList(offset, word.size))
            }
        }
        if (current.isNotEmpty() || result.isEmpty()) {
            result.add(ArrayList(current))
        }
        return result
    }

    private companion object {
        private const val TAB_WIDTH: Int = 4
        private const val SPACE: Int = ' '.code
        private const val REPLACEMENT: Int = 0xFFFD
    }
}
