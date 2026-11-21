package com.psiwray.organization.terminal_window_gui.input

/**
 * How a subtree participates in Tab traversal.
 *
 * @since 1.0.0
 */
public enum class FocusScope {
    /** Include this widget and its subtree in the normal traversal order. @since 1.0.0 */
    ENTER,

    /** Exclude this widget and its subtree from traversal. @since 1.0.0 */
    SKIP,

    /** Keep traversal inside this subtree until focus leaves the scope. @since 1.0.0 */
    TRAP,
}
