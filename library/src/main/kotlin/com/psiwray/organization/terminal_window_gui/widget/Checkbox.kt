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
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import kotlin.math.max

/**
 * Focusable checkbox with checked, unchecked, and indeterminate states.
 *
 * The box uses semantic brackets and a check or dash. Space or a mouse
 * activation toggles the state. A checked box uses the selected theme style;
 * the bracket and indicator keep the state visible in monochrome.
 *
 * @property label Visible label.
 * @property allowIndeterminate Whether the indeterminate state participates in
 *   toggling.
 * @property onChanged Callback for the new state.
 * @since 1.0.0
 */
public class Checkbox(
    label: String,
    state: CheckState = CheckState.UNCHECKED,
    public var allowIndeterminate: Boolean = false,
    enabled: Boolean = true,
    public var onChanged: (CheckState) -> Unit = {},
) : Widget() {
    private var labelValue: String = label
    private var stateValue: CheckState = state
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

    /** Current state. @since 1.0.0 */
    public var state: CheckState
        get() = stateValue
        set(value) {
            if (stateValue == value) {
                return
            }
            stateValue = value
            invalidatePaint()
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
        val stateStyle =
            when {
                !enabled -> WidgetState.DISABLED
                isPressed -> WidgetState.PRESSED
                isFocused -> WidgetState.FOCUSED
                isHovered -> WidgetState.HOVERED
                stateValue == CheckState.CHECKED || stateValue == CheckState.INDETERMINATE -> WidgetState.SELECTED
                else -> WidgetState.NORMAL
            }
        val style = theme.style(stateStyle)
        val width = bounds.width
        context.fill(Rect(0, 0, width, 1), style.fg, style.bg)
        if (width > 0) {
            context.shape(Shape.BRACKET_LEFT, 0, 0, style.fg, style.bg)
        }
        if (width > 1) {
            when (stateValue) {
                CheckState.CHECKED -> context.shape(Shape.CHECK, 1, 0, style.fg, style.bg)
                CheckState.INDETERMINATE -> context.canvas.set(1, 0, '-'.code, style.fg, style.bg)
                CheckState.UNCHECKED -> Unit
            }
        }
        if (width > 2) {
            context.shape(Shape.BRACKET_RIGHT, 2, 0, style.fg, style.bg)
        }
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
                    toggle()
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
        if (event.key == Key.SPACE) {
            toggle()
            return true
        }
        return false
    }

    private fun toggle() {
        val next =
            when (stateValue) {
                CheckState.UNCHECKED -> CheckState.CHECKED
                CheckState.CHECKED -> CheckState.UNCHECKED
                CheckState.INDETERMINATE -> if (allowIndeterminate) CheckState.CHECKED else CheckState.UNCHECKED
            }
        stateValue = next
        invalidatePaint()
        onChanged(next)
    }

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private companion object {
        private const val PREFIX_WIDTH: Int = 4
    }
}
