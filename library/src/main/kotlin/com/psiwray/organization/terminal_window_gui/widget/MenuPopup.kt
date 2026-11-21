package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A keyboard- and mouse-operable menu entry list.
 *
 * The popup is an overlay root. It does not own a window, a second overlay
 * layer, or a timer. Tall lists scroll with visible indicators and keep the
 * focused entry in view. Submenus open as additional overlay entries.
 *
 * @property items Entries in display order.
 * @property menuBar Owning menu bar, when this is a top-level menu.
 * @property menuIndex Owned menu index, or `-1` for a context menu.
 * @since 1.0.0
 */
public open class MenuPopup(
    items: List<MenuItem>,
    public val menuBar: MenuBar? = null,
    public var menuIndex: Int = -1,
) : Widget() {
    private var itemsValue: List<MenuItem> = items
    private var focusedIndexValue: Int = -1
    private var scrollOffsetValue: Int = 0
    private var hoverIndex: Int = -1
    private var host: OverlayHost? = null
    private var entryValue: OverlayEntry? = null
    private var previousFocus: Widget? = null
    private var restoreOnDetach: Boolean = true
    private var childPopup: MenuPopup? = null
    private var parentPopup: MenuPopup? = null
    private var closed: Boolean = false

    /** Called after the popup is detached. @since 1.0.0 */
    public var onClose: (() -> Unit)? = null

    /** Entries in display order. @since 1.0.0 */
    public var items: List<MenuItem>
        get() = itemsValue
        set(value) {
            if (itemsValue == value) {
                return
            }
            itemsValue = value
            focusedIndexValue = firstEnabledIndex(0)
            scrollOffsetValue = 0
            invalidateLayout()
        }

    /** Focused entry index, or `-1`. @since 1.0.0 */
    public var focusedIndex: Int
        get() = focusedIndexValue
        set(value) {
            val next = if (value in itemsValue.indices && isFocusableIndex(value)) value else -1
            if (focusedIndexValue == next) {
                return
            }
            focusedIndexValue = next
            ensureFocusedVisible()
            invalidatePaint()
        }

    /** Alias for [focusedIndex]. @since 1.0.0 */
    public var activeIndex: Int
        get() = focusedIndex
        set(value) {
            focusedIndex = value
        }

    /** Alias for [focusedIndex]. @since 1.0.0 */
    public var selectedIndex: Int
        get() = focusedIndex
        set(value) {
            focusedIndex = value
        }

    /** Current scroll offset in rows. @since 1.0.0 */
    public val scrollOffset: Int get() = scrollOffsetValue

    /** Overlay entry for this popup, if opened through [open]. @since 1.0.0 */
    public val entry: OverlayEntry? get() = entryValue

    /** Submenu popup currently open, if any. @since 1.0.0 */
    public val submenu: MenuPopup? get() = childPopup

    /** Parent menu, when this is a submenu. @since 1.0.0 */
    public val parentMenu: MenuPopup? get() = parentPopup

    /** Whether the popup is attached to an overlay host. @since 1.0.0 */
    public val isOpen: Boolean get() = entryValue != null || host?.entries?.any { it.widget === this } == true

    /** Child-first constructor for a top-level menu. @since 1.0.0 */
    public constructor(
        menu: Menu,
        menuBar: MenuBar? = null,
        menuIndex: Int = -1,
    ) : this(menu.items, menuBar, menuIndex)

    init {
        focusable = true
        cursorShape = CursorShape.HAND
        focusedIndexValue = firstEnabledIndex(0)
    }

    override fun measure(constraints: Constraints): IntSize {
        val width = preferredWidth()
        val rows = max(1, itemsValue.size)
        val height = if (constraints.hasBoundedHeight) minOf(rows, constraints.maxHeight) else rows
        return constraints.constrain(IntSize(width, max(1, height)))
    }

    override fun onAttach() {
        host = guiContext?.overlays
        if (entryValue == null) {
            entryValue = host?.entries?.firstOrNull { it.widget === this }
        }
        if (previousFocus == null) {
            previousFocus = guiContext?.focus?.focused
        }
        ensureFocusedVisible()
    }

    override fun onDetach() {
        childPopup?.close(restoreFocus = false)
        childPopup = null
        val manager = guiContext?.focus
        if (restoreOnDetach) {
            manager?.focus(previousFocus)
        }
        host = null
        entryValue = null
        closed = true
        onClose?.invoke()
    }

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        ensureFocusedVisible()
    }

    override fun onFocusChanged(focused: Boolean) {
        invalidatePaint()
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surface)
        val rows = visibleRows()
        val contentTop = if (scrollActive()) 1 else 0
        if (scrollActive() && bounds.height > 0) {
            context.shape(Shape.SCROLL_UP, 0, 0, theme.textMuted, theme.surface)
            context.shape(Shape.SCROLL_DOWN, 0, bounds.height - 1, theme.textMuted, theme.surface)
        }
        var visibleRow = 0
        var index = scrollOffsetValue
        while (index < itemsValue.size && visibleRow < rows) {
            paintItem(context, index, contentTop + visibleRow)
            visibleRow++
            index++
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onPointerButton(event)
            is GuiPointerMoveEvent -> onPointerMove(event)
            is GuiScrollEvent -> onScroll(event)
            else -> false
        }

    /** Opens this popup as a non-modal overlay entry. @since 1.0.0 */
    public fun open(
        overlays: OverlayHost,
        anchor: OverlayAnchor,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry {
        require(entryValue == null) { "menu popup is already open" }
        closed = false
        restoreOnDetach = true
        host = overlays
        previousFocus = overlays.focus?.focused
        val added =
            overlays.add(
                widget = this,
                anchor = anchor,
                placement = placement,
                dismissPolicy = dismissPolicy,
                modal = false,
                shadow = true,
            )
        entryValue = added
        overlays.focus?.focus(this)
        focusFirstItem()
        return added
    }

    /** Opens at a screen cell. @since 1.0.0 */
    public fun openAt(
        overlays: OverlayHost,
        column: Int,
        row: Int,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry =
        open(
            overlays,
            OverlayAnchor.Rect(Rect(column, row, 0, 0)),
            placement,
            dismissPolicy,
        )

    /** Closes this popup and any open submenu. @since 1.0.0 */
    public fun close(restoreFocus: Boolean = true) {
        childPopup?.close(restoreFocus = false)
        childPopup = null
        restoreOnDetach = restoreFocus
        val currentHost = host
        val currentEntry = entryValue
        if (currentEntry != null && currentHost != null) {
            currentHost.remove(currentEntry)
        } else if (currentHost != null) {
            currentHost.entries.firstOrNull { it.widget === this }?.let(currentHost::remove)
        }
        entryValue = null
        closed = true
    }

    /** Closes this popup and its child and restores focus. @since 1.0.0 */
    public fun dismiss() {
        close(restoreFocus = true)
    }

    /** Focuses the first enabled command or submenu. @since 1.0.0 */
    public fun focusFirstItem() {
        focusIndex(firstEnabledIndex(0))
    }

    /** Focuses the last enabled command or submenu. @since 1.0.0 */
    public fun focusLastItem() {
        focusIndex(lastEnabledIndex(itemsValue.lastIndex))
    }

    /** Runs the focused entry exactly once. @since 1.0.0 */
    public fun activateFocused(): Boolean = activate(focusedIndexValue)

    /** Runs entry [index] exactly once. @since 1.0.0 */
    public fun activate(index: Int): Boolean {
        if (closed || index !in itemsValue.indices) {
            return false
        }
        val item = itemsValue[index]
        return when (item) {
            is MenuItem.CommandItem -> {
                if (!item.command.isEnabled()) {
                    return false
                }
                rootMenu().closeAll()
                item.command.execute()
                true
            }

            is MenuItem.Submenu -> {
                openSubmenu(index)
                true
            }

            MenuItem.Separator -> {
                false
            }
        }
    }

    /** Opens the submenu at [index] when present. @since 1.0.0 */
    public fun openSubmenu(index: Int): Boolean {
        val item = itemsValue.getOrNull(index) as? MenuItem.Submenu ?: return false
        val currentHost = host ?: return false
        childPopup?.close(restoreFocus = false)
        val child =
            MenuPopup(
                items = item.items,
                menuBar = menuBar,
                menuIndex = menuIndex,
            )
        child.onClose = {
            if (childPopup === child) {
                childPopup = null
            }
        }
        val origin = screenOriginPoint()
        val anchor =
            OverlayAnchor.Rect(
                Rect(origin.x + bounds.width, origin.y + itemRowY(index), bounds.width, 1),
            )
        child.previousFocus = this
        child.parentPopup = this
        childPopup = child
        child.open(currentHost, anchor, OverlayPlacement.RIGHT, DismissPolicy.BOTH)
        child.focusFirstItem()
        return true
    }

    /** Closes this popup and every child, restoring the original focus. @since 1.0.0 */
    public fun closeAll() {
        close(restoreFocus = true)
    }

    /** Closes the root menu containing this popup. @since 1.0.0 */
    public fun dismissRoot() {
        rootMenu().closeAll()
    }

    /** Called by [MenuBar] when the active menu is switched. */
    internal fun switchTo(
        menuBar: MenuBar,
        index: Int,
    ) {
        close(restoreFocus = false)
        menuBar.open(index)
    }

    private fun rootMenu(): MenuPopup {
        var root = this
        while (root.parentPopup != null) {
            root = root.parentPopup!!
        }
        return root
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        return when (event.key) {
            Key.ESCAPE -> {
                closeAll()
                true
            }

            Key.UP -> {
                moveFocus(-1)
                true
            }

            Key.DOWN -> {
                moveFocus(1)
                true
            }

            Key.HOME -> {
                focusFirstItem()
                true
            }

            Key.END -> {
                focusLastItem()
                true
            }

            Key.ENTER, Key.KP_ENTER -> {
                activateFocused()
            }

            Key.RIGHT -> {
                val item = itemsValue.getOrNull(focusedIndexValue)
                if (item is MenuItem.Submenu) {
                    openSubmenu(focusedIndexValue)
                } else {
                    menuBar?.switchMenu(1)
                }
                true
            }

            Key.LEFT -> {
                when {
                    childPopup != null -> childPopup?.close()
                    parentPopup != null -> close()
                    else -> menuBar?.switchMenu(-1)
                }
                true
            }

            Key.F10 -> {
                closeAll()
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onPointerButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT || event.action != KeyAction.PRESS) {
            return false
        }
        if (event.localX < 0 || event.localX >= bounds.width || event.localY < 0 || event.localY >= bounds.height) {
            return false
        }
        if (scrollActive() && event.localY == 0) {
            scroll(-1)
            return true
        }
        if (scrollActive() && event.localY == bounds.height - 1) {
            scroll(1)
            return true
        }
        val index = itemAtRow(event.localY)
        if (index < 0) {
            return true
        }
        if (isFocusableIndex(index)) {
            focusIndex(index)
            activate(index)
        }
        return true
    }

    private fun onPointerMove(event: GuiPointerMoveEvent): Boolean {
        val index = itemAtRow(event.localY)
        if (index != hoverIndex) {
            hoverIndex = index
            invalidatePaint()
        }
        return index >= 0
    }

    private fun onScroll(event: GuiScrollEvent): Boolean {
        val delta = event.dy.roundToInt()
        if (delta == 0) {
            return false
        }
        return scroll(delta)
    }

    private fun scroll(delta: Int): Boolean {
        val next = (scrollOffsetValue + delta).coerceIn(0, maxScroll())
        if (next == scrollOffsetValue) {
            return false
        }
        scrollOffsetValue = next
        invalidatePaint()
        return true
    }

    private fun moveFocus(direction: Int) {
        if (itemsValue.isEmpty()) {
            return
        }
        var index = focusedIndexValue
        var remaining = itemsValue.size
        while (remaining-- > 0) {
            index = (index + direction + itemsValue.size) % itemsValue.size
            if (isFocusableIndex(index)) {
                focusIndex(index)
                return
            }
        }
    }

    private fun focusIndex(index: Int) {
        if (index < 0 || index >= itemsValue.size || !isFocusableIndex(index)) {
            return
        }
        focusedIndexValue = index
        ensureFocusedVisible()
        guiContext?.focus?.focus(this)
        invalidatePaint()
    }

    private fun ensureFocusedVisible() {
        if (focusedIndexValue < 0 || bounds.isEmpty) {
            return
        }
        val rows = visibleRows()
        if (rows <= 0) {
            return
        }
        if (focusedIndexValue < scrollOffsetValue) {
            scrollOffsetValue = focusedIndexValue
        } else if (focusedIndexValue >= scrollOffsetValue + rows) {
            scrollOffsetValue = focusedIndexValue - rows + 1
        }
        scrollOffsetValue = scrollOffsetValue.coerceIn(0, maxScroll())
    }

    private fun scrollActive(): Boolean = itemsValue.size > bounds.height && bounds.height >= 3

    private fun visibleRows(): Int {
        if (bounds.height <= 0) {
            return 0
        }
        return if (scrollActive()) (bounds.height - 2).coerceAtLeast(0) else bounds.height
    }

    private fun maxScroll(): Int = max(0, itemsValue.size - visibleRows())

    private fun itemAtRow(localY: Int): Int {
        val top = if (scrollActive()) 1 else 0
        val row = localY - top
        if (row < 0 || row >= visibleRows()) {
            return -1
        }
        return scrollOffsetValue + row
    }

    private fun itemRowY(index: Int): Int = (if (scrollActive()) 1 else 0) + index - scrollOffsetValue

    private fun firstEnabledIndex(start: Int): Int {
        var index = start
        while (index < itemsValue.size) {
            if (isFocusableIndex(index)) {
                return index
            }
            index++
        }
        return -1
    }

    private fun lastEnabledIndex(start: Int): Int {
        var index = start
        while (index >= 0) {
            if (isFocusableIndex(index)) {
                return index
            }
            index--
        }
        return -1
    }

    private fun isFocusableIndex(index: Int): Boolean =
        when (val item = itemsValue.getOrNull(index)) {
            is MenuItem.CommandItem -> item.command.isEnabled()
            is MenuItem.Submenu -> true
            MenuItem.Separator, null -> false
        }

    private fun preferredWidth(): Int {
        var widest = 8
        for (item in itemsValue) {
            val text =
                when (item) {
                    is MenuItem.CommandItem -> item.command.label
                    is MenuItem.Submenu -> item.title
                    MenuItem.Separator -> ""
                }
            val shortcut =
                when (item) {
                    is MenuItem.CommandItem -> {
                        item.command.shortcut
                            ?.label()
                            .orEmpty()
                    }

                    else -> {
                        ""
                    }
                }
            val shortcutSpace =
                if (shortcut.isEmpty()) 0 else 1 + TextPainter.codePointCount(shortcut) + 1
            val submenuSpace = if (item is MenuItem.Submenu) 1 else 0
            widest = max(widest, 5 + TextPainter.codePointCount(text) + shortcutSpace + submenuSpace)
        }
        return widest
    }

    private fun paintItem(
        context: PaintContext,
        index: Int,
        y: Int,
    ) {
        val theme = context.theme
        val item = itemsValue[index]
        val width = bounds.width
        if (item === MenuItem.Separator) {
            context.fill(Rect(0, y, width, 1), theme.text, theme.surface)
            var x = 1
            while (x < width - 1) {
                context.shape(Shape.BOX_SINGLE_H, x, y, theme.textMuted, theme.surface)
                x++
            }
            return
        }
        val focused = index == focusedIndexValue && isFocused
        val selected = index == hoverIndex
        val style =
            when {
                item is MenuItem.CommandItem && !item.command.isEnabled() -> theme.style(WidgetState.DISABLED)
                focused -> theme.style(WidgetState.SELECTED)
                selected -> theme.style(WidgetState.HOVERED)
                else -> theme.style(WidgetState.NORMAL)
            }
        context.fill(Rect(0, y, width, 1), style.fg, style.bg)
        if (focused) {
            context.text(0, y, ">", style.fg, style.bg)
        }
        val marker =
            when (item) {
                is MenuItem.CommandItem -> {
                    when {
                        item.radio && item.command.isChecked() -> Shape.DOT
                        item.command.isChecked() -> Shape.CHECK
                        else -> null
                    }
                }

                is MenuItem.Submenu -> {
                    Shape.TREE_COLLAPSED
                }

                MenuItem.Separator -> {
                    null
                }
            }
        val label =
            when (item) {
                is MenuItem.CommandItem -> item.command.label
                is MenuItem.Submenu -> item.title
                MenuItem.Separator -> ""
            }
        val shortcut =
            when (item) {
                is MenuItem.CommandItem -> {
                    item.command.shortcut
                        ?.label()
                        .orEmpty()
                }

                else -> {
                    ""
                }
            }
        val shortcutWidth = TextPainter.codePointCount(shortcut)
        val submenuWidth = if (item is MenuItem.Submenu) 1 else 0
        val labelX = 3
        val shortcutX = (width - 2 - shortcutWidth).coerceAtLeast(labelX)
        val labelWidth = (shortcutX - labelX - if (shortcut.isEmpty()) 0 else 1).coerceAtLeast(0)
        if (marker != null) {
            context.shape(marker, 1, y, style.fg, style.bg)
        }
        TextPainter.draw(
            context = context,
            line = label,
            x = labelX,
            y = y,
            width = labelWidth,
            alignment = TextAlignment.LEFT,
            fg = style.fg,
            bg = style.bg,
            ellipsis = TextPainter.codePointCount(label) > labelWidth,
        )
        if (shortcut.isNotEmpty()) {
            TextPainter.draw(
                context = context,
                line = shortcut,
                x = shortcutX,
                y = y,
                width = shortcutWidth,
                alignment = TextAlignment.RIGHT,
                fg = theme.textMuted,
                bg = style.bg,
                ellipsis = false,
            )
        }
        if (item is MenuItem.Submenu && width > 1) {
            context.shape(Shape.TREE_COLLAPSED, width - 2 - submenuWidth, y, style.fg, style.bg)
        }
    }
}
