package com.psiwray.organization.terminal_window_gui.input

/**
 * Opaque handle for a scheduler entry.
 *
 * @since 1.0.0
 */
public class TimerHandle internal constructor(
    /** Monotonic scheduler identifier. @since 1.0.0 */
    public val id: Long,
) {
    /** Whether this entry has been cancelled or has already run. @since 1.0.0 */
    public val isCancelled: Boolean get() = cancelled

    internal var cancelled: Boolean = false
}
