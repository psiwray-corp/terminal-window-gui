package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Adds minimum and maximum clamps to child constraints.
 *
 * @property minWidth Additional minimum width, or `null`.
 * @property maxWidth Additional maximum width, or `null`.
 * @property minHeight Additional minimum height, or `null`.
 * @property maxHeight Additional maximum height, or `null`.
 * @since 1.0.0
 */
public class ConstrainedBox(
    public val minWidth: Int? = null,
    public val maxWidth: Int? = null,
    public val minHeight: Int? = null,
    public val maxHeight: Int? = null,
    child: Widget? = null,
) : SingleChildWidget(child) {
    init {
        require(minWidth == null || minWidth >= 0) { "minWidth must be non-negative, got $minWidth" }
        require(maxWidth == null || maxWidth >= 0) { "maxWidth must be non-negative, got $maxWidth" }
        require(minHeight == null || minHeight >= 0) { "minHeight must be non-negative, got $minHeight" }
        require(maxHeight == null || maxHeight >= 0) { "maxHeight must be non-negative, got $maxHeight" }
    }

    override fun measure(constraints: Constraints): IntSize {
        val childConstraints = applyTo(constraints)
        val size = child?.measured(childConstraints) ?: childConstraints.constrain(IntSize.ZERO)
        return constraints.constrain(size)
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        val size = current.measured(applyTo(Constraints.loose(IntSize(content.width, content.height))))
        current.arrange(Rect(0, 0, size.width, size.height))
    }

    override fun paint(context: PaintContext) {}

    private fun applyTo(constraints: Constraints): Constraints {
        val nextMinWidth = maxOf(constraints.minWidth, minWidth ?: 0)
        val nextMaxWidth =
            minOf(constraints.maxWidth, maxWidth ?: Int.MAX_VALUE).coerceAtLeast(nextMinWidth)
        val nextMinHeight = maxOf(constraints.minHeight, minHeight ?: 0)
        val nextMaxHeight =
            minOf(constraints.maxHeight, maxHeight ?: Int.MAX_VALUE).coerceAtLeast(nextMinHeight)
        return Constraints(nextMinWidth, nextMaxWidth, nextMinHeight, nextMaxHeight)
    }
}
