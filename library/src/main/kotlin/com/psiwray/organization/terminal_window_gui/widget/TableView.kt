package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.ColumnLayout
import com.psiwray.organization.terminal_window_gui.internal.ColumnMetrics
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Virtualized table with a frozen header, stable-key selection, and
 * application-owned sorting.
 *
 * Columns are laid out from their [ColumnWidth] requests. Cell text is painted
 * into one row and truncated with an ellipsis. Header clicks call
 * [onHeaderClick]; the table never reorders [items]. [sortColumn] and
 * [sortDirection] only control the painted indicator.
 *
 * @property onHeaderClick Called with the clicked column index.
 * @property onActivate Called for Enter and double-click activation.
 * @since 1.0.0
 */
public class TableView<T>(
    items: List<T> = emptyList(),
    columns: List<TableColumn<T>> = emptyList(),
    keyOf: ((T) -> Any)? = null,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    selection: SelectionModel = SelectionModel(selectionMode),
    public var onHeaderClick: (Int) -> Unit = {},
    sortColumn: Int? = null,
    sortDirection: SortDirection? = null,
    public var onActivate: (T) -> Unit = {},
    emptyText: String = "No rows",
) : ContainerWidget() {
    private var itemsValue: List<T> = items
    private var columnsValue: List<TableColumn<T>> = columns
    private var keyOfValue: ((T) -> Any)? = keyOf
    private var selectionValue: SelectionModel = selection
    private var orderedKeys: List<Any> = emptyList()
    private var emptyTextValue: String = emptyText
    private var sortColumnValue: Int? = sortColumn
    private var sortDirectionValue: SortDirection? = sortDirection
    private var metricsValue: ColumnMetrics = ColumnLayout.measure(columnsValue, 1)
    private var bodyWidthValue: Int = 0
    private var bodyHeightValue: Int = 0
    private val selectionListener: () -> Unit = { onSelectionChanged() }

    /** Two-axis scroll state. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    private val verticalBarValue = ScrollBar(scroll, Orientation.VERTICAL, ScrollBarVisibility.AUTO)
    private val horizontalBarValue = ScrollBar(scroll, Orientation.HORIZONTAL, ScrollBarVisibility.AUTO)

    /** Trailing activation convenience constructor. @since 1.0.0 */
    public constructor(
        items: List<T>,
        columns: List<TableColumn<T>>,
        onActivate: (T) -> Unit,
    ) : this(
        items = items,
        columns = columns,
        keyOf = null,
        selectionMode = SelectionMode.SINGLE,
        selection = SelectionModel(SelectionMode.SINGLE),
        onHeaderClick = {},
        sortColumn = null,
        sortDirection = null,
        onActivate = onActivate,
        emptyText = "No rows",
    )

    init {
        focusable = true
        verticalBarValue.focusable = false
        horizontalBarValue.focusable = false
        adopt(verticalBarValue)
        adopt(horizontalBarValue)
        selectionValue.addListener(selectionListener)
        rebuildKeys()
    }

    /** Rows in display order. Assigning a new list invalidates selection. @since 1.0.0 */
    public var items: List<T>
        get() = itemsValue
        set(value) {
            if (itemsValue === value) {
                return
            }
            itemsValue = value
            rebuildKeys()
            selectionValue.retain(orderedKeys)
            invalidateLayout()
            invalidatePaint()
        }

    /** Column definitions. @since 1.0.0 */
    public var columns: List<TableColumn<T>>
        get() = columnsValue
        set(value) {
            if (columnsValue === value) {
                return
            }
            columnsValue = value
            invalidateLayout()
            invalidatePaint()
        }

    /** Stable key accessor, or `null` to use the row index. @since 1.0.0 */
    public var keyOf: ((T) -> Any)?
        get() = keyOfValue
        set(value) {
            if (keyOfValue === value) {
                return
            }
            keyOfValue = value
            rebuildKeys()
            selectionValue.retain(orderedKeys)
            invalidateLayout()
            invalidatePaint()
        }

    /** Selection state. @since 1.0.0 */
    public var selection: SelectionModel
        get() = selectionValue
        set(value) {
            if (selectionValue === value) {
                return
            }
            selectionValue.removeListener(selectionListener)
            selectionValue = value
            selectionValue.addListener(selectionListener)
            selectionValue.keys = orderedKeys
            selectionValue.retain(orderedKeys)
            onSelectionChanged()
            invalidateLayout()
        }

    /** Current selection policy alias. @since 1.0.0 */
    public val selectionMode: SelectionMode get() = selectionValue.mode

    /** Rebuilds keys and layout after in-place item, column, or accessor changes. @since 1.0.0 */
    public fun refresh() {
        rebuildKeys()
        selectionsRestored()
        invalidateLayout()
        invalidatePaint()
    }

    /** Resolved column widths from the last layout. @since 1.0.0 */
    public val columnWidths: List<Int> get() = metricsValue.widths.toList()

    /** Text shown when there are no rows. @since 1.0.0 */
    public var emptyText: String
        get() = emptyTextValue
        set(value) {
            emptyTextValue = value
            invalidatePaint()
        }

    /** Active sort column index for display, or `null`. @since 1.0.0 */
    public var sortColumn: Int?
        get() = sortColumnValue
        set(value) {
            if (sortColumnValue == value) {
                return
            }
            sortColumnValue = value
            invalidatePaint()
        }

    /** Active sort direction for display, or `null`. @since 1.0.0 */
    public var sortDirection: SortDirection?
        get() = sortDirectionValue
        set(value) {
            if (sortDirectionValue == value) {
                return
            }
            sortDirectionValue = value
            invalidatePaint()
        }

    /** Optional vertical scrollbar. @since 1.0.0 */
    public val scrollBar: ScrollBar get() = verticalBarValue

    /** Optional horizontal scrollbar. @since 1.0.0 */
    public val horizontalScrollBar: ScrollBar get() = horizontalBarValue

    /** Alias for [scroll]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = scroll

    override fun onFocusChanged(focused: Boolean) {
        invalidatePaint()
    }

    override fun measure(constraints: Constraints): IntSize {
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                metricsValue.contentWidth.coerceAtLeast(1)
            }
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                (itemsValue.size + 1).coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val height = bounds.height
        var verticalShown = false
        var horizontalShown = false
        var bodyWidth = bounds.width
        var bodyHeight = (height - 1).coerceAtLeast(0)
        repeat(3) {
            bodyWidth = (bounds.width - if (verticalShown) 1 else 0).coerceAtLeast(1)
            metricsValue = ColumnLayout.measure(columnsValue, bodyWidth)
            horizontalShown =
                itemsValue.isNotEmpty() && metricsValue.contentWidth > bodyWidth && height > 1
            bodyHeight = (height - 1 - if (horizontalShown) 1 else 0).coerceAtLeast(0)
            verticalShown = itemsValue.size > bodyHeight
        }
        if (verticalShown) {
            bodyWidth = (bounds.width - 1).coerceAtLeast(1)
            metricsValue = ColumnLayout.measure(columnsValue, bodyWidth)
            horizontalShown =
                itemsValue.isNotEmpty() && metricsValue.contentWidth > bodyWidth && height > 1
        }
        bodyWidthValue = bodyWidth
        bodyHeightValue = bodyHeight
        scroll.viewportWidth = bodyWidth
        scroll.viewportHeight = bodyHeight
        scroll.contentWidth = metricsValue.contentWidth
        scroll.contentHeight = itemsValue.size
        verticalBarValue.arrange(
            if (verticalShown) Rect(bodyWidth, 1, 1, bodyHeight) else Rect.EMPTY,
        )
        horizontalBarValue.arrange(
            if (horizontalShown) Rect(0, height - 1, bodyWidth, 1) else Rect.EMPTY,
        )
        ensureCursorVisible()
    }

    override fun paint(context: PaintContext) {
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surface)
        if (bounds.height == 0) {
            return
        }
        paintHeader(context)
        if (itemsValue.isEmpty()) {
            if (bodyHeightValue > 0 && emptyTextValue.isNotEmpty()) {
                TextPainter.draw(
                    context = context,
                    line = emptyTextValue,
                    x = 0,
                    y = 1,
                    width = bodyWidthValue,
                    alignment = TextAlignment.LEFT,
                    fg = theme.textMuted,
                    bg = theme.surface,
                    ellipsis = TextPainter.codePointCount(emptyTextValue) > bodyWidthValue,
                )
            }
            return
        }
        paintBody(context)
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onButton(event)
            is GuiScrollEvent -> onScrollEvent(event)
            else -> false
        }

    private fun paintHeader(context: PaintContext) {
        val theme = context.theme
        context.fill(Rect(0, 0, bodyWidthValue, 1), theme.text, theme.surfaceAlt)
        context.pushClip(Rect(0, 0, bodyWidthValue, 1))
        try {
            val widths = metricsValue.widths
            val offsets = metricsValue.offsets
            var index = 0
            while (index < columnsValue.size) {
                val column = columnsValue[index]
                val x = offsets[index] - scroll.offsetX
                val width = widths[index]
                if (x < bodyWidthValue && x + width > 0) {
                    val active = sortColumnValue == index
                    val fg = if (active) theme.accent else theme.text
                    context.fill(Rect(x, 0, width, 1), fg, theme.surfaceAlt)
                    val indicatorWidth = if (active && sortDirectionValue != null) 1 else 0
                    val titleWidth = (width - indicatorWidth).coerceAtLeast(0)
                    val title = column.title.replace('\n', ' ')
                    TextPainter.draw(
                        context = context,
                        line = title,
                        x = x,
                        y = 0,
                        width = titleWidth,
                        alignment = column.alignment,
                        fg = fg,
                        bg = theme.surfaceAlt,
                        ellipsis = TextPainter.codePointCount(title) > titleWidth,
                    )
                    if (indicatorWidth == 1) {
                        val shape =
                            if (sortDirectionValue == SortDirection.ASCENDING) {
                                Shape.SORT_ASCENDING
                            } else {
                                Shape.SORT_DESCENDING
                            }
                        context.shape(shape, x + width - 1, 0, theme.accent, theme.surfaceAlt)
                    }
                }
                index++
            }
        } finally {
            context.popClip()
        }
    }

    private fun paintBody(context: PaintContext) {
        val theme = context.theme
        context.pushClip(Rect(0, 1, bodyWidthValue, bodyHeightValue))
        try {
            val start = scroll.offsetY
            var screenRow = 0
            while (screenRow < bodyHeightValue) {
                val row = start + screenRow
                if (row >= itemsValue.size) {
                    break
                }
                val item = itemsValue[row]
                val key = orderedKeys[row]
                val selected = selectionValue.isSelected(key)
                val rowFg = if (selected) theme.selectionForeground else theme.text
                val rowBg = if (selected) theme.selectionBackground else theme.surface
                if (selected) {
                    context.fill(Rect(0, 1 + screenRow, bodyWidthValue, 1), rowFg, rowBg)
                }
                val widths = metricsValue.widths
                val offsets = metricsValue.offsets
                var columnIndex = 0
                while (columnIndex < columnsValue.size) {
                    val column = columnsValue[columnIndex]
                    val cell = column.cell(item)
                    val x = offsets[columnIndex] - scroll.offsetX
                    val width = widths[columnIndex]
                    if (x < bodyWidthValue && x + width > 0) {
                        val bg = cell.bg ?: rowBg
                        val fg = cell.fg ?: rowFg
                        context.fill(Rect(x, 1 + screenRow, width, 1), fg, bg)
                        var textX = x
                        var textWidth = width
                        if (cell.icon != null && width > 0) {
                            context.shape(cell.icon, x, 1 + screenRow, fg, bg)
                            textX++
                            textWidth--
                        }
                        val cellText = cell.text.replace('\n', ' ')
                        TextPainter.draw(
                            context = context,
                            line = cellText,
                            x = textX,
                            y = 1 + screenRow,
                            width = textWidth,
                            alignment = column.alignment,
                            fg = fg,
                            bg = bg,
                            ellipsis = TextPainter.codePointCount(cellText) > textWidth,
                        )
                    }
                    columnIndex++
                }
                screenRow++
            }
        } finally {
            context.popClip()
        }
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        val shift = event.modifiers.has(Modifiers.SHIFT)
        val control = event.modifiers.has(Modifiers.CONTROL)
        return when (event.key) {
            Key.UP -> {
                moveCursor(-1, shift)
            }

            Key.DOWN -> {
                moveCursor(1, shift)
            }

            Key.PAGE_UP -> {
                moveCursor(-max(1, scroll.viewportHeight), shift)
            }

            Key.PAGE_DOWN -> {
                moveCursor(max(1, scroll.viewportHeight), shift)
            }

            Key.HOME -> {
                moveTo(0, shift)
            }

            Key.END -> {
                moveTo(orderedKeys.lastIndex, shift)
            }

            Key.LEFT -> {
                val changed = scroll.scrollBy(-1, 0)
                if (changed) {
                    invalidatePaint()
                }
                changed
            }

            Key.RIGHT -> {
                val changed = scroll.scrollBy(1, 0)
                if (changed) {
                    invalidatePaint()
                }
                changed
            }

            Key.SPACE -> {
                val key = selectionValue.cursor ?: return false
                selectionValue.toggle(key)
                ensureCursorVisible()
                invalidatePaint()
                true
            }

            Key.A -> {
                if (!control) {
                    false
                } else {
                    selectionValue.selectAll(orderedKeys)
                    ensureCursorVisible()
                    invalidatePaint()
                    true
                }
            }

            Key.ENTER -> {
                activateCursor()
            }

            else -> {
                false
            }
        }
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT || event.action != KeyAction.PRESS) {
            return false
        }
        if (event.localY == 0) {
            val contentX = event.localX + scroll.offsetX
            val index = columnAt(contentX)
            if (index < 0) {
                return false
            }
            onHeaderClick(index)
            return true
        }
        if (event.localY < 1 || event.localY >= 1 + bodyHeightValue) {
            return false
        }
        if (event.localX >= bodyWidthValue) {
            return false
        }
        val index = scroll.offsetY + event.localY - 1
        if (index !in itemsValue.indices) {
            return false
        }
        val key = orderedKeys[index]
        val shift = event.modifiers.has(Modifiers.SHIFT)
        val control = event.modifiers.has(Modifiers.CONTROL)
        if (event.clickCount >= 2 && !control && !shift) {
            selectionValue.select(key)
            onActivate(itemsValue[index])
        } else if (control && selectionMode == SelectionMode.MULTIPLE) {
            selectionValue.toggle(key)
        } else if (shift) {
            selectionValue.select(key, range = true)
        } else {
            selectionValue.select(key)
        }
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun onScrollEvent(event: GuiScrollEvent): Boolean {
        val dx = event.dx.roundToInt()
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

    private fun moveCursor(
        delta: Int,
        extend: Boolean,
    ): Boolean {
        val moved = selectionValue.moveCursor(delta, extend)
        ensureCursorVisible()
        invalidatePaint()
        return moved || delta != 0
    }

    private fun moveTo(
        index: Int,
        extend: Boolean,
    ): Boolean {
        val key = orderedKeys.getOrNull(index) ?: return false
        if (extend) {
            selectionValue.select(key, range = true)
        } else {
            selectionValue.select(key)
        }
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun activateCursor(): Boolean {
        val index = orderedKeys.indexOf(selectionValue.cursor)
        if (index < 0) {
            return false
        }
        onActivate(itemsValue[index])
        return true
    }

    private fun columnAt(contentX: Int): Int {
        val offsets = metricsValue.offsets
        val widths = metricsValue.widths
        var index = 0
        while (index < offsets.size) {
            if (contentX >= offsets[index] && contentX < offsets[index] + widths[index]) {
                return index
            }
            index++
        }
        return -1
    }

    private fun ensureCursorVisible() {
        val index = orderedKeys.indexOf(selectionValue.cursor)
        if (index < 0) {
            return
        }
        scroll.viewportHeight = bodyHeightValue
        scroll.contentHeight = itemsValue.size
        scroll.ensureVisible(0, index, 1, 1)
    }

    private fun selectionsRestored() {
        selectionValue.retain(orderedKeys)
    }

    private fun onSelectionChanged() {
        ensureCursorVisible()
        invalidatePaint()
    }

    private fun rebuildKeys() {
        val keys = ArrayList<Any>(itemsValue.size)
        for (index in itemsValue.indices) {
            keys.add(keyOfValue?.invoke(itemsValue[index]) ?: index)
        }
        orderedKeys = keys
        selectionValue.keys = keys
    }
}
