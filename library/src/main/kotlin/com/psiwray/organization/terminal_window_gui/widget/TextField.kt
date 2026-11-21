package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.internal.EditorTextLayout
import com.psiwray.organization.terminal_window_gui.internal.TextInput
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextWrap
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import com.psiwray.organization.terminal_window_gui.text.TextSelection
import com.psiwray.organization.terminal_window_gui.text.WordBoundary
import kotlin.math.max

/**
 * Focusable single-row text field.
 *
 * The field edits a [TextEditingController] through printable
 * [com.psiwray.organization.terminal_window_gui.input.GuiCharEvent] values and
 * editing key chords. It never inserts a newline. A mask character replaces
 * every visible code point while leaving the underlying text intact.
 *
 * Preferred height is one cell. Horizontal scrolling follows the caret; the
 * caret is an inverted cell and is hidden while unfocused. Selection uses the
 * active theme selection colors. Read-only fields accept focus, selection, and
 * navigation but reject edits; masked fields disable copy and cut.
 *
 * @property controller Text, selection, and history.
 * @property placeholder Text shown while empty and unfocused.
 * @property mask Optional replacement character for visible text.
 * @property readOnly Whether editing is disabled.
 * @property error Whether the error theme style is used.
 * @property onSubmit Called when Enter is pressed.
 * @property onValueChange Called after the controller text changes.
 * @since 1.0.0
 */
public open class TextField(
    controller: TextEditingController = TextEditingController(),
    placeholder: String = "",
    mask: Char? = null,
    readOnly: Boolean = false,
    error: Boolean = false,
    public var onSubmit: (String) -> Unit = {},
    public var onValueChange: (String) -> Unit = {},
) : Widget() {
    private var controllerValue: TextEditingController = controller
    private var placeholderValue: String = placeholder
    private var maskValue: Char? = mask
    private var readOnlyValue: Boolean = readOnly
    private var errorValue: Boolean = error
    private var layout: EditorTextLayout = layoutFor(controllerValue)
    private var scrollOffsetValue: Int = 0
    private var dragging: Boolean = false
    private var dragAnchor: Int = 0
    private val textListener: () -> Unit = { onControllerTextChanged() }
    private val selectionListener: () -> Unit = { onControllerSelectionChanged() }

    /** Child-first constructor for initial text. @since 1.0.0 */
    public constructor(
        text: String,
        placeholder: String = "",
        mask: Char? = null,
        readOnly: Boolean = false,
        error: Boolean = false,
        onSubmit: (String) -> Unit = {},
        onValueChange: (String) -> Unit = {},
    ) : this(TextEditingController(text), placeholder, mask, readOnly, error, onSubmit, onValueChange)

    /** Text editing controller. @since 1.0.0 */
    public var controller: TextEditingController
        get() = controllerValue
        set(value) {
            if (controllerValue === value) {
                return
            }
            if (isAttached) {
                controllerValue.removeTextListener(textListener)
                controllerValue.removeSelectionListener(selectionListener)
            }
            controllerValue = value
            if (isAttached) {
                controllerValue.addTextListener(textListener)
                controllerValue.addSelectionListener(selectionListener)
            }
            layout = layoutFor(controllerValue)
            scrollOffsetValue = 0
            ensureCaretVisible()
            invalidateLayout()
        }

    /** Placeholder shown while empty and unfocused. @since 1.0.0 */
    public var placeholder: String
        get() = placeholderValue
        set(value) {
            if (placeholderValue == value) {
                return
            }
            placeholderValue = value
            invalidatePaint()
        }

    /** Visible mask character, or `null`. @since 1.0.0 */
    public var mask: Char?
        get() = maskValue
        set(value) {
            if (maskValue == value) {
                return
            }
            maskValue = value
            invalidatePaint()
        }

    /** Whether editing is disabled. @since 1.0.0 */
    public var readOnly: Boolean
        get() = readOnlyValue
        set(value) {
            readOnlyValue = value
            invalidatePaint()
        }

    /** Whether the error style is used. @since 1.0.0 */
    public var error: Boolean
        get() = errorValue
        set(value) {
            errorValue = value
            invalidatePaint()
        }

    /** Current horizontal cell offset. @since 1.0.0 */
    public val scrollOffset: Int get() = scrollOffsetValue

    init {
        focusable = true
        cursorShape = com.psiwray.organization.terminal_window.CursorShape.IBEAM
    }

    override fun measure(constraints: Constraints): IntSize {
        layout = layoutFor(controllerValue)
        val width = max(1, layout.contentWidth + 1)
        return constraints.constrain(IntSize(width, 1))
    }

    override fun onAttach() {
        controllerValue.addTextListener(textListener)
        controllerValue.addSelectionListener(selectionListener)
        layout = layoutFor(controllerValue)
        ensureCaretVisible()
    }

    override fun onDetach() {
        controllerValue.removeTextListener(textListener)
        controllerValue.removeSelectionListener(selectionListener)
        dragging = false
    }

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        ensureCaretVisible()
    }

    override fun onFocusChanged(focused: Boolean) {
        ensureCaretVisible()
        invalidatePaint()
    }

    override fun onHoverChanged(hovered: Boolean) {
        invalidatePaint()
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val state =
            when {
                !enabled -> WidgetState.DISABLED
                errorValue -> WidgetState.ERROR
                isFocused -> WidgetState.FOCUSED
                isHovered -> WidgetState.HOVERED
                else -> WidgetState.NORMAL
            }
        val style = theme.style(state)
        val width = bounds.width
        val bg = style.bg
        context.fill(Rect(0, 0, width, 1), style.fg, bg)

        val fieldText = controllerValue.text
        if (fieldText.isEmpty() && !isFocused && placeholderValue.isNotEmpty()) {
            TextPainter.draw(
                context = context,
                line = placeholderValue,
                x = 0,
                y = 0,
                width = width,
                alignment = com.psiwray.organization.terminal_window_gui.paint.TextAlignment.LEFT,
                fg = theme.textMuted,
                bg = bg,
                ellipsis = true,
            )
            return
        }

        ensureCaretVisible()
        val cells = layout.rows.firstOrNull()?.cells ?: emptyList()
        val selection = controllerValue.selection
        val selectedFg = theme.selectionForeground
        val selectedBg = theme.selectionBackground
        var column = 0
        while (column < width) {
            val index = scrollOffsetValue + column
            if (index >= cells.size) {
                break
            }
            val cell = cells[index]
            val isSelected = cell.charIndex >= selection.start && cell.charIndex < selection.end
            val codePoint = maskValue?.code ?: cell.codePoint
            val fg = if (isSelected) selectedFg else style.fg
            val cellBg = if (isSelected) selectedBg else bg
            context.canvas.set(column, 0, codePoint, fg, cellBg)
            column++
        }

        if (isFocused) {
            val caretColumn = layout.positionFor(controllerValue.caret).column
            val x = caretColumn - scrollOffsetValue
            if (x in 0 until width) {
                val codePoint =
                    if (caretColumn <
                        cells.size
                    ) {
                        maskValue?.code ?: cells[caretColumn].codePoint
                    } else {
                        ' '.code
                    }
                context.canvas.set(x, 0, codePoint, bg, theme.caret)
            }
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiCharEvent -> onChar(event)
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onPointerButton(event)
            is GuiPointerMoveEvent -> onPointerMove(event)
            else -> false
        }

    private fun onChar(event: GuiCharEvent): Boolean {
        if (readOnlyValue) {
            return true
        }
        val codePoint = event.codePoint
        if (codePoint == '\n'.code || codePoint == '\r'.code) {
            controllerValue.insert(" ")
        } else {
            controllerValue.insert(String(Character.toChars(codePoint)))
        }
        afterEdit()
        return true
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        val command = TextInput.command(event.modifiers)
        val shift = TextInput.shift(event.modifiers)
        return when {
            event.key == Key.TAB -> {
                false
            }

            command && event.key == Key.A -> {
                controllerValue.selectAll()
                afterEdit()
                true
            }

            command && event.key == Key.C -> {
                if (maskValue == null) {
                    controllerValue.copy(clipboard())
                }
                true
            }

            command && event.key == Key.X -> {
                if (!readOnlyValue && maskValue == null) {
                    controllerValue.cut(clipboard())
                    afterEdit()
                }
                true
            }

            command && event.key == Key.V -> {
                if (!readOnlyValue) {
                    val text = clipboard().getText()
                    if (text != null) {
                        controllerValue.replaceSelection(TextInput.singleLine(text))
                        afterEdit()
                    }
                }
                true
            }

            command && event.key == Key.Z -> {
                if (!readOnlyValue) {
                    controllerValue.undo()
                    afterEdit()
                }
                true
            }

            command && event.key == Key.Y -> {
                if (!readOnlyValue) {
                    controllerValue.redo()
                    afterEdit()
                }
                true
            }

            event.key == Key.BACKSPACE -> {
                if (!readOnlyValue) {
                    if (command) controllerValue.deleteWordBefore() else controllerValue.deleteBefore()
                    afterEdit()
                }
                true
            }

            event.key == Key.DELETE -> {
                if (!readOnlyValue) {
                    if (command) controllerValue.deleteWordAfter() else controllerValue.deleteAfter()
                    afterEdit()
                }
                true
            }

            event.key == Key.LEFT -> {
                if (command) controllerValue.moveWord(-1, shift) else controllerValue.moveCaret(-1, shift)
                afterEdit()
                true
            }

            event.key == Key.RIGHT -> {
                if (command) controllerValue.moveWord(1, shift) else controllerValue.moveCaret(1, shift)
                afterEdit()
                true
            }

            event.key == Key.HOME -> {
                if (command) controllerValue.textHome(shift) else controllerValue.home(shift)
                afterEdit()
                true
            }

            event.key == Key.END -> {
                if (command) controllerValue.textEnd(shift) else controllerValue.end(shift)
                afterEdit()
                true
            }

            event.key == Key.ENTER || event.key == Key.KP_ENTER -> {
                onSubmit(controllerValue.text)
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onPointerButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT) {
            return false
        }
        return when (event.action) {
            KeyAction.PRESS -> {
                if (!inside(event.localX, event.localY)) {
                    return false
                }
                val position = positionAt(event.localX)
                when (event.clickCount) {
                    2 -> {
                        val range = WordBoundary.wordRange(controllerValue.text, position)
                        controllerValue.selection = TextSelection(range.first, range.last + 1)
                    }

                    3 -> {
                        controllerValue.selectAll()
                    }

                    else -> {
                        controllerValue.selection = TextSelection(position, position)
                    }
                }
                dragAnchor = controllerValue.selection.anchor
                dragging = true
                invalidatePaint()
                true
            }

            KeyAction.RELEASE -> {
                dragging = false
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onPointerMove(event: GuiPointerMoveEvent): Boolean {
        if (!dragging) {
            return false
        }
        val position = positionAt(event.localX)
        controllerValue.selection = TextSelection(dragAnchor, position)
        invalidatePaint()
        return true
    }

    private fun positionAt(x: Int): Int {
        val column = (scrollOffsetValue + x).coerceAtLeast(0)
        return layout.charIndexAt(0, column)
    }

    private fun afterEdit() {
        ensureCaretVisible()
        invalidateLayout()
        invalidatePaint()
    }

    private fun onControllerTextChanged() {
        layout = layoutFor(controllerValue)
        ensureCaretVisible()
        invalidateLayout()
        onValueChange(controllerValue.text)
    }

    private fun onControllerSelectionChanged() {
        ensureCaretVisible()
        invalidatePaint()
    }

    private fun ensureCaretVisible() {
        val width = bounds.width
        if (width <= 0) {
            return
        }
        val caretColumn = layout.positionFor(controllerValue.caret).column
        if (caretColumn < scrollOffsetValue) {
            scrollOffsetValue = caretColumn
        } else if (caretColumn >= scrollOffsetValue + width) {
            scrollOffsetValue = caretColumn - width + 1
        }
        val maxOffset = (layout.contentWidth + 1 - width).coerceAtLeast(0)
        scrollOffsetValue = scrollOffsetValue.coerceIn(0, maxOffset)
    }

    private fun clipboard(): com.psiwray.organization.terminal_window_gui.input.Clipboard =
        guiContext?.clipboard ?: EMPTY_CLIPBOARD

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private fun layoutFor(value: TextEditingController): EditorTextLayout =
        EditorTextLayout(value.text, Int.MAX_VALUE, TextWrap.NONE)

    private companion object {
        private val EMPTY_CLIPBOARD: com.psiwray.organization.terminal_window_gui.input.Clipboard =
            object : com.psiwray.organization.terminal_window_gui.input.Clipboard {
                override fun getText(): String? = null

                override fun setText(text: String) {}
            }
    }
}
