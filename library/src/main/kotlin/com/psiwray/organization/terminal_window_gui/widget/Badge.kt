package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment

/**
 * One-line text badge with a colored background.
 *
 * @property text Badge text.
 * @property background Background color; defaults to the theme accent.
 * @property foreground Text color; defaults to the selection foreground.
 * @since 1.0.0
 */
public class Badge(
    public var text: String = "",
    public var background: Int? = null,
    public var foreground: Int? = null,
) : Widget() {
    override fun measure(constraints: Constraints): IntSize =
        constraints.constrain(IntSize(TextPainter.codePointCount(text).coerceAtLeast(1), 1))

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val bg = background ?: theme.accent
        val fg = foreground ?: theme.selectionForeground
        context.fill(Rect(0, 0, bounds.width, bounds.height), fg, bg)
        TextPainter.draw(
            context = context,
            line = text,
            x = 0,
            y = 0,
            width = bounds.width,
            alignment = TextAlignment.LEFT,
            fg = fg,
            bg = bg,
            ellipsis = TextPainter.codePointCount(text) > bounds.width,
        )
    }
}
