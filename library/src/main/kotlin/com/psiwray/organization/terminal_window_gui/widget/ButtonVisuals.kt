package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.style.Style
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.WidgetState

/**
 * Resolves button states without relying on color alone.
 *
 * @since 1.0.0
 */
internal object ButtonVisuals {
    /** State priority: disabled, pressed, focused, hovered, normal. */
    fun state(button: Button): WidgetState =
        when {
            !button.enabled -> WidgetState.DISABLED
            button.isPressed -> WidgetState.PRESSED
            button.isFocused -> WidgetState.FOCUSED
            button.isHovered -> WidgetState.HOVERED
            else -> WidgetState.NORMAL
        }

    /** Resolves the style for [state]. */
    fun style(
        theme: Theme,
        state: WidgetState,
    ): Style = theme.style(state)

    /** Left padding marker; `*` marks default, `>` marks focus. */
    fun leftMarker(button: Button): Char =
        when {
            button.default -> '*'
            button.isFocused -> '>'
            else -> ' '
        }
}
