package com.psiwray.organization.terminal_window_gui.input

/**
 * Ways a non-modal overlay may be dismissed.
 *
 * @since 1.0.0
 */
public enum class DismissPolicy {
    /** Dismiss on a pointer press outside the overlay. @since 1.0.0 */
    CLICK_OUTSIDE,

    /** Dismiss on Escape. @since 1.0.0 */
    ESCAPE,

    /** Dismiss on either action. @since 1.0.0 */
    BOTH,

    /** Require an explicit close. @since 1.0.0 */
    NONE,
}
