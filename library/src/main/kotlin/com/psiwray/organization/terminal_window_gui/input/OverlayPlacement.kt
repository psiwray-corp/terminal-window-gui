package com.psiwray.organization.terminal_window_gui.input

/**
 * Preferred side of an overlay relative to its anchor.
 *
 * @since 1.0.0
 */
public enum class OverlayPlacement {
    /** Prefer above the anchor. @since 1.0.0 */
    ABOVE,

    /** Prefer below the anchor. @since 1.0.0 */
    BELOW,

    /** Prefer left of the anchor. @since 1.0.0 */
    LEFT,

    /** Prefer right of the anchor. @since 1.0.0 */
    RIGHT,

    /** Choose a fitting side automatically. @since 1.0.0 */
    AUTO,
}
