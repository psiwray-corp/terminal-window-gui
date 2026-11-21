package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape

/**
 * One-cell semantic shape or explicit glyph.
 *
 * A [shape] resolves through the surface shape catalog. A [glyph] is written
 * directly and relies on the host font fallback. No image support exists.
 *
 * @property shape Semantic shape, if any.
 * @property glyph Explicit code point, if any.
 * @property foreground Optional foreground color.
 * @property background Optional background color.
 * @since 1.0.0
 */
public class Icon(
    public val shape: Shape? = null,
    public val glyph: Int? = null,
    public var foreground: Int? = null,
    public var background: Int? = null,
) : Widget() {
    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(1, 1))

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val fg = foreground ?: theme.text
        val bg = background ?: theme.surface
        if (background != null) {
            context.fill(Rect(0, 0, bounds.width, bounds.height), fg, bg)
        }
        when {
            shape != null -> context.shape(shape, 0, 0, fg, bg)
            glyph != null -> context.canvas.set(0, 0, glyph, fg, bg)
            else -> context.canvas.set(0, 0, SPACE, fg, bg)
        }
    }

    /** Factories for explicit glyph icons. @since 1.0.0 */
    public companion object {
        /** Creates an icon from an explicit code point. @since 1.0.0 */
        public fun glyph(codePoint: Int): Icon = Icon(glyph = codePoint)

        /** Creates an icon from a character. @since 1.0.0 */
        public fun glyph(value: Char): Icon = Icon(glyph = value.code)

        private const val SPACE: Int = 0x0020
    }
}
