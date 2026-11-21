package com.psiwray.organization.terminal_window_gui.widget

/**
 * Pane axis for [SplitPane].
 *
 * [HORIZONTAL] places the panes side by side with a vertical one-cell divider.
 * [VERTICAL] stacks the panes with a horizontal one-cell divider.
 *
 * @since 1.0.0
 */
public enum class SplitOrientation {
    /** First pane left of second; divider runs vertically. @since 1.0.0 */
    HORIZONTAL,

    /** First pane above second; divider runs horizontally. @since 1.0.0 */
    VERTICAL,
}
