package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Tab bar plus the active tab's content.
 *
 * The bar occupies row zero and the content fills the remaining rows. Inactive
 * content is detached from the tree: it is never measured, painted, hit, or
 * focused. Switching remembers the focus inside each tab's content and restores
 * it when the tab becomes active again.
 *
 * @property tabs Tab definitions in display order.
 * @property activeIndex Active tab index, clamped to [tabs].
 * @property onChanged Called with the new index after a user activation.
 * @since 1.0.0
 */
public class TabView(
    tabs: List<Tab> = emptyList(),
    activeIndex: Int = 0,
    public var onChanged: (Int) -> Unit = {},
) : ContainerWidget() {
    private var tabsValue: List<Tab> = tabs.toList()
    private var activeIndexValue: Int = normalize(activeIndex)
    private var activeContentValue: Widget? = null
    private val savedFocus: MutableMap<Any, Widget> = LinkedHashMap()

    /** Tab bar child. @since 1.0.0 */
    public val tabBar: TabBar = TabBar(tabsValue, activeIndexValue)

    /** Today's tab definitions. @since 1.0.0 */
    public var tabs: List<Tab>
        get() = tabsValue
        set(value) {
            if (tabsValue === value) {
                return
            }
            val oldTab = activeTab
            val oldContent = activeContentValue
            val oldIndex = activeIndexValue
            tabsValue = value.toList()
            tabBar.tabs = tabsValue
            activeIndexValue = normalize(oldIndex)
            switchContent(
                nextIndex = activeIndexValue,
                nextTab = tabsValue.getOrNull(activeIndexValue),
                oldTab = oldTab,
                oldContent = oldContent,
                notify = false,
            )
            invalidateLayout()
            invalidatePaint()
        }

    /** Active tab index. Assigning outside the list clamps. @since 1.0.0 */
    public var activeIndex: Int
        get() = activeIndexValue
        set(value) {
            switchTo(value, notify = false)
        }

    /** Active tab, or `null`. @since 1.0.0 */
    public val activeTab: Tab? get() = tabsValue.getOrNull(activeIndexValue)

    /** Active content widget, or `null`. @since 1.0.0 */
    public val content: Widget? get() = activeContentValue

    init {
        adopt(tabBar)
        tabBar.onChanged = { index -> switchTo(index, notify = true) }
        switchTo(activeIndexValue, notify = false)
    }

    override fun measure(constraints: Constraints): IntSize {
        val contentWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val contentHeight =
            if (constraints.hasBoundedHeight) {
                (constraints.maxHeight - 1).coerceAtLeast(0)
            } else {
                Int.MAX_VALUE
            }
        val contentSize =
            activeContentValue?.measured(
                Constraints(0, contentWidth, 0, contentHeight),
            )
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                contentSize?.width ?: 1
            }
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                ((contentSize?.height ?: 0) + 1).coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val width = contentRect.width
        val height = contentRect.height
        tabBar.arrange(Rect(0, 0, width, if (height >= 1) 1 else 0))
        arrangeActiveContent(width, height)
    }

    override fun paint(context: PaintContext) {}

    override fun onEvent(event: GuiEvent): Boolean = handleKey(event)

    override fun onCaptureEvent(event: GuiEvent): Boolean = handleKey(event)

    private fun handleKey(event: GuiEvent): Boolean {
        if (event !is GuiKeyEvent ||
            event.action == KeyAction.RELEASE ||
            event.repeat ||
            event.key != Key.TAB ||
            !event.modifiers.has(Modifiers.CONTROL) ||
            event.modifiers.has(Modifiers.ALT) ||
            event.modifiers.has(Modifiers.SUPER)
        ) {
            return false
        }
        val direction = if (event.modifiers.has(Modifiers.SHIFT)) -1 else 1
        val next = nextEnabled(activeIndexValue, direction) ?: return false
        switchTo(next, notify = true)
        return true
    }

    private fun switchTo(
        index: Int,
        notify: Boolean,
    ) {
        val oldTab = activeTab
        val oldContent = activeContentValue
        val nextIndex = normalize(index)
        if (notify && nextIndex == activeIndexValue && tabsValue.getOrNull(nextIndex) === oldTab) {
            onChanged(nextIndex)
            return
        }
        switchContent(
            nextIndex = nextIndex,
            nextTab = tabsValue.getOrNull(nextIndex),
            oldTab = oldTab,
            oldContent = oldContent,
            notify = notify,
        )
    }

    private fun switchContent(
        nextIndex: Int,
        nextTab: Tab?,
        oldTab: Tab?,
        oldContent: Widget?,
        notify: Boolean,
    ) {
        val nextContent = nextTab?.content
        if (oldTab === nextTab && oldContent === nextContent && nextIndex == activeIndexValue) {
            return
        }
        val manager = guiContext?.focus
        val focusedNow = manager?.focused
        val oldFocus =
            if (oldContent != null && focusedNow != null &&
                (focusedNow === oldContent || oldContent.isAncestorOf(focusedNow))
            ) {
                focusedNow
            } else {
                manager?.previousFocused?.takeIf {
                    oldContent != null && (it === oldContent || oldContent.isAncestorOf(it))
                }
            }
        val oldFocusWasContent =
            oldContent != null &&
                oldFocus != null &&
                (oldFocus === oldContent || oldContent.isAncestorOf(oldFocus))
        if (oldFocusWasContent) {
            savedFocus[oldTab!!.key] = oldFocus
            manager?.clearFocus()
        }
        activeIndexValue = nextIndex
        tabBar.activeIndex = nextIndex
        if (oldContent !== nextContent) {
            if (oldContent != null) {
                oldContent.visible = false
                drop(oldContent)
            }
            activeContentValue = nextContent
            if (nextContent != null) {
                nextContent.visible = true
                adopt(nextContent, 1)
            }
        }
        arrangeActiveContent(contentRect.width, contentRect.height)
        if (manager != null) {
            val saved = nextTab?.key?.let(savedFocus::get)
            val restored = saved != null && manager.restore(saved)
            if (!restored && oldFocusWasContent && tabBar.visible && !tabBar.bounds.isEmpty) {
                manager.focus(tabBar)
            }
        }
        invalidateLayout()
        invalidatePaint()
        if (notify) {
            onChanged(nextIndex)
        }
    }

    private fun arrangeActiveContent(
        width: Int,
        height: Int,
    ) {
        val current = activeContentValue ?: return
        if (width <= 0 || height <= 1) {
            current.arrange(Rect.EMPTY)
            return
        }
        current.visible = true
        current.arrange(Rect(0, 1, width, height - 1))
    }

    private fun nextEnabled(
        from: Int,
        direction: Int,
    ): Int? {
        if (tabsValue.isEmpty()) {
            return null
        }
        var index = from
        var attempts = 0
        while (attempts < tabsValue.size) {
            index = (index + direction + tabsValue.size) % tabsValue.size
            if (tabsValue[index].enabled) {
                return index
            }
            attempts++
        }
        return null
    }

    private fun normalize(index: Int): Int {
        if (tabsValue.isEmpty()) {
            return 0
        }
        return index.coerceIn(0, tabsValue.lastIndex)
    }
}
