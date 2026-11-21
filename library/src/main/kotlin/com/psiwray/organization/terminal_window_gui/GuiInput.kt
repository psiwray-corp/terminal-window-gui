package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.FocusEvent
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window.input.MouseMoveEvent
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiFocusEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent

/**
 * Pure translation from host `terminal-window` input events to GUI events.
 *
 * The translator only copies immutable values and never installs a listener,
 * polls devices, or owns a window. Cell coordinates are used as both local and
 * screen coordinates; the surface router converts them to widget-local
 * coordinates at each routing hop.
 *
 * @since 1.0.0
 */
public object GuiInput {
    /**
     * Translates a host key event.
     *
     * @since 1.0.0
     */
    public fun key(event: KeyEvent): GuiKeyEvent =
        GuiKeyEvent(
            key = event.key,
            action = event.action,
            modifiers = event.modifiers,
            repeat = event.repeat,
        )

    /**
     * Translates a host text event.
     *
     * @since 1.0.0
     */
    public fun char(event: CharEvent): GuiCharEvent = GuiCharEvent(event.codePoint)

    /**
     * Translates a host cursor move using its cell coordinates.
     *
     * @since 1.0.0
     */
    public fun move(event: MouseMoveEvent): GuiPointerMoveEvent =
        GuiPointerMoveEvent(
            localX = event.column,
            localY = event.row,
            screenX = event.column,
            screenY = event.row,
            modifiers = Modifiers.NONE,
        )

    /**
     * Translates a host mouse-button event using its cell coordinates.
     *
     * The host event has no click counter; the surface router derives the
     * double/triple-click count from the press stream.
     *
     * @since 1.0.0
     */
    public fun button(event: MouseButtonEvent): GuiPointerButtonEvent =
        GuiPointerButtonEvent(
            button = event.button,
            action = event.action,
            modifiers = event.modifiers,
            localX = event.column,
            localY = event.row,
            screenX = event.column,
            screenY = event.row,
            clickCount = 1,
        )

    /**
     * Translates a host scroll event at the supplied cell position.
     *
     * Host scroll events carry no position, so the caller supplies the last
     * pointer cell.
     *
     * @since 1.0.0
     */
    public fun scroll(
        event: ScrollEvent,
        cellX: Int,
        cellY: Int,
    ): GuiScrollEvent =
        GuiScrollEvent(
            dx = event.xOffset,
            dy = event.yOffset,
            localX = cellX,
            localY = cellY,
        )

    /**
     * Translates a host focus event.
     *
     * @since 1.0.0
     */
    public fun focus(event: FocusEvent): GuiFocusEvent = GuiFocusEvent(event.focused)
}
