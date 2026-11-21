package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.BoxPainter
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * One-cell-thick horizontal or vertical rule with an optional centered label.
 *
 * @property orientation Rule axis.
 * @property label Optional horizontal label.
 * @property style Border line style.
 * @since 1.0.0
 */
public class Separator(
    public val orientation: Orientation = Orientation.HORIZONTAL,
    public var label: String? = null,
    public val style: BoxStyle = BoxStyle.SINGLE,
) : Widget() {
    override fun measure(constraints: Constraints): IntSize {
        val labelWidth = label?.let(TextPainter::codePointCount) ?: 0
        val width =
            if (orientation == Orientation.HORIZONTAL) {
                if (constraints.hasBoundedWidth) constraints.maxWidth else labelWidth.coerceAtLeast(1)
            } else {
                1
            }
        val height =
            if (orientation == Orientation.VERTICAL) {
                if (constraints.hasBoundedHeight) constraints.maxHeight else labelWidth.coerceAtLeast(1)
            } else {
                1
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val fg = theme.border
        val bg = theme.surface
        if (orientation == Orientation.HORIZONTAL) {
            var x = 0
            while (x < bounds.width) {
                context.shape(BoxPainter.horizontal(style), x, 0, fg, bg)
                x++
            }
            val text = label ?: return
            val width = TextPainter.codePointCount(text)
            if (width == 0 || bounds.width == 0) {
                return
            }
            val start = ((bounds.width - width) / 2).coerceAtLeast(0)
            val clipped = TextPainter.takeCodePoints(text, (bounds.width - start).coerceAtLeast(0))
            context.text(start, 0, clipped, theme.text, bg)
        } else {
            var y = 0
            while (y < bounds.height) {
                context.shape(BoxPainter.vertical(style), 0, y, fg, bg)
                y++
            }
            val text = label
            if (!text.isNullOrEmpty() && bounds.height > 0) {
                val clipped = TextPainter.takeCodePoints(text, 1)
                context.text(0, bounds.height / 2, clipped, theme.text, bg)
            }
        }
    }
}
