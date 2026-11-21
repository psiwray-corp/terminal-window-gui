package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Tracks the widget that owns the pointer after a button press.
 *
 * While captured, move and release events keep going to [captured] even when
 * the pointer leaves its bounds.
 *
 * @since 1.0.0
 */
public class PointerCapture {
    /** Widget currently receiving captured pointer events, or `null`. @since 1.0.0 */
    public var captured: Widget? = null
        private set

    /** Button that started the capture, or `null`. @since 1.0.0 */
    public var pressedButton: MouseButton? = null
        private set

    /** Whether a widget currently owns the pointer. @since 1.0.0 */
    public val isActive: Boolean get() = captured != null

    /**
     * Captures the pointer for [widget].
     *
     * @since 1.0.0
     */
    public fun capture(
        widget: Widget,
        button: MouseButton? = null,
    ) {
        val old = captured
        if (old !== widget) {
            old?.setPressed(false)
            captured = widget
            widget.setPressed(button != null)
        } else if (button != null) {
            widget.setPressed(true)
        }
        pressedButton = button
    }

    /**
     * Releases the pointer and returns the previously captured widget.
     *
     * @since 1.0.0
     */
    public fun release(): Widget? {
        val old = captured ?: return null
        old.setPressed(false)
        captured = null
        pressedButton = null
        return old
    }

    /** Clears capture and pressed state without dispatching an event. @since 1.0.0 */
    public fun clear() {
        release()
    }
}
