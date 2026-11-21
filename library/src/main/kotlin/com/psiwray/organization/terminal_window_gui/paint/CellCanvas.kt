package com.psiwray.organization.terminal_window_gui.paint

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.layout.Rect

/**
 * Absolute screen-cell writer with origin and clip stacks.
 *
 * Coordinates passed to drawing methods are local to the current origin.
 * [pushOrigin] adds an integer offset to subsequent drawing. [pushClip]
 * translates a local rectangle by the current origin and intersects it with
 * the active clip. Clips are half-open. Writes outside the clip or the matrix
 * are ignored.
 *
 * @property matrix Destination matrix.
 * @since 1.0.0
 */
public class CellCanvas(
    public val matrix: TerminalMatrix,
) {
    private val clipStack = arrayOfNulls<Rect>(MAX_STACK_DEPTH)
    private val originXStack = IntArray(MAX_STACK_DEPTH)
    private val originYStack = IntArray(MAX_STACK_DEPTH)
    private var clipDepth = 0
    private var originDepth = 0
    private var originX = 0
    private var originY = 0
    private var clip = matrixBounds()

    /**
     * Writes one cell at local ([x], [y]).
     *
     * The call is ignored when the translated cell is outside the clip or the
     * matrix.
     *
     * @since 1.0.0
     */
    public fun set(
        x: Int,
        y: Int,
        codePoint: Int,
        fg: Int,
        bg: Int,
    ) {
        requireScalar(codePoint)
        writeCell(originX + x, originY + y, codePoint, fg, bg)
    }

    /** Writes one cell while preserving its current background. */
    internal fun setPreservingBackground(
        x: Int,
        y: Int,
        codePoint: Int,
        fg: Int,
    ) {
        requireScalar(codePoint)
        val column = originX + x
        val row = originY + y
        if (column < 0 || column >= matrix.columns || row < 0 || row >= matrix.rows) {
            return
        }
        if (!clip.contains(column, row)) {
            return
        }
        matrix.set(column, row, codePoint, fg, matrix.backgroundAt(column, row))
    }

    /** Writes text while preserving each destination cell's background. */
    internal fun textPreservingBackground(
        x: Int,
        y: Int,
        text: CharSequence,
        fg: Int,
        tabWidth: Int = DEFAULT_TAB_WIDTH,
    ): Int {
        require(tabWidth > 0) { "tabWidth must be positive, got $tabWidth" }
        val startColumn = originX + x
        var column = startColumn
        var row = originY + y
        var nextColumn = x
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            when {
                codePoint == '\n'.code -> {
                    column = startColumn
                    row++
                    nextColumn = x
                    if (row >= matrix.rows || row >= clip.bottom) {
                        break
                    }
                }

                codePoint == '\t'.code -> {
                    val spaces = tabWidth - Math.floorMod(column - startColumn, tabWidth)
                    repeat(spaces) {
                        writeCellPreservingBackground(column, row, SPACE, fg)
                        column++
                    }
                    nextColumn = column - originX
                }

                row >= matrix.rows || row >= clip.bottom -> {
                    break
                }

                column >= matrix.columns || column >= clip.right -> {
                    break
                }

                else -> {
                    val cell = if (codePoint in 0xD800..0xDFFF) REPLACEMENT else codePoint
                    writeCellPreservingBackground(column, row, cell, fg)
                    column++
                    nextColumn = column - originX
                }
            }
            index += Character.charCount(codePoint)
        }
        return nextColumn
    }

    /**
     * Writes [text] starting at local ([x], [y]) and returns the next local
     * column on the last line.
     *
     * Tabs expand relative to the first column of the current line. `\n` starts
     * a new line at the starting column. Writes stop at the right edge of the
     * clip or matrix. Surrogate pairs occupy one cell; unpaired surrogates
     * become U+FFFD.
     *
     * @since 1.0.0
     */
    public fun text(
        x: Int,
        y: Int,
        text: CharSequence,
        fg: Int,
        bg: Int,
        tabWidth: Int = DEFAULT_TAB_WIDTH,
    ): Int {
        require(tabWidth > 0) { "tabWidth must be positive, got $tabWidth" }
        val startColumn = originX + x
        var column = startColumn
        var row = originY + y
        var nextColumn = x
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            when {
                codePoint == '\n'.code -> {
                    column = startColumn
                    row++
                    nextColumn = x
                    if (row >= matrix.rows || row >= clip.bottom) {
                        break
                    }
                }

                codePoint == '\t'.code -> {
                    val spaces = tabWidth - Math.floorMod(column - startColumn, tabWidth)
                    repeat(spaces) {
                        writeCell(column, row, SPACE, fg, bg)
                        column++
                    }
                    nextColumn = column - originX
                }

                row >= matrix.rows || row >= clip.bottom -> {
                    break
                }

                column >= matrix.columns || column >= clip.right -> {
                    break
                }

                else -> {
                    val cell = if (codePoint in 0xD800..0xDFFF) REPLACEMENT else codePoint
                    writeCell(column, row, cell, fg, bg)
                    column++
                    nextColumn = column - originX
                }
            }
            index += Character.charCount(codePoint)
        }
        return nextColumn
    }

    /**
     * Fills every clipped cell in [rect] with [codePoint].
     *
     * @since 1.0.0
     */
    public fun fill(
        rect: Rect,
        codePoint: Int,
        fg: Int,
        bg: Int,
    ) {
        requireScalar(codePoint)
        val target =
            rect
                .offset(originX, originY)
                .intersect(clip)
                .intersect(matrixBounds())
        var row = target.y
        while (row < target.bottom) {
            var column = target.x
            while (column < target.right) {
                matrix.set(column, row, codePoint, fg, bg)
                column++
            }
            row++
        }
    }

    /**
     * Draws a horizontal line of [length] cells.
     *
     * @since 1.0.0
     */
    public fun hline(
        x: Int,
        y: Int,
        length: Int,
        codePoint: Int,
        fg: Int,
        bg: Int,
    ) {
        require(length >= 0) { "length must be non-negative, got $length" }
        fill(Rect(x, y, length, 1), codePoint, fg, bg)
    }

    /**
     * Draws a vertical line of [length] cells.
     *
     * @since 1.0.0
     */
    public fun vline(
        x: Int,
        y: Int,
        length: Int,
        codePoint: Int,
        fg: Int,
        bg: Int,
    ) {
        require(length >= 0) { "length must be non-negative, got $length" }
        fill(Rect(x, y, 1, length), codePoint, fg, bg)
    }

    /**
     * Pushes a local clip intersected with the active clip.
     *
     * @throws IllegalStateException if the fixed clip stack is full.
     * @since 1.0.0
     */
    public fun pushClip(rect: Rect) {
        if (clipDepth >= MAX_STACK_DEPTH) {
            throw IllegalStateException("clip stack depth exceeded: $MAX_STACK_DEPTH")
        }
        clipStack[clipDepth++] = clip
        clip = clip.intersect(rect.offset(originX, originY))
    }

    /**
     * Pops the most recent clip.
     *
     * @throws IllegalStateException if no clip is pushed.
     * @since 1.0.0
     */
    public fun popClip() {
        if (clipDepth == 0) {
            throw IllegalStateException("clip stack is empty")
        }
        clipDepth--
        clip = clipStack[clipDepth] ?: matrixBounds()
        clipStack[clipDepth] = null
    }

    /**
     * Adds a local origin offset.
     *
     * @throws IllegalStateException if the fixed origin stack is full.
     * @since 1.0.0
     */
    public fun pushOrigin(
        dx: Int,
        dy: Int,
    ) {
        if (originDepth >= MAX_STACK_DEPTH) {
            throw IllegalStateException("origin stack depth exceeded: $MAX_STACK_DEPTH")
        }
        originXStack[originDepth] = originX
        originYStack[originDepth] = originY
        originDepth++
        originX += dx
        originY += dy
    }

    /**
     * Pops the most recent origin offset.
     *
     * @throws IllegalStateException if no origin is pushed.
     * @since 1.0.0
     */
    public fun popOrigin() {
        if (originDepth == 0) {
            throw IllegalStateException("origin stack is empty")
        }
        originDepth--
        originX = originXStack[originDepth]
        originY = originYStack[originDepth]
    }

    private fun writeCell(
        column: Int,
        row: Int,
        codePoint: Int,
        fg: Int,
        bg: Int,
    ) {
        if (column < 0 || column >= matrix.columns || row < 0 || row >= matrix.rows) {
            return
        }
        if (!clip.contains(column, row)) {
            return
        }
        matrix.set(column, row, codePoint, fg, bg)
    }

    private fun writeCellPreservingBackground(
        column: Int,
        row: Int,
        codePoint: Int,
        fg: Int,
    ) {
        if (column < 0 || column >= matrix.columns || row < 0 || row >= matrix.rows) {
            return
        }
        if (!clip.contains(column, row)) {
            return
        }
        matrix.set(column, row, codePoint, fg, matrix.backgroundAt(column, row))
    }

    private fun matrixBounds(): Rect = Rect(0, 0, matrix.columns, matrix.rows)

    private fun requireScalar(codePoint: Int) {
        require(codePoint in 0..0x10FFFF && codePoint !in 0xD800..0xDFFF) {
            "codePoint must be a Unicode scalar value, got U+${codePoint.toString(16).uppercase()}"
        }
    }

    private companion object {
        private const val MAX_STACK_DEPTH = 32
        private const val SPACE = 0x0020
        private const val REPLACEMENT = 0xFFFD
    }
}
