package com.psiwray.organization.terminal_window_gui.layout

/**
 * Immutable integer size in terminal cells.
 *
 * @property width Non-negative width in cells.
 * @property height Non-negative height in cells.
 * @since 1.0.0
 */
public data class IntSize(
    public val width: Int,
    public val height: Int,
) {
    init {
        require(width >= 0) { "width must be non-negative, got $width" }
        require(height >= 0) { "height must be non-negative, got $height" }
    }

    /** Shared `(0, 0)` size. @since 1.0.0 */
    public companion object {
        /** A zero width and height. @since 1.0.0 */
        public val ZERO: IntSize = IntSize(0, 0)
    }
}
