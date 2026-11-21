package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextMetrics
import com.psiwray.organization.terminal_window_gui.paint.TextWrap
import kotlin.math.max
import kotlin.math.min

/**
 * Word-wrapped tooltip content.
 *
 * The tooltip itself does not decide when to show. A [TooltipController]
 * anchors it to a widget and observes hover/focus state with the caller-driven
 * scheduler.
 *
 * @property text Tooltip text.
 * @property maxWidth Maximum content width in cells.
 * @since 1.0.0
 */
public class Tooltip(
    text: String,
    maxWidth: Int = 40,
) : Widget() {
    private var textValue: String = text
    private var maxWidthValue: Int = maxWidth
    internal var onDetachHook: (() -> Unit)? = null

    /** Tooltip text. @since 1.0.0 */
    public var text: String
        get() = textValue
        set(value) {
            if (textValue == value) {
                return
            }
            textValue = value
            invalidateLayout()
        }

    /** Alias for [text]. @since 1.0.0 */
    public var content: String
        get() = text
        set(value) {
            text = value
        }

    /** Maximum content width in cells. @since 1.0.0 */
    public var maxWidth: Int
        get() = maxWidthValue
        set(value) {
            require(value > 0) { "maxWidth must be positive, got $value" }
            if (maxWidthValue == value) {
                return
            }
            maxWidthValue = value
            invalidateLayout()
        }

    init {
        require(maxWidth > 0) { "maxWidth must be positive, got $maxWidth" }
        focusable = false
    }

    override fun measure(constraints: Constraints): IntSize {
        val maxAvailable = if (constraints.hasBoundedWidth) min(maxWidth, constraints.maxWidth) else maxWidth
        val layout = layout(max(1, maxAvailable))
        return constraints.constrain(IntSize(max(1, layout.width), max(1, layout.height)))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surfaceAlt)
        val layout = layout(bounds.width)
        var y = 0
        while (y < layout.lines.size && y < bounds.height) {
            TextPainter.draw(
                context = context,
                line = layout.lines[y],
                x = 0,
                y = y,
                width = bounds.width,
                alignment = com.psiwray.organization.terminal_window_gui.paint.TextAlignment.LEFT,
                fg = theme.text,
                bg = theme.surfaceAlt,
                ellipsis = layout.truncated && y == layout.lines.lastIndex,
            )
            y++
        }
    }

    override fun onDetach() {
        onDetachHook?.invoke()
    }

    private fun layout(width: Int) =
        TextMetrics().measure(
            textValue,
            width.coerceAtLeast(1),
            TextWrap.WORD,
        )
}
