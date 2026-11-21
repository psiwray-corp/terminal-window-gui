package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.max

/**
 * One transient notification row.
 *
 * Toasts paint a severity icon, message, and close indicator. They never take
 * focus and never own a timer; [ToastManager] schedules auto-dismissal through
 * the caller-driven scheduler.
 *
 * @property message Notification text.
 * @property severity Severity color and icon.
 * @property durationMillis Auto-dismiss delay in milliseconds.
 * @since 1.0.0
 */
public class Toast(
    message: String,
    public var severity: Severity = Severity.INFO,
    public var durationMillis: Long = 3000,
) : Widget() {
    private var messageValue: String = message
    private var dismissed: Boolean = false

    /** Called once when the close cell is activated. @since 1.0.0 */
    public var onClose: (() -> Unit)? = null

    init {
        require(durationMillis >= 0) { "durationMillis must be non-negative, got $durationMillis" }
    }

    /** Notification text. @since 1.0.0 */
    public var message: String
        get() = messageValue
        set(value) {
            if (messageValue == value) {
                return
            }
            messageValue = value
            invalidateLayout()
        }

    /** Whether this toast has already requested dismissal. @since 1.0.0 */
    public val isDismissed: Boolean get() = dismissed

    /** Requests manual dismissal; idempotent. @since 1.0.0 */
    public fun dismiss() {
        if (dismissed) {
            return
        }
        dismissed = true
        onClose?.invoke()
    }

    override fun measure(constraints: Constraints): IntSize =
        constraints.constrain(IntSize(max(4, TextPainter.codePointCount(messageValue) + 4), 1))

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val bg = theme.surfaceAlt
        val fg = theme.text
        context.fill(Rect(0, 0, bounds.width, 1), fg, bg)
        context.shape(severityShape(), 0, 0, severityColor(theme), bg)
        val messageWidth = (bounds.width - 4).coerceAtLeast(0)
        TextPainter.draw(
            context = context,
            line = messageValue,
            x = 2,
            y = 0,
            width = messageWidth,
            alignment = TextAlignment.LEFT,
            fg = fg,
            bg = bg,
            ellipsis = TextPainter.codePointCount(messageValue) > messageWidth,
        )
        if (bounds.width > 1) {
            context.shape(Shape.CLOSE, bounds.width - 1, 0, fg, bg)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        if (event !is GuiPointerButtonEvent ||
            event.button != MouseButton.LEFT ||
            event.action != KeyAction.PRESS
        ) {
            return false
        }
        if (event.localX == bounds.width - 1 && event.localY == 0) {
            dismiss()
            return true
        }
        return false
    }

    private fun severityShape(): Shape =
        when (severity) {
            Severity.INFO -> Shape.DOT
            Severity.SUCCESS -> Shape.CHECK
            Severity.WARNING -> Shape.DOT
            Severity.ERROR -> Shape.CROSS
        }

    private fun severityColor(theme: com.psiwray.organization.terminal_window_gui.style.Theme): Int =
        when (severity) {
            Severity.INFO -> theme.accent
            Severity.SUCCESS -> theme.success
            Severity.WARNING -> theme.warning
            Severity.ERROR -> theme.error
        }
}
