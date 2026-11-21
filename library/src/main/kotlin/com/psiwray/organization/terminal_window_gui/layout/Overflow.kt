package com.psiwray.organization.terminal_window_gui.layout

/**
 * How a container treats children that exceed its content rect.
 *
 * @since 1.0.0
 */
public enum class Overflow {
    /** Clip to the content rect. @since 1.0.0 */
    CLIP,

    /** Let children paint outside the content rect. @since 1.0.0 */
    VISIBLE,
}
