package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.EditorTextLayout
import com.psiwray.organization.terminal_window_gui.internal.TextInput
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import com.psiwray.organization.terminal_window_gui.text.TextSelection
import com.psiwray.organization.terminal_window_gui.text.WordBoundary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Focusable multi-line editor with optional wrapping and line numbers.
 *
 * Character indices stay in the controller; visual rows are a cached mapping
 * produced from the controller text, viewport width, and wrap mode. Wrapped
 * modes use vertical-only scrolling. [TextAreaWrap.NONE] keeps logical lines
 * intact and scrolls both axes.
 *
 * Vertical scrolling is shared through [scroll]; viewport and content metrics
 * are updated during layout. Enter inserts a newline, Tab is left for focus
 * traversal, and read-only fields reject edits while retaining navigation and
 * copy.
 *
 * @property controller Text, selection, and history.
 * @property wrap Wrapping policy.
 * @property lineNumbers Gutter configuration.
 * @property readOnly Whether editing is disabled.
 * @property onValueChange Called after the controller text changes.
 * @since 1.0.0
 */
public class TextArea(
    controller: TextEditingController = TextEditingController(),
    wrap: TextAreaWrap = TextAreaWrap.NONE,
    lineNumbers: LineNumbers = LineNumbers.NONE,
    readOnly: Boolean = false,
    public var onValueChange: (String) -> Unit = {},
) : Widget() {
    private var controllerValue: TextEditingController = controller
    private var wrapValue: TextAreaWrap = wrap
    private var lineNumbersValue: LineNumbers = lineNumbers
    private var readOnlyValue: Boolean = readOnly
    private var layout: EditorTextLayout? = null
    private var layoutWidth: Int = -1
    private var dragging: Boolean = false
    private var dragAnchor: Int = 0
    private val textListener: () -> Unit = { onControllerTextChanged() }
    private val selectionListener: () -> Unit = { onControllerSelectionChanged() }

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
            layout = null
            rebuildLayout()
            invalidateLayout()
        }

    /** Wrapping policy. @since 1.0.0 */
    public var wrap: TextAreaWrap
        get() = wrapValue
        set(value) {
            if (wrapValue == value) {
                return
            }
            wrapValue = value
            layout = null
            rebuildLayout()
            invalidateLayout()
        }

    /** Line-number gutter configuration. @since 1.0.0 */
    public var lineNumbers: LineNumbers
        get() = lineNumbersValue
        set(value) {
            if (lineNumbersValue == value) {
                return
            }
            lineNumbersValue = value
            layout = null
            rebuildLayout()
            invalidateLayout()
        }

    /** Whether editing is disabled. @since 1.0.0 */
    public var readOnly: Boolean
        get() = readOnlyValue
        set(value) {
            if (readOnlyValue == value) {
                return
            }
            readOnlyValue = value
            invalidatePaint()
        }

    /** Vertical and horizontal viewport state. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    /** Alias for [scroll]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = scroll

    /** Width reserved by line numbers in cells. @since 1.0.0 */
    public val lineNumberWidth: Int
        get() {
            if (!lineNumbersValue.enabled) {
                return 0
            }
            val lines = controllerValue.text.count { it == '\n' } + 1
            return max(lineNumbersValue.minDigits, digits(lines)) + 1
        }

    /** Alias for [lineNumberWidth]. @since 1.0.0 */
    public val gutterWidth: Int get() = lineNumberWidth

    /** Child-first constructor for initial text. @since 1.0.0 */
    public constructor(
        text: String,
        wrap: TextAreaWrap = TextAreaWrap.NONE,
        lineNumbers: LineNumbers = LineNumbers.NONE,
        readOnly: Boolean = false,
        onValueChange: (String) -> Unit = {},
    ) : this(TextEditingController(text), wrap, lineNumbers, readOnly, onValueChange)

    /** Initial-text constructor with boolean line numbers. @since 1.0.0 */
    public constructor(
        text: String,
        wrap: TextAreaWrap = TextAreaWrap.NONE,
        lineNumbers: Boolean,
        readOnly: Boolean = false,
        onValueChange: (String) -> Unit = {},
    ) : this(TextEditingController(text), wrap, LineNumbers(lineNumbers), readOnly, onValueChange)

    /** Controller constructor with boolean line numbers. @since 1.0.0 */
    public constructor(
        controller: TextEditingController,
        wrap: TextAreaWrap = TextAreaWrap.NONE,
        lineNumbers: Boolean,
        readOnly: Boolean = false,
        onValueChange: (String) -> Unit = {},
    ) : this(controller, wrap, LineNumbers(lineNumbers), readOnly, onValueChange)

    init {
        focusable = true
        cursorShape = CursorShape.IBEAM
    }

    override fun measure(constraints: Constraints): IntSize {
        val gutter = lineNumberWidth
        val maxTextWidth =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth - gutter).coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
        val measuredLayout = EditorTextLayout(controllerValue.text, maxTextWidth, wrapValue.toTextWrap())
        layout = measuredLayout
        layoutWidth = maxTextWidth
        scroll.contentWidth = measuredLayout.contentWidth
        scroll.contentHeight = measuredLayout.rows.size
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                (measuredLayout.contentWidth + gutter).coerceAtLeast(1)
            }
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                measuredLayout.rows.size.coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        rebuildLayout()
        val current = layout
        if (current != null) {
            scroll.contentWidth = current.contentWidth
            scroll.contentHeight = current.rows.size
        }
        scroll.viewportWidth = textWidth()
        scroll.viewportHeight = bounds.height.coerceAtLeast(0)
        ensureCaretVisible()
    }

    override fun onAttach() {
        controllerValue.addTextListener(textListener)
        controllerValue.addSelectionListener(selectionListener)
        layout = null
        rebuildLayout()
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
        layout = null
        rebuildLayout()
        scroll.viewportWidth = textWidth()
        scroll.viewportHeight = new.height.coerceAtLeast(0)
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
                isFocused -> WidgetState.FOCUSED
                isHovered -> WidgetState.HOVERED
                else -> WidgetState.NORMAL
            }
        val style = theme.style(state)
        val bg = style.bg
        context.fill(Rect(0, 0, bounds.width, bounds.height), style.fg, bg)
        val currentLayout =
            layout ?: EditorTextLayout(controllerValue.text, textWidth(), wrapValue.toTextWrap()).also {
                layout = it
            }
        val gutter = lineNumberWidth
        val textWidth = textWidth()
        val offsetY = scroll.offsetY
        val maxOffsetY = (currentLayout.rows.size - bounds.height).coerceAtLeast(0)
        val actualOffsetY = offsetY.coerceIn(0, maxOffsetY)
        val maxOffsetX = (currentLayout.contentWidth - textWidth).coerceAtLeast(0)
        val offsetX = if (wrapValue == TextAreaWrap.NONE) scroll.offsetX.coerceIn(0, maxOffsetX) else 0

        if (gutter > 0) {
            var row = 0
            while (row < bounds.height) {
                val visual = actualOffsetY + row
                if (visual >= currentLayout.rows.size) {
                    break
                }
                val line = currentLayout.rows[visual]
                if (line.firstOfLine) {
                    val number = (line.logicalLine + 1).toString().padStart(gutter - 1)
                    context.text(0, row, number, theme.textMuted, bg)
                }
                row++
            }
        }

        context.pushClip(Rect(gutter, 0, textWidth, bounds.height))
        context.pushOrigin(gutter, 0)
        try {
            val selection = controllerValue.selection
            var row = 0
            while (row < bounds.height) {
                val visual = actualOffsetY + row
                if (visual >= currentLayout.rows.size) {
                    break
                }
                val line = currentLayout.rows[visual]
                var column = 0
                while (column < line.cells.size) {
                    val x = column - offsetX
                    if (x >= 0 && x < textWidth) {
                        val cell = line.cells[column]
                        val selected = cell.charIndex >= selection.start && cell.charIndex < selection.end
                        val fg = if (selected) theme.selectionForeground else style.fg
                        val cellBg = if (selected) theme.selectionBackground else bg
                        context.canvas.set(x, row, cell.codePoint, fg, cellBg)
                    }
                    column++
                }
                row++
            }
            if (isFocused && !readOnlyValue) {
                val position = currentLayout.positionFor(controllerValue.caret)
                val x = position.column - offsetX
                val y = position.row - actualOffsetY
                if (x in 0 until textWidth && y in 0 until bounds.height) {
                    val line = currentLayout.rows[position.row]
                    val codePoint =
                        if (position.column < line.cells.size) line.cells[position.column].codePoint else ' '.code
                    context.canvas.set(x, y, codePoint, bg, theme.caret)
                }
            }
        } finally {
            context.popOrigin()
            context.popClip()
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiCharEvent -> onChar(event)
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onPointerButton(event)
            is GuiPointerMoveEvent -> onPointerMove(event)
            is GuiScrollEvent -> onScroll(event)
            else -> false
        }

    private fun onChar(event: GuiCharEvent): Boolean {
        if (readOnlyValue) {
            return true
        }
        controllerValue.insert(String(Character.toChars(event.codePoint)))
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
                if (!readOnlyValue) {
                    controllerValue.copy(clipboard())
                } else {
                    controllerValue.copy(clipboard())
                }
                true
            }

            command && event.key == Key.X -> {
                if (!readOnlyValue) {
                    controllerValue.cut(clipboard())
                    afterEdit()
                }
                true
            }

            command && event.key == Key.V -> {
                if (!readOnlyValue) {
                    val text = clipboard().getText()
                    if (text != null) {
                        controllerValue.replaceSelection(text)
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

            event.key == Key.ENTER || event.key == Key.KP_ENTER -> {
                if (!readOnlyValue) {
                    controllerValue.insert("\n")
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

            event.key == Key.UP -> {
                moveVertical(-1, shift)
                afterEdit()
                true
            }

            event.key == Key.DOWN -> {
                moveVertical(1, shift)
                afterEdit()
                true
            }

            event.key == Key.PAGE_UP -> {
                moveVertical(-max(1, scroll.viewportHeight), shift)
                afterEdit()
                true
            }

            event.key == Key.PAGE_DOWN -> {
                moveVertical(max(1, scroll.viewportHeight), shift)
                afterEdit()
                true
            }

            event.key == Key.HOME -> {
                if (command) {
                    controllerValue.textHome(shift)
                } else {
                    val position = currentLayout().positionFor(controllerValue.caret)
                    controllerValue.selection =
                        if (shift) {
                            TextSelection(controllerValue.selection.anchor, currentLayout().rowStart(position.row))
                        } else {
                            val start = currentLayout().rowStart(position.row)
                            TextSelection(start, start)
                        }
                }
                afterEdit()
                true
            }

            event.key == Key.END -> {
                if (command) {
                    controllerValue.textEnd(shift)
                } else {
                    val position = currentLayout().positionFor(controllerValue.caret)
                    val end = currentLayout().rowEnd(position.row)
                    controllerValue.selection =
                        if (shift) {
                            TextSelection(controllerValue.selection.anchor, end)
                        } else {
                            TextSelection(end, end)
                        }
                }
                afterEdit()
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
                val position = positionAt(event.localX, event.localY)
                when (event.clickCount) {
                    2 -> {
                        val range = WordBoundary.wordRange(controllerValue.text, position)
                        controllerValue.selection = TextSelection(range.first, range.last + 1)
                    }

                    3 -> {
                        selectLogicalLine(position)
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
        val position = positionAt(event.localX, event.localY)
        controllerValue.selection = TextSelection(dragAnchor, position)
        invalidatePaint()
        return true
    }

    private fun onScroll(event: GuiScrollEvent): Boolean {
        val dx = if (wrapValue == TextAreaWrap.NONE) event.dx.roundToInt() else 0
        val dy = event.dy.roundToInt()
        if (dx == 0 && dy == 0) {
            return false
        }
        val changed = scroll.scrollBy(dx, dy)
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun positionAt(
        x: Int,
        y: Int,
    ): Int {
        val layout = currentLayout()
        val row = (scroll.offsetY + y).coerceIn(0, layout.rows.lastIndex.coerceAtLeast(0))
        val column =
            if (x < lineNumberWidth) {
                0
            } else {
                (scroll.offsetX + x - lineNumberWidth).coerceAtLeast(0)
            }
        return layout.charIndexAt(row, column)
    }

    private fun selectLogicalLine(position: Int) {
        val layout = currentLayout()
        val row = layout.positionFor(position).row
        val logical = layout.rows[row].logicalLine
        var first = row
        while (first > 0 && layout.rows[first - 1].logicalLine == logical) {
            first--
        }
        var last = row
        while (last + 1 < layout.rows.size && layout.rows[last + 1].logicalLine == logical) {
            last++
        }
        controllerValue.selection = TextSelection(layout.rows[first].startChar, layout.rows[last].endChar)
    }

    private fun moveVertical(
        deltaRows: Int,
        extendSelection: Boolean,
    ) {
        val layout = currentLayout()
        val current = layout.positionFor(controllerValue.caret)
        val targetRow = (current.row + deltaRows).coerceIn(0, layout.rows.lastIndex.coerceAtLeast(0))
        val target = layout.charIndexAt(targetRow, current.column)
        controllerValue.selection =
            if (extendSelection) {
                TextSelection(controllerValue.selection.anchor, target)
            } else {
                TextSelection(target, target)
            }
    }

    private fun afterEdit() {
        rebuildLayout()
        ensureCaretVisible()
        invalidateLayout()
        invalidatePaint()
    }

    private fun onControllerTextChanged() {
        layout = null
        rebuildLayout()
        ensureCaretVisible()
        invalidateLayout()
        onValueChange(controllerValue.text)
    }

    private fun onControllerSelectionChanged() {
        ensureCaretVisible()
        invalidatePaint()
    }

    private fun ensureCaretVisible() {
        val layout = currentLayout()
        val position = layout.positionFor(controllerValue.caret)
        scroll.viewportWidth = textWidth()
        scroll.viewportHeight = bounds.height.coerceAtLeast(0)
        scroll.contentWidth = layout.contentWidth
        scroll.contentHeight = layout.rows.size
        scroll.ensureVisible(position.column, position.row, 1, 1)
    }

    private fun rebuildLayout() {
        val width = textWidth()
        if (layout != null && layoutWidth == width) {
            return
        }
        layoutWidth = width
        layout = EditorTextLayout(controllerValue.text, width, wrapValue.toTextWrap())
        scroll.contentWidth = layout!!.contentWidth
        scroll.contentHeight = layout!!.rows.size
    }

    private fun currentLayout(): EditorTextLayout {
        val existing = layout
        if (existing != null) {
            return existing
        }
        val width = textWidth()
        val created = EditorTextLayout(controllerValue.text, width, wrapValue.toTextWrap())
        layout = created
        layoutWidth = width
        return created
    }

    private fun textWidth(): Int = (bounds.width - lineNumberWidth).coerceAtLeast(1)

    private fun digits(value: Int): Int {
        var digits = 1
        var remaining = value
        while (remaining >= 10) {
            remaining /= 10
            digits++
        }
        return digits
    }

    private fun clipboard(): com.psiwray.organization.terminal_window_gui.input.Clipboard =
        guiContext?.clipboard ?: EMPTY_CLIPBOARD

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < bounds.width && y < bounds.height

    private companion object {
        private val EMPTY_CLIPBOARD: com.psiwray.organization.terminal_window_gui.input.Clipboard =
            object : com.psiwray.organization.terminal_window_gui.input.Clipboard {
                override fun getText(): String? = null

                override fun setText(text: String) {}
            }
    }
}
