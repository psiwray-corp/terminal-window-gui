package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape

internal object TextPainter {
    fun draw(
        context: PaintContext,
        line: String,
        x: Int,
        y: Int,
        width: Int,
        alignment: TextAlignment,
        fg: Int,
        bg: Int?,
        ellipsis: Boolean,
    ) {
        if (width <= 0) {
            return
        }
        var content = line
        var contentWidth = codePointCount(content)
        if (ellipsis && contentWidth >= width) {
            content = takeCodePoints(content, (width - 1).coerceAtLeast(0))
            contentWidth = codePointCount(content)
        }
        val start = x + alignmentOffset(alignment, width, contentWidth)
        if (bg == null) {
            context.canvas.textPreservingBackground(start, y, content, fg)
        } else {
            context.text(start, y, content, fg, bg)
        }
        if (ellipsis && contentWidth < width) {
            if (bg == null) {
                context.canvas.setPreservingBackground(
                    start + contentWidth,
                    y,
                    context.resolvedShapes[Shape.ELLIPSIS],
                    fg,
                )
            } else {
                context.shape(Shape.ELLIPSIS, start + contentWidth, y, fg, bg)
            }
        }
    }

    fun drawSpanLine(
        context: PaintContext,
        line: String,
        sourceStart: Int,
        x: Int,
        y: Int,
        width: Int,
        alignment: TextAlignment,
        spans: List<SpanColor>,
        ellipsis: Boolean,
        forceEllipsis: Boolean,
        defaultFg: Int,
        defaultBg: Int?,
    ) {
        if (width <= 0) {
            return
        }
        val contentWidth = codePointCount(line)
        val clipped = ellipsis && contentWidth >= width
        val drawWidth = if (clipped) (width - 1).coerceAtLeast(0) else contentWidth
        val start = x + alignmentOffset(alignment, width, contentWidth.coerceAtMost(width))
        var column = 0
        var index = 0
        while (index < line.length && column < drawWidth) {
            val codePoint = Character.codePointAt(line, index)
            val span = spanAt(spans, sourceStart + column)
            val fg = span?.fg ?: defaultFg
            val bg = span?.bg ?: defaultBg
            if (bg == null) {
                context.canvas.setPreservingBackground(start + column, y, codePoint, fg)
            } else {
                context.canvas.set(start + column, y, codePoint, fg, bg)
            }
            column++
            index += Character.charCount(codePoint)
        }
        if (clipped || (ellipsis && forceEllipsis)) {
            val ellipsisColumn = if (clipped) drawWidth else contentWidth.coerceAtMost(width - 1)
            val span = spanAt(spans, sourceStart + ellipsisColumn)
            val ellipsisFg = span?.fg ?: defaultFg
            val ellipsisBg = span?.bg ?: defaultBg
            if (ellipsisBg == null) {
                context.canvas.setPreservingBackground(
                    start + ellipsisColumn,
                    y,
                    context.resolvedShapes[Shape.ELLIPSIS],
                    ellipsisFg,
                )
            } else {
                context.shape(Shape.ELLIPSIS, start + ellipsisColumn, y, ellipsisFg, ellipsisBg)
            }
        }
    }

    fun codePointCount(text: CharSequence): Int = Character.codePointCount(text, 0, text.length)

    fun takeCodePoints(
        text: CharSequence,
        count: Int,
    ): String {
        if (count <= 0) {
            return ""
        }
        val result = StringBuilder()
        var index = 0
        var taken = 0
        while (index < text.length && taken < count) {
            val codePoint = Character.codePointAt(text, index)
            result.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
            taken++
        }
        return result.toString()
    }

    private fun alignmentOffset(
        alignment: TextAlignment,
        width: Int,
        contentWidth: Int,
    ): Int {
        val free = (width - contentWidth).coerceAtLeast(0)
        return when (alignment) {
            TextAlignment.LEFT -> 0
            TextAlignment.CENTER -> free / 2
            TextAlignment.RIGHT -> free
        }
    }

    private fun spanAt(
        spans: List<SpanColor>,
        offset: Int,
    ): SpanColor? {
        var index = 0
        while (index < spans.size) {
            val span = spans[index]
            if (offset >= span.start && offset < span.end) {
                return span
            }
            index++
        }
        return null
    }
}

internal class SpanColor(
    val start: Int,
    val end: Int,
    val fg: Int?,
    val bg: Int?,
)
