package com.psiwray.organization.terminal_window_gui.widget

/**
 * Display state for the active table sort indicator.
 *
 * Sorting itself is application-owned; tables never reorder data.
 *
 * @since 1.0.0
 */
public enum class SortDirection {
    /** Ascending indicator. @since 1.0.0 */
    ASCENDING,

    /** Descending indicator. @since 1.0.0 */
    DESCENDING,
}
