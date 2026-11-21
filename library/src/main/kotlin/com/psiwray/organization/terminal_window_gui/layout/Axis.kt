package com.psiwray.organization.terminal_window_gui.layout

/**
 * Layout axis for flex containers.
 *
 * @since 1.0.0
 */
public enum class Axis {
    /** Horizontal main axis. @since 1.0.0 */
    HORIZONTAL,

    /** Vertical main axis. @since 1.0.0 */
    VERTICAL,

    ;

    /** Returns the main-axis extent of [size]. @since 1.0.0 */
    public fun main(size: IntSize): Int = if (this == HORIZONTAL) size.width else size.height

    /** Returns the cross-axis extent of [size]. @since 1.0.0 */
    public fun cross(size: IntSize): Int = if (this == HORIZONTAL) size.height else size.width

    /** Builds a size from main and cross extents. @since 1.0.0 */
    public fun size(
        main: Int,
        cross: Int,
    ): IntSize = if (this == HORIZONTAL) IntSize(main, cross) else IntSize(cross, main)
}

/**
 * Whether a flex container shrinks to its children or fills the main axis.
 *
 * @since 1.0.0
 */
public enum class MainAxisSize {
    /** Uses the preferred child extent. @since 1.0.0 */
    MIN,

    /** Fills the bounded main-axis maximum. @since 1.0.0 */
    MAX,
}
