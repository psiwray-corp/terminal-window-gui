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
 * One radio option owned by a [RadioGroup].
 *
 * The option paints parentheses around a dot. Selection and focus are visible
 * as the dot and an inverted row even without color. Arrow navigation is
 * handled by the owning group.
 *
 * @property key Stable selection key.
 * @property label Visible label.
 * @since 1.0.0
 */
public class RadioButton(
    public val key: String,
    label: String,
    enabled: Boolean = true,
) : Widget() {
    internal var owner: RadioGroup? = null
    private var labelValue: String = label
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

    /** Whether this option is the group's selected option. @since 1.0.0 */
    public val isSelected: Boolean get() = owner?.selectedKey == key

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
        val selected = isSelected
        val state =
            when {
                !enabled -> WidgetState.DISABLED
                isPressed -> WidgetState.PRESSED
                isFocused -> WidgetState.FOCUSED
                isHovered -> WidgetState.HOVERED
                selected -> WidgetState.SELECTED
                else -> WidgetState.NORMAL
            }
        val style = theme.style(state)
        val width = bounds.width
        context.fill(Rect(0, 0, width, 1), style.fg, style.bg)
        if (width > 0) {
            context.shape(Shape.PAREN_LEFT, 0, 0, style.fg, style.bg)
        }
        if (width > 1 && selected) {
            context.shape(Shape.DOT, 1, 0, style.fg, style.bg)
        }
        if (width > 2) {
            context.shape(Shape.PAREN_RIGHT, 2, 0, style.fg, style.bg)
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
        owner?.onRadioFocused(this, focused)
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
                    owner?.select(key, focus = true)
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
            owner?.select(key, focus = true)
            return true
        }
        return false
    }

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private companion object {
        private const val PREFIX_WIDTH: Int = 4
    }
}
