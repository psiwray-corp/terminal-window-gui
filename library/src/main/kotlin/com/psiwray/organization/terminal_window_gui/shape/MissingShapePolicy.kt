package com.psiwray.organization.terminal_window_gui.shape

/**
 * Strictness policy for shape resolution.
 *
 * @since 1.0.0
 */
public enum class MissingShapePolicy {
    /**
     * Uses a per-shape fallback, then the global last resort.
     * @since 1.0.0
     */
    FALLBACK,

    /**
     * Throws when any primary code point is absent, even when a fallback is
     * available.
     * @since 1.0.0
     */
    FAIL,
}
