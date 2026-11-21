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
 * Focusable push button with an optional icon, default marker, and mnemonic.
 *
 * The preferred width is one cell of padding on each side plus the icon (one
 * cell and one gap) plus the label. Narrow bounds truncate the label with an
 * ellipsis. Focus is visible as an inverted row plus a `>` marker; a default
 * button additionally carries a `*` marker and uses the accent color.
 *
 * Space activates the focused button. Enter activates it only when it is the
 * default button. `Alt+<mnemonic>` activates the first visible enabled button
 * with that mnemonic in the current focus scope. Repeats never activate.
 *
 * @property icon Optional semantic icon shape, rendered in one cell.
 * @property default Whether this is the default button.
 * @property mnemonic Optional activation character.
 * @property onActivate Callback invoked exactly once per activation.
 * @since 1.0.0
 */
public class Button(
    label: String,
    public var icon: Shape? = null,
    public var default: Boolean = false,
    enabled: Boolean = true,
    public var mnemonic: Char? = null,
    public var onActivate: () -> Unit = {},
) : Widget() {
    private var labelValue: String = label
    private val activation: Activation = Activation()
    private val line: LineLabel = LineLabel()

    /** Button label. @since 1.0.0 */
    public var label: String
        get() = labelValue
        set(value) {
            if (labelValue == value) {
                return
            }
            labelValue = value
            invalidateLayout()
        }

    init {
        this.enabled = enabled
        focusable = true
        cursorShape = com.psiwray.organization.terminal_window.CursorShape.HAND
    }

    override fun measure(constraints: Constraints): IntSize {
        val iconWidth = if (icon == null) 0 else ICON_WIDTH
        val width = max(1, PADDING * 2 + iconWidth + TextPainter.codePointCount(labelValue))
        return constraints.constrain(IntSize(width, 1))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val state = ButtonVisuals.state(this)
        val style = ButtonVisuals.style(theme, state)
        context.fill(Rect(0, 0, bounds.width, 1), style.fg, style.bg)
        val width = bounds.width
        val leftMarker = ButtonVisuals.leftMarker(this)
        context.canvas.set(0, 0, leftMarker.code, style.fg, style.bg)
        var contentX = PADDING
        val currentIcon = icon
        if (currentIcon != null && contentX < width) {
            context.shape(currentIcon, contentX, 0, style.fg, style.bg)
            contentX += ICON_WIDTH
        }
        if (contentX >= width) {
            return
        }
        val labelWidth = (width - contentX - PADDING).coerceAtLeast(0)
        line.update(labelValue, labelWidth)
        val highlight = if (enabled) line.mnemonicIndex(mnemonic) else -1
        val highlightStyle = theme.style(WidgetState.SELECTED)
        val labelFg = if (default && state == WidgetState.NORMAL) theme.accent else style.fg
        line.draw(
            context = context,
            x = contentX,
            y = 0,
            fg = labelFg,
            bg = style.bg,
            highlightIndex = highlight,
            highlightFg = highlightStyle.fg,
            highlightBg = highlightStyle.bg,
        )
        if (default && isFocused && width > contentX + line.length() + 1) {
            context.canvas.set(width - 1, 0, '>'.code, style.fg, style.bg)
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

    override fun matchesMnemonic(codePoint: Int): Boolean {
        val value = mnemonic ?: return false
        return value.lowercaseChar().code == codePoint || value.uppercaseChar().code == codePoint
    }

    override fun activateMnemonic(codePoint: Int): Boolean {
        if (!enabled || !matchesMnemonic(codePoint)) {
            return false
        }
        onActivate()
        return true
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
                    onActivate()
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
        if (event.key == Key.SPACE || ((event.key == Key.ENTER || event.key == Key.KP_ENTER) && default)) {
            onActivate()
            invalidatePaint()
            return true
        }
        return false
    }

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private companion object {
        private const val PADDING: Int = 1

        /** One icon cell plus one gap. */
        private const val ICON_WIDTH: Int = 2
    }
}
