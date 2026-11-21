package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.internal.TypeAhead
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import java.util.Collections
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Virtualized tree over eager nodes.
 *
 * Visible nodes are flattened and cached until the node list, expansion set,
 * accessors, or [refresh] changes. Only flattened rows intersecting the
 * viewport are painted. Selection uses stable keys from [keyOf]; multiple
 * selection is not the v1 default but a supplied [SelectionModel] is honored.
 *
 * @property onActivate Called for Enter and non-expander double-click.
 * @since 1.0.0
 */
public class TreeView<T>(
    nodes: List<T> = emptyList(),
    keyOf: ((T) -> Any)? = null,
    label: ((T) -> String)? = null,
    children: ((T) -> List<T>)? = null,
    expandedKeys: Set<Any> = emptySet(),
    selection: SelectionModel = SelectionModel(SelectionMode.SINGLE),
    emptyText: String = "No nodes",
    guideLines: Boolean = true,
    icon: ((T) -> Shape?)? = null,
    public var onActivate: (T) -> Unit = {},
) : ContainerWidget() {
    private var nodesValue: List<T> = nodes
    private var keyOfValue: ((T) -> Any)? = keyOf
    private var labelValue: ((T) -> String)? = label
    private var childrenValue: ((T) -> List<T>)? = children
    private var iconValue: ((T) -> Shape?)? = icon
    private var expandedValue: LinkedHashSet<Any> = LinkedHashSet(expandedKeys)
    private var selectionValue: SelectionModel = selection
    private var emptyTextValue: String = emptyText
    private var guideLinesValue: Boolean = guideLines
    private var flatRows: List<FlatRow<T>>? = null
    private var allKeys: List<Any> = emptyList()
    private val typeAhead = TypeAhead()
    private val selectionListener: () -> Unit = { onSelectionChanged() }

    /** Vertical scroll state. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    private val scrollBarValue = ScrollBar(scroll, Orientation.VERTICAL, ScrollBarVisibility.AUTO)

    /** Constructor matching the section signature for positional calls. @since 1.0.0 */
    public constructor(
        nodes: List<T>,
        keyOf: ((T) -> Any)?,
        label: ((T) -> String)?,
        children: ((T) -> List<T>)?,
        expandedKeys: Set<Any>,
        selection: SelectionModel,
        onActivate: (T) -> Unit,
    ) : this(
        nodes = nodes,
        keyOf = keyOf,
        label = label,
        children = children,
        expandedKeys = expandedKeys,
        selection = selection,
        emptyText = "No nodes",
        guideLines = true,
        icon = null,
        onActivate = onActivate,
    )

    init {
        focusable = true
        scrollBarValue.focusable = false
        adopt(scrollBarValue)
        selectionValue.addListener(selectionListener)
        rebuildFlat()
    }

    /** Root nodes. @since 1.0.0 */
    public var nodes: List<T>
        get() = nodesValue
        set(value) {
            if (nodesValue === value) {
                return
            }
            nodesValue = value
            refresh()
        }

    /** Stable key accessor. @since 1.0.0 */
    public var keyOf: ((T) -> Any)?
        get() = keyOfValue
        set(value) {
            if (keyOfValue === value) {
                return
            }
            keyOfValue = value
            refresh()
        }

    /** Label accessor. @since 1.0.0 */
    public var label: ((T) -> String)?
        get() = labelValue
        set(value) {
            if (labelValue === value) {
                return
            }
            labelValue = value
            invalidatePaint()
        }

    /** Child accessor. @since 1.0.0 */
    public var children: ((T) -> List<T>)?
        get() = childrenValue
        set(value) {
            if (childrenValue === value) {
                return
            }
            childrenValue = value
            refresh()
        }

    /** Icon accessor. @since 1.0.0 */
    public var icon: ((T) -> Shape?)?
        get() = iconValue
        set(value) {
            if (iconValue === value) {
                return
            }
            iconValue = value
            invalidatePaint()
        }

    /** Expanded stable keys. @since 1.0.0 */
    public var expandedKeys: Set<Any>
        get() = Collections.unmodifiableSet(expandedValue)
        set(value) {
            if (expandedValue == value) {
                return
            }
            expandedValue = LinkedHashSet(value)
            refresh()
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
            selectionValue.keys = flat().map { it.key }
            selectionValue.retain(allKeys)
            onSelectionChanged()
            invalidateLayout()
        }

    /** Current selection policy alias. @since 1.0.0 */
    public val selectionMode: SelectionMode get() = selectionValue.mode

    /** Text shown when there are no nodes. @since 1.0.0 */
    public var emptyText: String
        get() = emptyTextValue
        set(value) {
            emptyTextValue = value
            invalidatePaint()
        }

    /** Whether ancestor guide cells are painted. @since 1.0.0 */
    public var guideLines: Boolean
        get() = guideLinesValue
        set(value) {
            if (guideLinesValue == value) {
                return
            }
            guideLinesValue = value
            invalidatePaint()
        }

    /** Optional vertical scrollbar. @since 1.0.0 */
    public val scrollBar: ScrollBar get() = scrollBarValue

    /** Alias for [scroll]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = scroll

    /** Rebuilds the flattened visible-node cache. @since 1.0.0 */
    public fun refresh() {
        flatRows = null
        rebuildFlat()
        invalidateLayout()
        invalidatePaint()
    }

    /** Returns whether [key] is expanded. @since 1.0.0 */
    public fun isExpanded(key: Any): Boolean = expandedValue.contains(key)

    /** Expands [key] when it has children and is collapsed. @since 1.0.0 */
    public fun expand(key: Any): Boolean {
        val row = flat().firstOrNull { it.key == key }
        if (row == null || !row.hasChildren) {
            return false
        }
        if (!expandedValue.add(key)) {
            return false
        }
        refresh()
        return true
    }

    /** Collapses [key] when expanded. @since 1.0.0 */
    public fun collapse(key: Any): Boolean {
        if (!expandedValue.remove(key)) {
            return false
        }
        refresh()
        return true
    }

    /** Toggles expansion of [key] when it has children. @since 1.0.0 */
    public fun toggleExpansion(key: Any): Boolean {
        val row = flat().firstOrNull { it.key == key } ?: return false
        if (!row.hasChildren) {
            return false
        }
        return if (row.expanded) collapse(key) else expand(key)
    }

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
                flat().size.coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val bodyHeight = bounds.height
        val barShown = flat().size > bodyHeight
        val bodyWidth = (bounds.width - if (barShown) 1 else 0).coerceAtLeast(1)
        scroll.viewportWidth = bodyWidth
        scroll.viewportHeight = bodyHeight
        scroll.contentWidth = bodyWidth
        scroll.contentHeight = flat().size
        scrollBarValue.arrange(
            if (barShown) Rect(bodyWidth, 0, 1, bodyHeight) else Rect.EMPTY,
        )
        ensureCursorVisible()
    }

    override fun paint(context: PaintContext) {
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surface)
        val rows = flat()
        if (rows.isEmpty()) {
            if (emptyText.isNotEmpty()) {
                TextPainter.draw(
                    context = context,
                    line = emptyText,
                    x = 0,
                    y = 0,
                    width = bounds.width,
                    alignment = TextAlignment.LEFT,
                    fg = theme.textMuted,
                    bg = theme.surface,
                    ellipsis = TextPainter.codePointCount(emptyText) > bounds.width,
                )
            }
            return
        }
        val start = scroll.offsetY
        var screenRow = 0
        while (screenRow < bounds.height) {
            val index = start + screenRow
            if (index >= rows.size) {
                break
            }
            val row = rows[index]
            val selected = selectionValue.isSelected(row.key)
            val rowFg = if (selected) theme.selectionForeground else theme.text
            val rowBg = if (selected) theme.selectionBackground else theme.surface
            if (selected) {
                context.fill(Rect(0, screenRow, bounds.width, 1), rowFg, rowBg)
            }
            paintRow(context, row, screenRow, rowFg, rowBg)
            if (isFocused && selectionValue.cursor == row.key && bounds.width > 0) {
                context.shape(Shape.ARROW_RIGHT, bounds.width - 1, screenRow, theme.accent, rowBg)
            }
            screenRow++
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

    private fun paintRow(
        context: PaintContext,
        row: FlatRow<T>,
        screenRow: Int,
        fg: Int,
        bg: Int,
    ) {
        val depth = row.depth
        if (guideLinesValue) {
            var level = 0
            while (level < depth) {
                if (row.ancestorHasNext.getOrElse(level) { false }) {
                    context.shape(Shape.TREE_VERTICAL, level * 2, screenRow, context.theme.border, bg)
                }
                level++
            }
        }
        if (depth > 0) {
            val branch = if (row.lastSibling) Shape.TREE_LAST else Shape.TREE_BRANCH
            context.shape(branch, depth * 2 - 1, screenRow, context.theme.border, bg)
        }
        if (row.hasChildren) {
            val shape = if (row.expanded) Shape.TREE_EXPANDED else Shape.TREE_COLLAPSED
            context.shape(shape, depth * 2, screenRow, fg, bg)
        }
        var x = depth * 2 + 1
        val nodeIcon = iconOf(row.node)
        if (nodeIcon != null) {
            context.shape(nodeIcon, x, screenRow, fg, bg)
            x++
        }
        val labelText = labelOf(row.node)
        val line = labelText.replace('\n', ' ')
        val available = (bounds.width - x).coerceAtLeast(0)
        TextPainter.draw(
            context = context,
            line = line,
            x = x,
            y = screenRow,
            width = available,
            alignment = TextAlignment.LEFT,
            fg = fg,
            bg = bg,
            ellipsis = TextPainter.codePointCount(line) > available,
        )
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        val shift = event.modifiers.has(Modifiers.SHIFT)
        val rows = flat()
        if (rows.isEmpty()) {
            return false
        }
        val cursorIndex = rows.indexOfFirst { it.key == selectionValue.cursor }
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
                moveTo(rows.lastIndex, shift)
            }

            Key.RIGHT -> {
                if (cursorIndex < 0) {
                    moveTo(0, shift)
                } else {
                    expandOrChild(rows[cursorIndex])
                }
            }

            Key.LEFT -> {
                if (cursorIndex < 0) {
                    false
                } else {
                    collapseOrParent(rows[cursorIndex])
                }
            }

            Key.SPACE -> {
                if (cursorIndex < 0) {
                    false
                } else {
                    toggleExpansion(rows[cursorIndex].key)
                }
            }

            Key.ENTER -> {
                if (cursorIndex < 0) {
                    false
                } else {
                    onActivate(rows[cursorIndex].node)
                    true
                }
            }

            else -> {
                false
            }
        }
    }

    private fun onText(event: GuiCharEvent): Boolean {
        if (event.codePoint < 0x20 || event.codePoint == 0x7F) {
            return false
        }
        val now = guiContext?.scheduler?.now() ?: System.nanoTime()
        typeAhead.append(event.codePoint, now)
        val rows = flat()
        val labels = rows.map { labelOf(it.node) }
        val cursorIndex = rows.indexOfFirst { it.key == selectionValue.cursor }
        val match = typeAhead.match(labels, cursorIndex, now) ?: return false
        selectionValue.select(rows[match].key)
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT || event.action != KeyAction.PRESS) {
            return false
        }
        val rows = flat()
        if (event.localY < 0 || event.localY >= bounds.height) {
            return false
        }
        val barShown = rows.size > bounds.height
        if (barShown && event.localX >= (bounds.width - 1).coerceAtLeast(0)) {
            return false
        }
        val index = scroll.offsetY + event.localY
        if (index !in rows.indices) {
            return false
        }
        val row = rows[index]
        val shift = event.modifiers.has(Modifiers.SHIFT)
        val control = event.modifiers.has(Modifiers.CONTROL)
        val expanderX = row.depth * 2
        if (event.clickCount >= 2 && !control && !shift) {
            selectionValue.select(row.key)
            onActivate(row.node)
        } else {
            if (control && selectionMode == SelectionMode.MULTIPLE) {
                selectionValue.toggle(row.key)
            } else if (shift) {
                selectionValue.select(row.key, range = true)
            } else {
                selectionValue.select(row.key)
            }
            if (row.hasChildren && event.localX == expanderX) {
                toggleExpansion(row.key)
            }
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
        val row = flat().getOrNull(index) ?: return false
        if (extend) {
            selectionValue.select(row.key, range = true)
        } else {
            selectionValue.select(row.key)
        }
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun expandOrChild(row: FlatRow<T>): Boolean {
        if (row.hasChildren && !row.expanded) {
            expand(row.key)
            selectionValue.select(row.key)
            ensureCursorVisible()
            return true
        }
        val rows = flat()
        val index = rows.indexOfFirst { it.key == row.key }
        if (row.hasChildren && index >= 0 && index + 1 < rows.size && rows[index + 1].depth > row.depth) {
            selectionValue.select(rows[index + 1].key)
            ensureCursorVisible()
            invalidatePaint()
            return true
        }
        return false
    }

    private fun collapseOrParent(row: FlatRow<T>): Boolean {
        if (row.hasChildren && row.expanded) {
            collapse(row.key)
            selectionValue.select(row.key)
            ensureCursorVisible()
            return true
        }
        val rows = flat()
        val index = rows.indexOfFirst { it.key == row.key }
        var parent = index - 1
        while (parent >= 0 && rows[parent].depth >= row.depth) {
            parent--
        }
        if (parent < 0) {
            return false
        }
        selectionValue.select(rows[parent].key)
        ensureCursorVisible()
        invalidatePaint()
        return true
    }

    private fun ensureCursorVisible() {
        val index = flat().indexOfFirst { it.key == selectionValue.cursor }
        if (index < 0) {
            return
        }
        scroll.viewportHeight = bounds.height
        scroll.contentHeight = flat().size
        scroll.ensureVisible(0, index, 1, 1)
    }

    private fun onSelectionChanged() {
        ensureCursorVisible()
        invalidatePaint()
    }

    private fun flat(): List<FlatRow<T>> = flatRows ?: rebuildFlat()

    private fun rebuildFlat(): List<FlatRow<T>> {
        val rows = ArrayList<FlatRow<T>>()
        val keys = ArrayList<Any>()

        fun collect(node: T) {
            keys.add(keyOfNode(node))
            for (child in childrenOf(node)) {
                collect(child)
            }
        }
        for (node in nodesValue) {
            collect(node)
        }

        fun visit(
            node: T,
            depth: Int,
            lastSibling: Boolean,
            ancestors: List<Boolean>,
        ) {
            val key = keyOfNode(node)
            val childNodes = childrenOf(node)
            val expanded = expandedValue.contains(key)
            rows.add(FlatRow(node, key, depth, lastSibling, ancestors, childNodes.isNotEmpty(), expanded))
            if (expanded) {
                var index = 0
                while (index < childNodes.size) {
                    visit(
                        node = childNodes[index],
                        depth = depth + 1,
                        lastSibling = index == childNodes.lastIndex,
                        ancestors = ancestors + !lastSibling,
                    )
                    index++
                }
            }
        }
        var index = 0
        while (index < nodesValue.size) {
            visit(nodesValue[index], 0, index == nodesValue.lastIndex, emptyList())
            index++
        }
        flatRows = rows
        allKeys = keys
        selectionValue.retain(keys)
        selectionValue.keys = rows.map { it.key }
        return rows
    }

    @Suppress("UNCHECKED_CAST")
    private fun childrenOf(node: T): List<T> {
        val accessor = childrenValue
        if (accessor != null) {
            return accessor(node)
        }
        val tree = node as? TreeNode<*> ?: return emptyList()
        return tree.children as List<T>
    }

    private fun keyOfNode(node: T): Any {
        keyOfValue?.let { return it(node) }
        val tree = node as? TreeNode<*>
        return tree?.key ?: (node ?: "null")
    }

    private fun labelOf(node: T): String {
        labelValue?.let { return it(node) }
        val tree = node as? TreeNode<*>
        return tree?.label ?: node.toString()
    }

    private fun iconOf(node: T): Shape? {
        iconValue?.let { return it(node) }
        val tree = node as? TreeNode<*>
        return tree?.icon
    }

    private data class FlatRow<T>(
        val node: T,
        val key: Any,
        val depth: Int,
        val lastSibling: Boolean,
        val ancestorHasNext: List<Boolean>,
        val hasChildren: Boolean,
        val expanded: Boolean,
    )
}
