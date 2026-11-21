package com.psiwray.organization.terminal_window_gui.layout

/**
 * Immutable integer-cell layout constraints.
 *
 * Ranges are normalized: negative minima become `0`, and a maximum below its
 * minimum is clamped to that minimum. `maxWidth` and `maxHeight` may be
 * [Int.MAX_VALUE] for bounded-by-content layouts.
 *
 * @since 1.0.0
 */
public class Constraints(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int,
) {
    /** Normalized minimum width. @since 1.0.0 */
    public val minWidth: Int = minWidth.coerceAtLeast(0)

    /** Normalized maximum width. @since 1.0.0 */
    public val maxWidth: Int = maxWidth.coerceAtLeast(this.minWidth)

    /** Normalized minimum height. @since 1.0.0 */
    public val minHeight: Int = minHeight.coerceAtLeast(0)

    /** Normalized maximum height. @since 1.0.0 */
    public val maxHeight: Int = maxHeight.coerceAtLeast(this.minHeight)

    /** Whether the width maximum is finite. @since 1.0.0 */
    public val hasBoundedWidth: Boolean get() = maxWidth != Int.MAX_VALUE

    /** Whether the height maximum is finite. @since 1.0.0 */
    public val hasBoundedHeight: Boolean get() = maxHeight != Int.MAX_VALUE

    /** Constraints with zero minima and the same maxima. @since 1.0.0 */
    public fun loosen(): Constraints = Constraints(0, maxWidth, 0, maxHeight)

    /**
     * Tightens both axes to their bounded maxima.
     *
     * Unbounded axes tighten to their minimum.
     *
     * @since 1.0.0
     */
    public fun tighten(): Constraints {
        val width = if (hasBoundedWidth) maxWidth else minWidth
        val height = if (hasBoundedHeight) maxHeight else minHeight
        return Constraints(width, width, height, height)
    }

    /** Tightens both axes to [size]. @since 1.0.0 */
    public fun tighten(size: IntSize): Constraints = Constraints(size.width, size.width, size.height, size.height)

    /** Clamps [size] into these constraints. @since 1.0.0 */
    public fun constrain(size: IntSize): IntSize =
        IntSize(
            width = size.width.coerceIn(minWidth, maxWidth),
            height = size.height.coerceIn(minHeight, maxHeight),
        )

    /** Destructuring component for [minWidth]. @since 1.0.0 */
    public operator fun component1(): Int = minWidth

    /** Destructuring component for [maxWidth]. @since 1.0.0 */
    public operator fun component2(): Int = maxWidth

    /** Destructuring component for [minHeight]. @since 1.0.0 */
    public operator fun component3(): Int = minHeight

    /** Destructuring component for [maxHeight]. @since 1.0.0 */
    public operator fun component4(): Int = maxHeight

    /** Returns a copy with selected values replaced and normalized. @since 1.0.0 */
    public fun copy(
        minWidth: Int = this.minWidth,
        maxWidth: Int = this.maxWidth,
        minHeight: Int = this.minHeight,
        maxHeight: Int = this.maxHeight,
    ): Constraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)

    /** Value equality over normalized bounds. @since 1.0.0 */
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is Constraints &&
                    minWidth == other.minWidth &&
                    maxWidth == other.maxWidth &&
                    minHeight == other.minHeight &&
                    maxHeight == other.maxHeight
            )

    /** Value hash over normalized bounds. @since 1.0.0 */
    override fun hashCode(): Int {
        var result = minWidth
        result = 31 * result + maxWidth
        result = 31 * result + minHeight
        result = 31 * result + maxHeight
        return result
    }

    /** Diagnostic representation. @since 1.0.0 */
    override fun toString(): String =
        "Constraints(minWidth=$minWidth, maxWidth=$maxWidth, " +
            "minHeight=$minHeight, maxHeight=$maxHeight)"

    /** Shared constructors. @since 1.0.0 */
    public companion object {
        /** Tight constraints for [size]. @since 1.0.0 */
        public fun tight(size: IntSize): Constraints = Constraints(size.width, size.width, size.height, size.height)

        /** Zero-minimum constraints with maximum [size]. @since 1.0.0 */
        public fun loose(size: IntSize): Constraints = Constraints(0, size.width, 0, size.height)

        /** Constraints with no finite maxima. @since 1.0.0 */
        public fun unbounded(): Constraints = Constraints(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
    }
}
