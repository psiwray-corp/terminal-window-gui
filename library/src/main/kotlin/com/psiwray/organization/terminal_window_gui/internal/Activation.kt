package com.psiwray.organization.terminal_window_gui.internal

/**
 * Press/release state for a one-shot activation control.
 *
 * A press inside the control arms it. A release inside activates exactly once;
 * a release outside or a cancellation disarms it without activation.
 *
 * @since 1.0.0
 */
internal class Activation {
    private var armed: Boolean = false

    /** Arms the control when [inside] is true. */
    fun press(inside: Boolean): Boolean {
        armed = inside
        return armed
    }

    /** Disarms the control and returns whether the release activates. */
    fun release(inside: Boolean): Boolean {
        if (!armed) {
            return false
        }
        armed = false
        return inside
    }

    /** Cancels a pending press. */
    fun cancel() {
        armed = false
    }
}
