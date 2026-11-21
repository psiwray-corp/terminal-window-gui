package com.psiwray.organization.terminal_window_gui.layout

/**
 * Immutable half-open rectangle in terminal cells.
 *
 * A rectangle covers columns `x until x + width` and rows
 * `y until y + height`. Width and height must be non-negative.
 *
 * @property x Left column.
 * @property y Top row.
 * @property width Non-negative width in cells.
 * @property height Non-negative height in cells.
 * @since 1.0.0
 */
public data class Rect(
    public val x: Int,
    public val y: Int,
    public val width: Int,
    public val height: Int,
) {
    init {
        require(width >= 0) { "width must be non-negative, got $width" }
        require(height >= 0) { "height must be non-negative, got $height" }
    }

    /** First column after the rectangle. @since 1.0.0 */
    public val right: Int get() = x + width

    /** First row after the rectangle. @since 1.0.0 */
    public val bottom: Int get() = y + height

    /** Returns whether ([column], [row]) lies inside the half-open bounds. @since 1.0.0 */
    public fun contains(
        column: Int,
        row: Int,
    ): Boolean = column >= x && column < right && row >= y && row < bottom

    /**
     * Returns the overlap with [other], or [EMPTY] when they do not overlap.
     *
     * @since 1.0.0
     */
    public fun intersect(other: Rect): Rect {
        val left = maxOf(x, other.x)
        val top = maxOf(y, other.y)
        val nextRight = minOf(right, other.right)
        val nextBottom = minOf(bottom, other.bottom)
        if (nextRight <= left || nextBottom <= top) {
            return EMPTY
        }
        return Rect(left, top, nextRight - left, nextBottom - top)
    }

    /** Returns a copy translated by ([dx], [dy]). @since 1.0.0 */
    public fun offset(
        dx: Int,
        dy: Int,
    ): Rect = Rect(x + dx, y + dy, width, height)

    /** Returns a copy translated into [parent] content coordinates. @since 1.0.0 */
    public fun localToParent(parent: Rect): Rect = offset(parent.x, parent.y)

    /** Returns a copy translated from local to parent coordinates. @since 1.0.0 */
    public fun localToParent(
        parentX: Int,
        parentY: Int,
    ): Rect = offset(parentX, parentY)

    /**
     * Shrinks this rectangle by non-negative edge insets.
     *
     * The resulting width or height is clamped at zero, preserving the
     * top-left corner after the left and top insets.
     *
     * @since 1.0.0
     */
    public fun inset(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): Rect {
        require(left >= 0) { "left inset must be non-negative, got $left" }
        require(top >= 0) { "top inset must be non-negative, got $top" }
        require(right >= 0) { "right inset must be non-negative, got $right" }
        require(bottom >= 0) { "bottom inset must be non-negative, got $bottom" }
        return Rect(
            x = x + left,
            y = y + top,
            width = (width - left - right).coerceAtLeast(0),
            height = (height - top - bottom).coerceAtLeast(0),
        )
    }

    /** Shrinks this rectangle by [amount] on every edge. @since 1.0.0 */
    public fun inset(amount: Int): Rect = inset(amount, amount, amount, amount)

    /** Whether the rectangle has no area. @since 1.0.0 */
    public val isEmpty: Boolean get() = width == 0 || height == 0

    /** Shared empty rectangles and constructors. @since 1.0.0 */
    public companion object {
        /** An empty rectangle at the origin. @since 1.0.0 */
        public val EMPTY: Rect = Rect(0, 0, 0, 0)
    }
}
