package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.SpanColor
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.paint.TextLayout
import com.psiwray.organization.terminal_window_gui.paint.TextMetrics
import com.psiwray.organization.terminal_window_gui.paint.TextWrap

/**
 * Text display with ordered foreground/background color spans.
 *
 * Spans flow through the same wrapping and truncation rules as [Label]. Colors
 * are the only span attribute; no weight, italic, or underline exists.
 *
 * @property spans Ordered color runs.
 * @property alignment Horizontal alignment within the widget.
 * @property wrap Wrapping policy.
 * @property maxLines Maximum painted lines; must be positive.
 * @property ellipsis Whether clipped content ends with an ellipsis.
 * @property background Optional widget background.
 * @since 1.0.0
 */
public class RichLabel(
    public var spans: List<TextSpan> = emptyList(),
    public var alignment: TextAlignment = TextAlignment.LEFT,
    public var wrap: TextWrap = TextWrap.WORD,
    public var maxLines: Int = Int.MAX_VALUE,
    public var ellipsis: Boolean = true,
    public var background: Int? = null,
) : Widget() {
    init {
        require(maxLines > 0) { "maxLines must be positive, got $maxLines" }
    }

    override fun measure(constraints: Constraints): IntSize {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val layout = layout(maxWidth)
        return constraints.constrain(
            IntSize(
                layout.width.coerceAtLeast(1),
                layout.height.coerceAtLeast(1),
            ),
        )
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val defaultBg = background
        val widgetBackground = background
        if (widgetBackground != null) {
            context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, widgetBackground)
        }
        val full = fullText()
        val colors = spanColors()
        val layout = layout(bounds.width)
        var searchFrom = 0
        var index = 0
        while (index < layout.lines.size && index < bounds.height) {
            val line = layout.lines[index]
            val found = full.indexOf(line, searchFrom)
            val charStart = if (found >= 0) found else searchFrom
            if (found >= 0) {
                searchFrom = found + line.length
            }
            val sourceStart = codePointOffset(full, charStart)
            TextPainter.drawSpanLine(
                context = context,
                line = line,
                sourceStart = sourceStart,
                x = 0,
                y = index,
                width = bounds.width,
                alignment = alignment,
                spans = colors,
                ellipsis = ellipsis && layout.truncated && index == layout.lines.lastIndex,
                forceEllipsis = layout.truncated && index == layout.lines.lastIndex,
                defaultFg = theme.text,
                defaultBg = defaultBg,
            )
            index++
        }
    }

    private fun layout(width: Int): TextLayout = TextMetrics().measure(fullText(), width, wrap, maxLines)

    private fun fullText(): String {
        if (spans.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        for (span in spans) {
            builder.append(span.text)
        }
        return builder.toString()
    }

    private fun spanColors(): List<SpanColor> {
        val result = ArrayList<SpanColor>(spans.size)
        var offset = 0
        for (span in spans) {
            val count = TextPainter.codePointCount(span.text)
            result.add(SpanColor(offset, offset + count, span.fg, span.bg))
            offset += count
        }
        return result
    }

    private fun codePointOffset(
        text: String,
        charIndex: Int,
    ): Int = Character.codePointCount(text, 0, charIndex)
}
