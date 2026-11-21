package com.psiwray.organization.terminal_window_gui.widget

/**
 * When a scrollbar is shown.
 *
 * @since 1.0.0
 */
public enum class ScrollBarVisibility {
    /** Always reserve and paint the bar. @since 1.0.0 */
    ALWAYS,

    /** Show the bar only when content exceeds the viewport. @since 1.0.0 */
    AUTO,

    /** Never reserve or paint the bar. @since 1.0.0 */
    NEVER,
}
