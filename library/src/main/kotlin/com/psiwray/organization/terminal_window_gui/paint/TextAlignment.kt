package com.psiwray.organization.terminal_window_gui.paint

/**
 * Horizontal alignment applied by widgets after [TextMetrics] measurement.
 *
 * Text metrics never align content; widgets choose the starting column.
 *
 * @since 1.0.0
 */
public enum class TextAlignment {
    /** Start at the left edge. @since 1.0.0 */
    LEFT,

    /** Center within the available width. @since 1.0.0 */
    CENTER,

    /** End at the right edge. @since 1.0.0 */
    RIGHT,
}
