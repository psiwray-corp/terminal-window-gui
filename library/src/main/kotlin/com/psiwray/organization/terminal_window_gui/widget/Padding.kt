package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Non-negative edge insets in cells.
 *
 * @property left Left inset.
 * @property top Top inset.
 * @property right Right inset.
 * @property bottom Bottom inset.
 * @since 1.0.0
 */
public data class Insets(
    public val left: Int,
    public val top: Int,
    public val right: Int,
    public val bottom: Int,
) {
    init {
        require(left >= 0) { "left must be non-negative, got $left" }
        require(top >= 0) { "top must be non-negative, got $top" }
        require(right >= 0) { "right must be non-negative, got $right" }
        require(bottom >= 0) { "bottom must be non-negative, got $bottom" }
    }

    /** Total horizontal inset. @since 1.0.0 */
    public val horizontal: Int get() = left + right

    /** Total vertical inset. @since 1.0.0 */
    public val vertical: Int get() = top + bottom

    /** Shared inset constructors. @since 1.0.0 */
    public companion object {
        /** Uniform insets. @since 1.0.0 */
        public fun all(value: Int): Insets = Insets(value, value, value, value)

        /** Horizontal and vertical insets. @since 1.0.0 */
        public fun symmetric(
            horizontal: Int = 0,
            vertical: Int = 0,
        ): Insets = Insets(horizontal, vertical, horizontal, vertical)

        /** Selected edge insets. @since 1.0.0 */
        public fun only(
            left: Int = 0,
            top: Int = 0,
            right: Int = 0,
            bottom: Int = 0,
        ): Insets = Insets(left, top, right, bottom)
    }
}

/**
 * Shrinks child constraints and clips children to the inset content rect.
 *
 * @property insets Edge insets.
 * @since 1.0.0
 */
public class Padding(
    public val insets: Insets,
    child: Widget? = null,
) : SingleChildWidget(child) {
    /** Uniform-inset constructor. @since 1.0.0 */
    public constructor(
        all: Int,
        child: Widget? = null,
    ) : this(Insets.all(all), child)

    /** Symmetric-inset constructor. @since 1.0.0 */
    public constructor(
        horizontal: Int,
        vertical: Int,
        child: Widget? = null,
    ) : this(Insets.symmetric(horizontal, vertical), child)

    /** Four-edge constructor. @since 1.0.0 */
    public constructor(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        child: Widget? = null,
    ) : this(Insets(left, top, right, bottom), child)

    override fun computeContentRect(bounds: Rect): Rect =
        bounds.inset(insets.left, insets.top, insets.right, insets.bottom)

    override fun measure(constraints: Constraints): IntSize {
        val deflated = deflate(constraints)
        val childSize = child?.measured(deflated) ?: IntSize.ZERO
        return constraints.constrain(
            IntSize(childSize.width + insets.horizontal, childSize.height + insets.vertical),
        )
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        val size = current.measured(Constraints.loose(IntSize(content.width, content.height)))
        current.arrange(Rect(0, 0, size.width, size.height))
    }

    override fun paint(context: PaintContext) {}

    private fun deflate(constraints: Constraints): Constraints {
        val minWidth = (constraints.minWidth - insets.horizontal).coerceAtLeast(0)
        val maxWidth =
            if (constraints.maxWidth == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                (constraints.maxWidth - insets.horizontal).coerceAtLeast(minWidth)
            }
        val minHeight = (constraints.minHeight - insets.vertical).coerceAtLeast(0)
        val maxHeight =
            if (constraints.maxHeight == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                (constraints.maxHeight - insets.vertical).coerceAtLeast(minHeight)
            }
        return Constraints(minWidth, maxWidth, minHeight, maxHeight)
    }
}
