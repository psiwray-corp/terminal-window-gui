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
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import kotlin.math.max

/**
 * One-row application menu bar.
 *
 * A menu opens with `F10`, `Alt+<mnemonic>`, or a pointer press. Left and Right
 * switch between open menus. Escape closes the whole menu and restores the
 * previously focused widget. The bar itself draws a non-color bracket marker
 * around the open menu.
 *
 * @since 1.0.0
 */
public class MenuBar(
    menus: List<Menu>,
) : Widget() {
    private var menusValue: List<Menu> = menus
    private var activeIndexValue: Int = -1
    private var popup: MenuPopup? = null
    private var restoreFocus: Widget? = null
    private var starts: IntArray = IntArray(0)
    private var widths: IntArray = IntArray(0)

    /** Menus in display order. @since 1.0.0 */
    public var menus: List<Menu>
        get() = menusValue
        set(value) {
            if (menusValue == value) {
                return
            }
            closeMenu(restore = false)
            menusValue = value
            invalidateLayout()
        }

    /** Open menu index, or `-1`. @since 1.0.0 */
    public val activeIndex: Int get() = activeIndexValue

    /** Open menu, or `null`. @since 1.0.0 */
    public val activeMenu: Menu? get() = menusValue.getOrNull(activeIndexValue)

    /** Alias for [activeIndex]. @since 1.0.0 */
    public val activeMenuIndex: Int get() = activeIndexValue

    /** Whether a menu is open. @since 1.0.0 */
    public val isOpen: Boolean get() = activeIndexValue >= 0

    /** Open popup, or `null`. @since 1.0.0 */
    public val popupMenu: MenuPopup? get() = popup

    init {
        focusable = true
        tabStop = false
        cursorShape = CursorShape.HAND
    }

    /** Vararg convenience constructor. @since 1.0.0 */
    public constructor(vararg menus: Menu) : this(menus.toList())

    override fun measure(constraints: Constraints): IntSize {
        computeLayout()
        val preferred = if (starts.isEmpty()) 1 else starts.last() + widths.last()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else preferred
        return constraints.constrain(IntSize(max(1, width), 1))
    }

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        computeLayout()
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        computeLayout()
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, 1), theme.text, theme.surface)
        for (index in menusValue.indices) {
            val start = starts[index]
            val width = widths[index]
            if (start >= bounds.width) {
                break
            }
            val active = index == activeIndexValue
            val style = if (active) theme.style(WidgetState.SELECTED) else theme.style(WidgetState.NORMAL)
            val available = (minOf(width, bounds.width - start)).coerceAtLeast(0)
            context.fill(Rect(start, 0, available, 1), style.fg, style.bg)
            val title = menusValue[index].title
            val inner = (available - 2).coerceAtLeast(0)
            TextPainter.draw(
                context = context,
                line = title,
                x = start + 1,
                y = 0,
                width = inner,
                alignment = TextAlignment.LEFT,
                fg = style.fg,
                bg = style.bg,
                ellipsis = TextPainter.codePointCount(title) > inner,
            )
            if (active && available > 0) {
                context.text(start, 0, "[", style.fg, style.bg)
                if (available > 1) {
                    context.text(start + available - 1, 0, "]", style.fg, style.bg)
                }
            }
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onPointerButton(event)
            is GuiPointerMoveEvent -> onPointerMove(event)
            else -> false
        }

    override fun onGlobalKey(event: GuiKeyEvent): Boolean {
        if (event.key != Key.F10 || event.action == KeyAction.RELEASE) {
            return false
        }
        if (event.repeat) {
            return activeIndexValue >= 0
        }
        if (menusValue.isEmpty()) {
            return false
        }
        if (activeIndexValue >= 0) {
            closeMenu(restore = true)
        } else {
            open(firstMenuIndex())
        }
        return true
    }

    override fun onDetach() {
        closeMenu(restore = false)
    }

    override fun matchesMnemonic(codePoint: Int): Boolean =
        menusValue.any { menu ->
            val char = menu.mnemonic ?: return@any false
            char.lowercaseChar().code == codePoint || char.uppercaseChar().code == codePoint
        }

    override fun activateMnemonic(codePoint: Int): Boolean {
        val index =
            menusValue.indexOfFirst { menu ->
                val char = menu.mnemonic ?: return@indexOfFirst false
                char.lowercaseChar().code == codePoint || char.uppercaseChar().code == codePoint
            }
        if (index < 0) {
            return false
        }
        open(index)
        return true
    }

    /** Alias for [open]. @since 1.0.0 */
    public fun openMenu(index: Int) {
        open(index)
    }

    /** Opens menu [index]. @since 1.0.0 */
    public fun open(index: Int) {
        if (index !in menusValue.indices) {
            return
        }
        val host = guiContext?.overlays ?: return
        if (popup != null) {
            popup?.close(restoreFocus = false)
            popup = null
        }
        restoreFocus = guiContext?.focus?.focused
        computeLayout()
        val origin = screenOriginPoint()
        val menu = menusValue[index]
        val start = starts.getOrElse(index) { 0 }
        val width = widths.getOrElse(index) { max(1, TextPainter.codePointCount(menu.title) + 2) }
        val anchor = OverlayAnchor.Rect(Rect(origin.x + start, origin.y, width, 1))
        val created =
            MenuPopup(menu, menuBar = this, menuIndex = index).also {
                it.onClose = {
                    if (popup === it) {
                        popup = null
                        activeIndexValue = -1
                    }
                }
            }
        popup = created
        activeIndexValue = index
        created.open(host, anchor, OverlayPlacement.BELOW, DismissPolicy.BOTH)
        created.focusFirstItem()
        invalidatePaint()
    }

    /** Alias for [closeMenu]. @since 1.0.0 */
    public fun close() {
        closeMenu(restore = true)
    }

    /** Closes the open menu. @since 1.0.0 */
    public fun closeMenu(restore: Boolean = true) {
        val current = popup
        popup = null
        activeIndexValue = -1
        current?.close(restoreFocus = restore)
        if (restore) {
            guiContext?.focus?.focus(restoreFocus)
        }
        restoreFocus = null
        invalidatePaint()
    }

    /** Switches to the next or previous menu while open. @since 1.0.0 */
    public fun switchMenu(delta: Int) {
        if (menusValue.isEmpty()) {
            return
        }
        val current = if (activeIndexValue >= 0) activeIndexValue else 0
        var next = current
        var attempts = 0
        do {
            next = (next + delta + menusValue.size) % menusValue.size
            attempts++
        } while (next == current && attempts <= menusValue.size)
        val target = restoreFocus
        popup?.close(restoreFocus = false)
        popup = null
        guiContext?.focus?.focus(target)
        restoreFocus = target
        open(next)
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        if (event.key == Key.F10) {
            return onGlobalKey(event)
        }
        return when (event.key) {
            Key.LEFT -> {
                if (activeIndexValue >= 0) {
                    switchMenu(-1)
                    true
                } else {
                    focusable && false
                }
            }

            Key.RIGHT -> {
                if (activeIndexValue >= 0) {
                    switchMenu(1)
                    true
                } else {
                    false
                }
            }

            Key.DOWN -> {
                if (activeIndexValue >= 0) {
                    popup?.focusFirstItem()
                    true
                } else {
                    open(firstMenuIndex())
                    true
                }
            }

            Key.UP -> {
                if (activeIndexValue >= 0) {
                    popup?.focusLastItem()
                    true
                } else {
                    open(lastMenuIndex())
                    true
                }
            }

            Key.ESCAPE -> {
                if (activeIndexValue >= 0) {
                    closeMenu(restore = true)
                    true
                } else {
                    false
                }
            }

            Key.ENTER, Key.KP_ENTER -> {
                if (activeIndexValue >= 0) {
                    popup?.activateFocused() ?: false
                } else {
                    false
                }
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
        val index = menuIndexAt(event.localX)
        if (index < 0) {
            return false
        }
        if (index == activeIndexValue) {
            closeMenu(restore = true)
        } else {
            open(index)
        }
        return true
    }

    private fun onPointerMove(event: GuiPointerMoveEvent): Boolean {
        if (activeIndexValue < 0) {
            return false
        }
        val index = menuIndexAt(event.localX)
        if (index < 0 || index == activeIndexValue) {
            return false
        }
        switchMenu(if (index > activeIndexValue) 1 else -1)
        return true
    }

    private fun menuIndexAt(localX: Int): Int {
        computeLayout()
        for (index in starts.indices) {
            if (localX >= starts[index] && localX < starts[index] + widths[index]) {
                return index
            }
        }
        return -1
    }

    private fun computeLayout() {
        starts = IntArray(menusValue.size)
        widths = IntArray(menusValue.size)
        var x = 0
        for (index in menusValue.indices) {
            val width = max(3, TextPainter.codePointCount(menusValue[index].title) + 2)
            starts[index] = x
            widths[index] = width
            x += width
        }
    }

    private fun firstMenuIndex(): Int = menusValue.indices.firstOrNull() ?: -1

    private fun lastMenuIndex(): Int = menusValue.indices.lastOrNull() ?: -1
}
