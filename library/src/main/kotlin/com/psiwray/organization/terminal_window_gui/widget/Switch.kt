package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.internal.Activation
import com.psiwray.organization.terminal_window_gui.internal.LineLabel
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import kotlin.math.max

/**
 * Focusable two-state switch.
 *
 * The row starts with a focus cursor, then `ON` or `OFF`, a gap, and the label.
 * Space or Enter toggles, Left selects off, Right selects on, and mouse
 * activation toggles. The ON/OFF text keeps the state visible without color.
 *
 * @property label Visible label.
 * @property checked Whether the switch is on.
 * @property onChanged Callback invoked when the value changes.
 * @since 1.0.0
 */
public class Switch(
    label: String,
    checked: Boolean = false,
    enabled: Boolean = true,
    public var onChanged: (Boolean) -> Unit = {},
) : Widget() {
    private var labelValue: String = label
    private var checkedValue: Boolean = checked
    private val activation: Activation = Activation()
    private val line: LineLabel = LineLabel()

    /** Visible label. @since 1.0.0 */
    public var label: String
        get() = labelValue
        set(value) {
            if (labelValue == value) {
                return
            }
            labelValue = value
            invalidateLayout()
        }

    /** Whether the switch is on. @since 1.0.0 */
    public var checked: Boolean
        get() = checkedValue
        set(value) {
            setChecked(value, notify = false)
        }

    init {
        this.enabled = enabled
        focusable = true
        cursorShape = com.psiwray.organization.terminal_window.CursorShape.HAND
    }

    override fun measure(constraints: Constraints): IntSize {
        val width = max(1, PREFIX_WIDTH + TextPainter.codePointCount(labelValue))
        return constraints.constrain(IntSize(width, 1))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val state =
            when {
                !enabled -> WidgetState.DISABLED
                isPressed -> WidgetState.PRESSED
                isFocused -> WidgetState.FOCUSED
                isHovered -> WidgetState.HOVERED
                checkedValue -> WidgetState.SELECTED
                else -> WidgetState.NORMAL
            }
        val style = theme.style(state)
        val width = bounds.width
        context.fill(Rect(0, 0, width, 1), style.fg, style.bg)
        context.canvas.set(0, 0, (if (isFocused) '>' else ' ').code, style.fg, style.bg)
        val value = if (checkedValue) "ON" else "OFF"
        val valueFg = if (checkedValue) theme.accent else style.fg
        context.text(1, 0, value, valueFg, style.bg)
        if (width > PREFIX_WIDTH) {
            line.update(labelValue, width - PREFIX_WIDTH)
            line.draw(context, PREFIX_WIDTH, 0, style.fg, style.bg)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        if (!enabled) {
            return false
        }
        return when (event) {
            is GuiPointerButtonEvent -> onPointer(event)
            is GuiKeyEvent -> onKey(event)
            else -> false
        }
    }

    override fun onFocusChanged(focused: Boolean) {
        if (!focused) {
            activation.cancel()
        }
        invalidatePaint()
    }

    override fun onHoverChanged(hovered: Boolean) {
        invalidatePaint()
    }

    override fun onDetach() {
        activation.cancel()
    }

    private fun onPointer(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT) {
            return false
        }
        val inside = inside(event.localX, event.localY)
        return when (event.action) {
            KeyAction.PRESS -> {
                if (!inside) {
                    return false
                }
                activation.press(true)
                invalidatePaint()
                true
            }

            KeyAction.RELEASE -> {
                val activate = activation.release(inside)
                invalidatePaint()
                if (activate) {
                    setChecked(!checkedValue, notify = true)
                }
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (!isFocused || event.action != KeyAction.PRESS || event.repeat) {
            return false
        }
        return when (event.key) {
            Key.SPACE, Key.ENTER, Key.KP_ENTER -> {
                setChecked(!checkedValue, notify = true)
                true
            }

            Key.LEFT -> {
                setChecked(false, notify = true)
                true
            }

            Key.RIGHT -> {
                setChecked(true, notify = true)
                true
            }

            else -> {
                false
            }
        }
    }

    private fun setChecked(
        value: Boolean,
        notify: Boolean,
    ) {
        if (checkedValue == value) {
            if (notify) {
                invalidatePaint()
            }
            return
        }
        checkedValue = value
        invalidatePaint()
        if (notify) {
            onChanged(value)
        }
    }

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private companion object {
        private const val PREFIX_WIDTH: Int = 5
    }
}
