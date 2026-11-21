package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.paint.TextLayout
import com.psiwray.organization.terminal_window_gui.paint.TextMetrics
import com.psiwray.organization.terminal_window_gui.paint.TextWrap

/**
 * Text display with optional wrapping, alignment, line limits, and ellipsis.
 *
 * Text uses the host's one-cell-per-code-point model. Measurement and painting
 * never split surrogate pairs. When content is clipped, the last painted line
 * ends with [com.psiwray.organization.terminal_window_gui.shape.Shape.ELLIPSIS].
 *
 * @property text Text to display.
 * @property alignment Horizontal alignment within the widget.
 * @property wrap Wrapping policy.
 * @property maxLines Maximum painted lines; must be positive.
 * @property ellipsis Whether clipped content ends with an ellipsis.
 * @property foreground Optional text color.
 * @property background Optional text background.
 * @since 1.0.0
 */
public class Label(
    text: String = "",
    public var alignment: TextAlignment = TextAlignment.LEFT,
    public var wrap: TextWrap = TextWrap.NONE,
    public var maxLines: Int = Int.MAX_VALUE,
    public var ellipsis: Boolean = true,
    public var foreground: Int? = null,
    public var background: Int? = null,
) : Widget() {
    private var textValue: String = text

    /** Text to display. @since 1.0.0 */
    public var text: String
        get() = textValue
        set(value) {
            if (textValue == value) {
                return
            }
            textValue = value
            invalidateLayout()
        }

    init {
        require(maxLines > 0) { "maxLines must be positive, got $maxLines" }
    }

    override fun measure(constraints: Constraints): IntSize {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val layout = layout(maxWidth)
        val width = layout.width.coerceAtLeast(1)
        val height = layout.height.coerceAtLeast(1)
        return constraints.constrain(IntSize(width, height))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val fg = foreground ?: theme.text
        val bg = background
        if (bg != null) {
            context.fill(
                com.psiwray.organization.terminal_window_gui.layout
                    .Rect(0, 0, bounds.width, bounds.height),
                fg,
                bg,
            )
        }
        val layout = layout(bounds.width)
        var index = 0
        while (index < layout.lines.size && index < bounds.height) {
            TextPainter.draw(
                context = context,
                line = layout.lines[index],
                x = 0,
                y = index,
                width = bounds.width,
                alignment = alignment,
                fg = fg,
                bg = bg,
                ellipsis = ellipsis && layout.truncated && index == layout.lines.lastIndex,
            )
            index++
        }
    }

    private fun layout(width: Int): TextLayout = metrics().measure(textValue, width, wrap, maxLines)

    private fun metrics(): TextMetrics = TextMetrics()
}
