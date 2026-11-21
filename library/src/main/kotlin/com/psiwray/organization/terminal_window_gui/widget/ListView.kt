package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.FocusScope
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.TypeAhead
import com.psiwray.organization.terminal_window_gui.internal.VisibleRowCache
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Virtualized one-row-per-item list with stable-key selection.
 *
 * Only rows intersecting the viewport are built and attached. [itemBuilder]
 * receives the current item index and item and must return a widget that fits
 * one row. [keyOf] defaults to the item index, which is unstable across
 * reordering; supply a stable application key for selection preservation.
 *
 * @property onActivate Called for Enter and double-click activation.
 * @property emptyText Muted text used when [items] is empty.
 * @since 1.0.0
 */
public class ListView<T>(
    items: List<T> = emptyList(),
    itemBuilder: (Int, T) -> Widget = { _, item -> Label(item.toString()) },
    keyOf: ((T) -> Any)? = null,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    selection: SelectionModel = SelectionModel(selectionMode),
    public var onActivate: (T) -> Unit = {},
    emptyText: String = "No items",
) : ContainerWidget() {
    private var itemsValue: List<T> = items
    private var itemBuilderValue: (Int, T) -> Widget = itemBuilder
    private var keyOfValue: ((T) -> Any)? = keyOf
    private var textOfValue: ((T) -> String)? = null
    private var selectionValue: SelectionModel = selection
    private var orderedKeys: List<Any> = emptyList()
    private val rowCache = VisibleRowCache(itemBuilderValue)
    private val typeAhead = TypeAhead()
    private var emptyTextValue: String = emptyText
    private val emptyLabel = Label(emptyText).also { it.focusable = false }
    private val selectionListener: () -> Unit = { onSelectionChanged() }
    private val scrollListener: () -> Unit = { onScrollChanged() }

    /** Scroll state shared with the optional scrollbar. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    private val scrollBarValue = ScrollBar(scroll, Orientation.VERTICAL, ScrollBarVisibility.AUTO)

    /** Trailing item-builder convenience constructor. @since 1.0.0 */
    public constructor(
        items: List<T>,
        itemBuilder: (Int, T) -> Widget,
    ) : this(
        items = items,
        itemBuilder = itemBuilder,
        keyOf = null,
        selectionMode = SelectionMode.SINGLE,
        selection = SelectionModel(SelectionMode.SINGLE),
        onActivate = {},
        emptyText = "No items",
    )

    init {
        focusable = true
        scrollBarValue.focusable = false
        adopt(scrollBarValue)
        scrollBarValue.visible = true
        selectionValue.addListener(selectionListener)
        scroll.addOffsetListener(scrollListener)
        rebuildKeys()
    }

    /** Text shown when there are no items. @since 1.0.0 */
    public var emptyText: String
        get() = emptyTextValue
        set(value) {
            if (emptyTextValue == value) {
                return
            }
            emptyTextValue = value
            emptyLabel.text = value
            invalidatePaint()
        }

    /** Items in display order. Assigning a new list invalidates the row cache. @since 1.0.0 */
    public var items: List<T>
        get() = itemsValue
        set(value) {
            if (itemsValue === value) {
                return
            }
            dropAllRows()
            itemsValue = value
            rebuildKeys()
            selectionValue.retain(orderedKeys)
            invalidateLayout()
            invalidatePaint()
        }

    /** Builds the row widget for one visible item. @since 1.0.0 */
    public var itemBuilder: (Int, T) -> Widget
        get() = itemBuilderValue
        set(value) {
            if (itemBuilderValue === value) {
                return
            }
            itemBuilderValue = value
            rowCache.builder = value
            dropAllRows()
            invalidateLayout()
            invalidatePaint()
        }

    /** Stable key accessor, or `null` to use the item index. @since 1.0.0 */
    public var keyOf: ((T) -> Any)?
        get() = keyOfValue
        set(value) {
            if (keyOfValue === value) {
                return
            }
            keyOfValue = value
            dropAllRows()
            rebuildKeys()
            selectionValue.retain(orderedKeys)
            invalidateLayout()
            invalidatePaint()
        }

    /** Text used by type-ahead, or `null` to use `item.toString()`. @since 1.0.0 */
    public var textOf: ((T) -> String)?
        get() = textOfValue
        set(value) {
            textOfValue = value
            invalidatePaint()
        }

    /** Selection state. Replacing it keeps this widget synchronized. @since 1.0.0 */
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

    /** Rebuilds keys and rows after in-place item changes. @since 1.0.0 */
    public fun refresh() {
        dropAllRows()
        rebuildKeys()
        selectionValue.retain(orderedKeys)
        invalidateLayout()
        invalidatePaint()
    }

    /** Optional vertical scrollbar. @since 1.0.0 */
    public val scrollBar: ScrollBar get() = scrollBarValue

    /** Alias for [scrollBar]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = scroll

    override fun onFocusChanged(focused: Boolean) {
        invalidatePaint()
    }

    override fun measure(constraints: Constraints): IntSize {
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                20
            }
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                itemsValue.size.coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val bodyHeight = bounds.height
        val barShown = itemsValue.size > bodyHeight
        val bodyWidth = (bounds.width - if (barShown) 1 else 0).coerceAtLeast(1)
        scroll.viewportWidth = bodyWidth
        scroll.viewportHeight = bodyHeight
        scroll.contentWidth = bodyWidth
        scroll.contentHeight = itemsValue.size
        scrollBarValue.arrange(
            if (barShown) {
                Rect(bodyWidth, 0, 1, bodyHeight)
            } else {
                Rect.EMPTY
            },
        )
        ensureCursorVisible()
        syncRows(bodyWidth, bodyHeight)
    }

    override fun paint(context: PaintContext) {
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surface)
        emptyLabel.foreground = null
        if (itemsValue.isEmpty()) {
            emptyLabel.foreground = theme.textMuted
            return
        }
        val bodyHeight = bounds.height
        val bodyWidth = (bounds.width - if (itemsValue.size > bodyHeight) 1 else 0).coerceAtLeast(1)
        val start = scroll.offsetY
        var row = 0
        while (row < bodyHeight) {
            val index = start + row
            if (index >= itemsValue.size) {
                break
            }
            val key = orderedKeys[index]
            val selected = selectionValue.isSelected(key)
            val bg = if (selected) theme.selectionBackground else theme.surface
            val fg = if (selected) theme.selectionForeground else theme.text
            if (selected) {
                context.fill(Rect(0, row, bodyWidth, 1), fg, bg)
            }
            if (isFocused && selectionValue.cursor == key && bodyWidth > 0) {
                context.shape(Shape.ARROW_RIGHT, bodyWidth - 1, row, theme.accent, bg)
            }
            row++
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiCharEvent -> onText(event)
            is GuiPointerButtonEvent -> onButton(event)
            is GuiScrollEvent -> onScrollEvent(event)
            else -> false
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

    private fun onText(event: GuiCharEvent): Boolean {
        val codePoint = event.codePoint
        if (codePoint < 0x20 || codePoint == 0x7F) {
            return false
        }
        val now = guiContext?.scheduler?.now() ?: System.nanoTime()
        typeAhead.append(codePoint, now)
        val labels = ArrayList<String>(itemsValue.size)
        for (item in itemsValue) {
            labels.add(textOfValue?.invoke(item) ?: item.toString())
        }
        val cursorIndex = orderedKeys.indexOf(selectionValue.cursor)
        val match = typeAhead.match(labels, cursorIndex, now) ?: return false
        selectionValue.select(orderedKeys[match])
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT || event.action != KeyAction.PRESS) {
            return false
        }
        val bodyHeight = bounds.height
        if (event.localY < 0 || event.localY >= bodyHeight) {
            return false
        }
        val barShown = itemsValue.size > bodyHeight
        if (barShown && event.localX >= (bounds.width - 1).coerceAtLeast(0)) {
            return false
        }
        val index = scroll.offsetY + event.localY
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
        val dy = event.dy.roundToInt()
        if (dy == 0) {
            return false
        }
        val changed = scroll.scrollBy(0, dy)
        if (changed) {
            invalidateLayout()
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

    private fun ensureCursorVisible() {
        val index = orderedKeys.indexOf(selectionValue.cursor)
        if (index < 0) {
            return
        }
        scroll.viewportHeight = bounds.height
        scroll.contentHeight = itemsValue.size
        scroll.ensureVisible(0, index, 1, 1)
    }

    private fun onSelectionChanged() {
        ensureCursorVisible()
        invalidateLayout()
        invalidatePaint()
    }

    private fun onScrollChanged() {
        invalidateLayout()
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

    private fun syncRows(
        bodyWidth: Int,
        bodyHeight: Int,
    ) {
        if (itemsValue.isEmpty()) {
            dropAllRows()
            if (emptyLabel.parent !== this) {
                adopt(emptyLabel)
            }
            emptyLabel.measured(Constraints.tight(IntSize(bodyWidth, 1)))
            emptyLabel.arrange(Rect(0, 0, bodyWidth, 1))
            return
        }
        if (emptyLabel.parent === this) {
            drop(emptyLabel)
        }
        val start = scroll.offsetY
        val end = minOf(itemsValue.size, start + bodyHeight)
        val visible = LinkedHashSet<Any>()
        var index = start
        while (index < end) {
            val key = orderedKeys[index]
            visible.add(key)
            val widget = rowCache.row(index, itemsValue[index], key)
            widget.focusScope = FocusScope.SKIP
            if (widget.parent !== this) {
                adopt(widget)
            }
            widget.measured(Constraints.tight(IntSize(bodyWidth, 1)))
            widget.arrange(Rect(0, index - start, bodyWidth, 1))
            index++
        }
        for (removed in rowCache.retain(visible)) {
            if (removed.parent === this) {
                drop(removed)
            }
        }
    }

    private fun dropAllRows() {
        for (widget in rowCache.clear()) {
            if (widget.parent === this) {
                drop(widget)
            }
        }
        if (emptyLabel.parent === this) {
            drop(emptyLabel)
        }
    }
}
