package com.psiwray.organization.terminal_window_gui.widget

/**
 * Selection policy for collection widgets.
 *
 * @since 1.0.0
 */
public enum class SelectionMode {
    /** The collection tracks a cursor but cannot select keys. @since 1.0.0 */
    NONE,

    /** Exactly one key may be selected; the cursor is always selected. @since 1.0.0 */
    SINGLE,

    /** Any number of keys may be selected; the cursor may be unselected. @since 1.0.0 */
    MULTIPLE,
}
