package com.psiwray.organization.terminal_window_gui.layout

import kotlin.math.roundToInt

/**
 * Fractional two-dimensional alignment inside a box.
 *
 * Both components are in `0.0..1.0`: `0.0` is the start edge and `1.0` the
 * end edge. Integer offsets are rounded deterministically and clamped at zero.
 *
 * @property x Horizontal fraction.
 * @property y Vertical fraction.
 * @since 1.0.0
 */
public data class Alignment(
    public val x: Float,
    public val y: Float,
) {
    /** Double-precision convenience constructor. @since 1.0.0 */
    public constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())

    init {
        require(x.isFinite() && x in 0f..1f) { "x must be in 0.0..1.0, got $x" }
        require(y.isFinite() && y in 0f..1f) { "y must be in 0.0..1.0, got $y" }
    }

    /** Returns the horizontal offset for a child of [child] inside [available]. @since 1.0.0 */
    public fun offsetX(
        available: Int,
        child: Int,
    ): Int = ((available - child) * x).roundToInt().coerceAtLeast(0)

    /** Returns the vertical offset for a child of [child] inside [available]. @since 1.0.0 */
    public fun offsetY(
        available: Int,
        child: Int,
    ): Int = ((available - child) * y).roundToInt().coerceAtLeast(0)

    /** Shared alignments. @since 1.0.0 */
    public companion object {
        /** Top-left alignment. @since 1.0.0 */
        public val TOP_START: Alignment = Alignment(0f, 0f)

        /** Top-center alignment. @since 1.0.0 */
        public val TOP_CENTER: Alignment = Alignment(0.5f, 0f)

        /** Top-right alignment. @since 1.0.0 */
        public val TOP_END: Alignment = Alignment(1f, 0f)

        /** Center-left alignment. @since 1.0.0 */
        public val CENTER_START: Alignment = Alignment(0f, 0.5f)

        /** Center alignment. @since 1.0.0 */
        public val CENTER: Alignment = Alignment(0.5f, 0.5f)

        /** Center-right alignment. @since 1.0.0 */
        public val CENTER_END: Alignment = Alignment(1f, 0.5f)

        /** Bottom-left alignment. @since 1.0.0 */
        public val BOTTOM_START: Alignment = Alignment(0f, 1f)

        /** Bottom-center alignment. @since 1.0.0 */
        public val BOTTOM_CENTER: Alignment = Alignment(0.5f, 1f)

        /** Bottom-right alignment. @since 1.0.0 */
        public val BOTTOM_END: Alignment = Alignment(1f, 1f)

        /** Alias for [TOP_START]. @since 1.0.0 */
        public val TOP_LEFT: Alignment get() = TOP_START

        /** Alias for [TOP_END]. @since 1.0.0 */
        public val TOP_RIGHT: Alignment get() = TOP_END

        /** Alias for [BOTTOM_START]. @since 1.0.0 */
        public val BOTTOM_LEFT: Alignment get() = BOTTOM_START

        /** Alias for [BOTTOM_END]. @since 1.0.0 */
        public val BOTTOM_RIGHT: Alignment get() = BOTTOM_END

        /** Alias for [CENTER_START]. @since 1.0.0 */
        public val CENTER_LEFT: Alignment get() = CENTER_START

        /** Alias for [CENTER_END]. @since 1.0.0 */
        public val CENTER_RIGHT: Alignment get() = CENTER_END
    }
}

/**
 * Main-axis alignment in Row, Column, and Wrap.
 *
 * @since 1.0.0
 */
public enum class MainAxisAlignment {
    /** Place at the start edge. @since 1.0.0 */
    START,

    /** Center the run. @since 1.0.0 */
    CENTER,

    /** Place at the end edge. @since 1.0.0 */
    END,

    /** Distribute free space between children. @since 1.0.0 */
    SPACE_BETWEEN,
}

/**
 * Cross-axis alignment in Row, Column, and Wrap.
 *
 * @since 1.0.0
 */
public enum class CrossAxisAlignment {
    /** Align at the cross-axis start. @since 1.0.0 */
    START,

    /** Center on the cross axis. @since 1.0.0 */
    CENTER,

    /** Align at the cross-axis end. @since 1.0.0 */
    END,

    /** Stretch to the cross-axis extent. @since 1.0.0 */
    STRETCH,
}
