package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape

/**
 * Cached single-line label layout with optional ellipsis and mnemonic cell.
 *
 * Re-layout happens only when the source string or available width changes, so
 * painting does not allocate a truncated string on every frame.
 *
 * @since 1.0.0
 */
internal class LineLabel {
    private var source: String = ""
    private var available: Int = -1
    private var content: String = ""
    private var contentLength: Int = 0
    private var truncated: Boolean = false

    /** Recomputes the visible content when inputs changed. */
    fun update(
        source: String,
        available: Int,
    ) {
        if (this.source == source && this.available == available) {
            return
        }
        this.source = source
        this.available = available
        val count = TextPainter.codePointCount(source)
        truncated = count > available
        content =
            when {
                available <= 0 -> ""
                truncated -> TextPainter.takeCodePoints(source, available - 1)
                else -> source
            }
        contentLength = TextPainter.codePointCount(content)
    }

    /** Number of visible code points before the ellipsis. */
    fun length(): Int = contentLength

    /** Whether the ellipsis is appended. */
    fun isTruncated(): Boolean = truncated

    /** Code-point index of the first case-insensitive match, or `-1`. */
    fun mnemonicIndex(mnemonic: Char?): Int {
        if (mnemonic == null) {
            return -1
        }
        val lower = mnemonic.lowercaseChar().code
        val upper = mnemonic.uppercaseChar().code
        var index = 0
        var offset = 0
        while (offset < source.length) {
            val codePoint = Character.codePointAt(source, offset)
            if (codePoint == lower || codePoint == upper) {
                return index
            }
            offset += Character.charCount(codePoint)
            index++
        }
        return -1
    }

    /**
     * Draws the cached label. [highlightIndex] is a code-point index in the
     * visible content; an index at or beyond the content is ignored.
     */
    fun draw(
        context: PaintContext,
        x: Int,
        y: Int,
        fg: Int,
        bg: Int,
        highlightIndex: Int = -1,
        highlightFg: Int = fg,
        highlightBg: Int = bg,
    ) {
        if (available <= 0) {
            return
        }
        var column = 0
        var offset = 0
        while (column < contentLength && offset < content.length) {
            val codePoint = Character.codePointAt(content, offset)
            val cellFg = if (column == highlightIndex) highlightFg else fg
            val cellBg = if (column == highlightIndex) highlightBg else bg
            context.canvas.set(x + column, y, codePoint, cellFg, cellBg)
            column++
            offset += Character.charCount(codePoint)
        }
        if (truncated) {
            context.shape(Shape.ELLIPSIS, x + contentLength, y, fg, bg)
        }
    }
}
