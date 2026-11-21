package com.psiwray.organization.terminal_window_gui.paint

/**
 * Horizontal wrapping policy for [TextMetrics.measure].
 *
 * @since 1.0.0
 */
public enum class TextWrap {
    /** Never wraps; content beyond the width is truncated. @since 1.0.0 */
    NONE,

    /** Breaks at spaces and hard-breaks words wider than the width. @since 1.0.0 */
    WORD,

    /** Breaks at every code-point boundary. @since 1.0.0 */
    CHAR,
}
