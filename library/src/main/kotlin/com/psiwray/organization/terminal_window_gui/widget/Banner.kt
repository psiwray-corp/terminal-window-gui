package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
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
 * One-row severity banner with an optional action and close control.
 *
 * @property severity Banner severity.
 * @property message Message text.
 * @property action Optional action label.
 * @property onAction Optional action callback.
 * @property onClose Optional close callback.
 * @property closable Whether the close cell is shown.
 * @property background Optional background override.
 * @since 1.0.0
 */
public class Banner(
    message: String = "",
    severity: Severity = Severity.INFO,
    public var action: String? = null,
    public var onAction: (() -> Unit)? = null,
    public var onClose: (() -> Unit)? = null,
    public var closable: Boolean = true,
    public var background: Int? = null,
) : Widget() {
    private var severityValue: Severity = severity
    private var messageValue: String = message

    /** Banner severity. @since 1.0.0 */
    public var severity: Severity
        get() = severityValue
        set(value) {
            severityValue = value
            invalidatePaint()
        }

    /** Banner message. @since 1.0.0 */
    public var message: String
        get() = messageValue
        set(value) {
            messageValue = value
            invalidateLayout()
        }

    init {
        focusable = true
        tabStop = (closable && onClose != null) || (action != null && onAction != null)
    }

    override fun measure(constraints: Constraints): IntSize {
        val actionWidth = action?.let { TextPainter.codePointCount(it) + 2 } ?: 0
        val closeWidth = if (closable) 1 else 0
        val width =
            max(
                3,
                3 + TextPainter.codePointCount(messageValue) + actionWidth + closeWidth,
            )
        return constraints.constrain(IntSize(width, 1))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val bg = background ?: severityBackground(theme)
        val fg = theme.text
        context.fill(Rect(0, 0, bounds.width, bounds.height), fg, bg)
        context.shape(severityShape(), 0, 0, fg, bg)
        if (isFocused && bounds.width > 1) {
            context.text(1, 0, ">", fg, bg)
        }
        val closeWidth = if (closable) 1 else 0
        val actionWidth = action?.let { TextPainter.codePointCount(it) } ?: 0
        val actionStart = (bounds.width - closeWidth - actionWidth).coerceAtLeast(3)
        val messageWidth = (actionStart - 3).coerceAtLeast(0)
        val messageOverflow = TextPainter.codePointCount(messageValue) > messageWidth
        TextPainter.draw(
            context = context,
            line = messageValue,
            x = 2,
            y = 0,
            width = messageWidth,
            alignment = TextAlignment.LEFT,
            fg = fg,
            bg = bg,
            ellipsis = messageOverflow,
        )
        val actionText = action
        if (actionText != null && actionWidth > 0) {
            context.text(actionStart, 0, actionText, fg, bg)
        }
        if (closable && bounds.width > 0) {
            context.shape(Shape.CLOSE, bounds.width - 1, 0, fg, bg)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onPointer(event)
            else -> false
        }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        return when (event.key) {
            Key.ENTER -> {
                onAction?.invoke()
                onAction != null
            }

            Key.ESCAPE -> {
                if (closable) {
                    onClose?.invoke()
                }
                closable
            }

            else -> {
                false
            }
        }
    }

    private fun onPointer(event: GuiPointerButtonEvent): Boolean {
        if (event.action != KeyAction.PRESS) {
            return false
        }
        val x = event.localX
        if (closable && x == bounds.width - 1) {
            onClose?.invoke()
            return true
        }
        val actionText = action
        if (actionText != null) {
            val start =
                (
                    bounds.width - (if (closable) 1 else 0) -
                        TextPainter.codePointCount(
                            actionText,
                        )
                ).coerceAtLeast(3)
            if (x >= start && x < start + TextPainter.codePointCount(actionText)) {
                onAction?.invoke()
                return true
            }
        }
        return false
    }

    private fun severityBackground(theme: com.psiwray.organization.terminal_window_gui.style.Theme): Int =
        when (severityValue) {
            Severity.INFO -> theme.surfaceAlt
            Severity.SUCCESS -> theme.success
            Severity.WARNING -> theme.warning
            Severity.ERROR -> theme.error
        }

    private fun severityShape(): Shape =
        when (severityValue) {
            Severity.INFO -> Shape.DOT
            Severity.SUCCESS -> Shape.CHECK
            Severity.WARNING -> Shape.DOT
            Severity.ERROR -> Shape.CROSS
        }
}
