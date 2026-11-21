package com.psiwray.organization.terminal_window_gui.paint

import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes

/**
 * Widget-local drawing API over a [CellCanvas].
 *
 * All coordinates are local to the current origin. The context never exposes
 * absolute screen positions to widgets. Text, fills, and shapes are clipped by
 * the canvas clip stack.
 *
 * @property canvas Destination canvas.
 * @property resolvedShapes Shape catalog resolved against the font.
 * @property theme Theme used for default widget colors.
 * @since 1.0.0
 */
public class PaintContext(
    public val canvas: CellCanvas,
    public val resolvedShapes: ResolvedShapeSet,
    public val theme: Theme = Themes.dark(),
) {
    /**
     * Draws text at local ([x], [y]) and returns the next local column.
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
    ): Int = canvas.text(x, y, text, fg, bg, tabWidth)

    /**
     * Fills [rect] with spaces and the given colors.
     *
     * @since 1.0.0
     */
    public fun fill(
        rect: Rect,
        fg: Int,
        bg: Int,
    ) {
        canvas.fill(rect, SPACE, fg, bg)
    }

    /**
     * Draws [shape] at local ([x], [y]).
     *
     * The resolved code point comes from [resolvedShapes]; widget code never
     * handles glyph code points.
     *
     * @since 1.0.0
     */
    public fun shape(
        shape: Shape,
        x: Int,
        y: Int,
        fg: Int,
        bg: Int,
    ) {
        canvas.set(x, y, resolvedShapes[shape], fg, bg)
    }

    /**
     * Pushes a local origin offset.
     *
     * @since 1.0.0
     */
    public fun pushOrigin(
        dx: Int,
        dy: Int,
    ) {
        canvas.pushOrigin(dx, dy)
    }

    /**
     * Pops the most recent origin offset.
     *
     * @since 1.0.0
     */
    public fun popOrigin() {
        canvas.popOrigin()
    }

    /**
     * Pushes a local clip translated by the current origin.
     *
     * @since 1.0.0
     */
    public fun pushClip(rect: Rect) {
        canvas.pushClip(rect)
    }

    /**
     * Pops the most recent clip.
     *
     * @since 1.0.0
     */
    public fun popClip() {
        canvas.popClip()
    }

    private companion object {
        private const val SPACE = 0x0020
    }
}
