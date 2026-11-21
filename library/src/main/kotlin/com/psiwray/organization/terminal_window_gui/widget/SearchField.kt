package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.internal.Activation
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Overflow
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.math.max

/**
 * Text field with a one-cell clear button.
 *
 * The field occupies all but the last cell; the clear button is the last cell
 * and participates in Tab order. Activating clear empties the field and moves
 * focus back to the field. Submit calls [onSearch].
 *
 * @property field Embedded text field.
 * @property onSearch Called when the field submits.
 * @property onValueChange Called after the controller text changes.
 * @since 1.0.0
 */
public class SearchField(
    controller: TextEditingController = TextEditingController(),
    placeholder: String = "",
    public var onSearch: (String) -> Unit = {},
    public var onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
) : ContainerWidget(Overflow.CLIP) {
    /** Embedded text field. @since 1.0.0 */
    public val field: TextField =
        TextField(
            controller = controller,
            placeholder = placeholder,
            onSubmit = { onSearch(it) },
            onValueChange = { onValueChange(it) },
        )

    private val clearButton: ClearButton = ClearButton { clearAndFocus() }

    /** Text editing controller. @since 1.0.0 */
    public val controller: TextEditingController get() = this.field.controller

    /** Current text. @since 1.0.0 */
    public var text: String
        get() = this.field.controller.text
        set(value) {
            this.field.controller.setText(value)
        }

    /** Placeholder shown while empty and unfocused. @since 1.0.0 */
    public var placeholder: String
        get() = this.field.placeholder
        set(value) {
            this.field.placeholder = value
            invalidateLayout()
        }

    /** Visible mask character, or `null`. @since 1.0.0 */
    public var mask: Char?
        get() = this.field.mask
        set(value) {
            this.field.mask = value
            invalidatePaint()
        }

    /** Whether editing is disabled. @since 1.0.0 */
    public var readOnly: Boolean
        get() = this.field.readOnly
        set(value) {
            this.field.readOnly = value
        }

    /** Whether the error style is used. @since 1.0.0 */
    public var error: Boolean
        get() = this.field.error
        set(value) {
            this.field.error = value
            invalidatePaint()
        }

    init {
        this.enabled = enabled
        addChild(field)
        addChild(clearButton)
    }

    /** Initial-text constructor. @since 1.0.0 */
    public constructor(
        text: String,
        placeholder: String = "",
        onSearch: (String) -> Unit = {},
        onValueChange: (String) -> Unit = {},
        enabled: Boolean = true,
    ) : this(TextEditingController(text), placeholder, onSearch, onValueChange, enabled)

    /** Clears the text and focuses the field. @since 1.0.0 */
    public fun clear() {
        clearAndFocus()
    }

    override fun paint(context: PaintContext) {}

    override fun measure(constraints: Constraints): IntSize {
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                max(1, field.measured(Constraints(0, Int.MAX_VALUE, 1, 1)).width + 1)
            }
        return constraints.constrain(IntSize(max(1, width), 1))
    }

    override fun arrangeChildren() {
        val width = bounds.width
        val fieldWidth = if (width > 1) width - 1 else 1
        val fieldSize = field.measured(Constraints(fieldWidth, fieldWidth, 1, 1))
        field.arrange(Rect(0, 0, fieldSize.width, 1))
        clearButton.visible = width > 1
        if (clearButton.visible) {
            val clearSize = clearButton.measured(Constraints(1, 1, 1, 1))
            clearButton.arrange(Rect(width - 1, 0, clearSize.width, 1))
        }
    }

    private fun addChild(child: Widget) {
        child.setParent(this)
        childList.add(child)
    }

    private fun clearAndFocus() {
        field.controller.setText("")
        guiContext?.focus?.focus(field)
        invalidateLayout()
    }

    private class ClearButton(
        private val action: () -> Unit,
    ) : Widget() {
        private val activation: Activation = Activation()

        init {
            focusable = true
            cursorShape = CursorShape.HAND
        }

        override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(1, 1))

        override fun paint(context: PaintContext) {
            if (bounds.isEmpty) {
                return
            }
            val theme = context.theme
            val fg = if (isFocused) theme.borderFocused else theme.border
            val bg = if (isFocused) theme.surfaceAlt else theme.surface
            context.fill(Rect(0, 0, bounds.width, 1), fg, bg)
            context.shape(Shape.CROSS, 0, 0, fg, bg)
        }

        override fun onEvent(event: GuiEvent): Boolean {
            if (event is GuiKeyEvent) {
                if (!isFocused || event.action == KeyAction.RELEASE) {
                    return false
                }
                if (event.key == Key.SPACE || event.key == Key.ENTER || event.key == Key.KP_ENTER) {
                    action()
                    return true
                }
                return false
            }
            if (event !is GuiPointerButtonEvent || event.button != MouseButton.LEFT) {
                return false
            }
            val inside = event.localX in 0 until bounds.width && event.localY in 0 until bounds.height
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
                        action()
                    }
                    true
                }

                else -> {
                    false
                }
            }
        }

        override fun onFocusChanged(focused: Boolean) {
            if (!focused) {
                activation.cancel()
            }
            invalidatePaint()
        }

        override fun onDetach() {
            activation.cancel()
        }
    }
}
