package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Tightens specified dimensions and otherwise passes constraints through.
 *
 * @property width Fixed width, or `null` to use the child.
 * @property height Fixed height, or `null` to use the child.
 * @since 1.0.0
 */
public class SizedBox(
    public val width: Int? = null,
    public val height: Int? = null,
    child: Widget? = null,
) : SingleChildWidget(child) {
    init {
        require(width == null || width >= 0) { "width must be non-negative, got $width" }
        require(height == null || height >= 0) { "height must be non-negative, got $height" }
    }

    override fun measure(constraints: Constraints): IntSize {
        val tightWidth = width?.coerceIn(constraints.minWidth, constraints.maxWidth)
        val tightHeight = height?.coerceIn(constraints.minHeight, constraints.maxHeight)
        val childConstraints =
            Constraints(
                minWidth = tightWidth ?: constraints.minWidth,
                maxWidth = tightWidth ?: constraints.maxWidth,
                minHeight = tightHeight ?: constraints.minHeight,
                maxHeight = tightHeight ?: constraints.maxHeight,
            )
        val childSize = child?.measured(childConstraints) ?: IntSize.ZERO
        return constraints.constrain(
            IntSize(tightWidth ?: childSize.width, tightHeight ?: childSize.height),
        )
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        current.arrange(Rect(0, 0, content.width, content.height))
    }

    override fun paint(context: PaintContext) {}

    /** Shared constructors. @since 1.0.0 */
    public companion object {
        /** Square sized box. @since 1.0.0 */
        public fun square(
            size: Int,
            child: Widget? = null,
        ): SizedBox = SizedBox(size, size, child)
    }
}
