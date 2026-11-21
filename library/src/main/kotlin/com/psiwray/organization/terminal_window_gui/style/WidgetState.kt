package com.psiwray.organization.terminal_window_gui.style

/**
 * Resolvable visual state of a widget.
 *
 * @since 1.0.0
 */
public enum class WidgetState {
    /** Resting state. @since 1.0.0 */
    NORMAL,

    /** Keyboard focus. @since 1.0.0 */
    FOCUSED,

    /** Pointer hover. @since 1.0.0 */
    HOVERED,

    /** Active press. @since 1.0.0 */
    PRESSED,

    /** Input disabled. @since 1.0.0 */
    DISABLED,

    /** Selected item. @since 1.0.0 */
    SELECTED,

    /** Validation error. @since 1.0.0 */
    ERROR,
}
