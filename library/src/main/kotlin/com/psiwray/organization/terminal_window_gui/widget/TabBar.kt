package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.roundToInt

/**
 * One-row horizontal tab strip with an overflow viewport.
 *
 * Tabs are fixed title cells plus one marker cell on each side. The active tab
 * is drawn with `>` and `<`, a selected background, and is always scrolled into
 * view. Inactive tabs are muted. When the strip overflows, the left and right
 * edges show [Shape.SCROLL_LEFT] and [Shape.SCROLL_RIGHT] and scroll one cell
 * per click or wheel step.
 *
 * Left/Right move to the previous/next enabled tab and activate it. Enter and
 * Space activate the current tab. Ctrl+Tab and Ctrl+Shift+Tab cycle; Home and
 * End jump to the first/last enabled tab. Only the active tab is selected;
 * disabled tabs are skipped by keyboard navigation and cannot be clicked.
 *
 * @property tabs Tab definitions in display order.
 * @property activeIndex Index of the active tab; clamped to the tab list.
 * @property onChanged Called with the new index after a user activation.
 * @since 1.0.0
 */
public class TabBar(
    tabs: List<Tab> = emptyList(),
    activeIndex: Int = 0,
    public var onChanged: (Int) -> Unit = {},
) : Widget() {
    private var tabsValue: List<Tab> = tabs.toList()
    private var activeIndexValue: Int = normalize(activeIndex)
    private var segmentsCache: List<Segment> = emptyList()

    /** Horizontal overflow state for the tab strip. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    /** Tab definitions in display order. @since 1.0.0 */
    public var tabs: List<Tab>
        get() = tabsValue
        set(value) {
            if (tabsValue === value) {
                return
            }
            tabsValue = value.toList()
            activeIndexValue = normalize(activeIndexValue)
            updateGeometry()
            invalidateLayout()
            invalidatePaint()
        }

    /** Index of the active tab. Assigning outside the list clamps. @since 1.0.0 */
    public var activeIndex: Int
        get() = activeIndexValue
        set(value) {
            val next = normalize(value)
            if (activeIndexValue == next) {
                return
            }
            activeIndexValue = next
            ensureActiveVisible()
            invalidatePaint()
        }

    /** Active tab, or `null` when the bar has no tabs. @since 1.0.0 */
    public val activeTab: Tab? get() = tabsValue.getOrNull(activeIndexValue)

    init {
        focusable = true
        cursorShape = com.psiwray.organization.terminal_window.CursorShape.HAND
    }

    override fun measure(constraints: Constraints): IntSize {
        val contentWidth = contentWidth()
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                contentWidth
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), 1))
    }

    override fun arrangeChildren() {
        updateGeometry()
        ensureActiveVisible()
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, 1), theme.textMuted, theme.surfaceAlt)
        updateGeometry()
        val segments = segmentsCache
        context.pushClip(Rect(0, 0, bounds.width, 1))
        try {
            var index = 0
            while (index < segments.size) {
                val segment = segments[index]
                val x = segment.start - scroll.offsetX
                if (x < bounds.width && x + segment.width > 0) {
                    paintSegment(context, segment, x)
                }
                index++
            }
        } finally {
            context.popClip()
        }
        if (scroll.maxOffsetX > 0) {
            val fg = if (isFocused) theme.borderFocused else theme.border
            if (scroll.offsetX > 0) {
                context.shape(Shape.SCROLL_LEFT, 0, 0, fg, theme.surfaceAlt)
            }
            if (scroll.offsetX < scroll.maxOffsetX && bounds.width > 1) {
                context.shape(Shape.SCROLL_RIGHT, bounds.width - 1, 0, fg, theme.surfaceAlt)
            }
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        if (!enabled) {
            return false
        }
        return when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onButton(event)
            is GuiScrollEvent -> onScrollEvent(event)
            else -> false
        }
    }

    override fun onFocusChanged(focused: Boolean) {
        invalidatePaint()
    }

    override fun onHoverChanged(hovered: Boolean) {
        invalidatePaint()
    }

    /**
     * Activates [index] when it names an enabled tab.
     *
     * @return whether the active tab changed.
     * @since 1.0.0
     */
    public fun activate(index: Int): Boolean {
        val tab = tabsValue.getOrNull(index) ?: return false
        if (!tab.enabled || index == activeIndexValue) {
            return false
        }
        activeIndexValue = index
        ensureActiveVisible()
        invalidatePaint()
        onChanged(index)
        return true
    }

    private fun paintSegment(
        context: PaintContext,
        segment: Segment,
        x: Int,
    ) {
        val theme = context.theme
        val tab = segment.tab
        val active = segment.index == activeIndexValue
        val fg =
            when {
                !tab.enabled -> theme.textDisabled
                active -> theme.selectionForeground
                else -> theme.textMuted
            }
        val bg = if (active) theme.selectionBackground else theme.surfaceAlt
        context.fill(Rect(x, 0, segment.width, 1), fg, bg)
        if (active) {
            val markerFg = if (isFocused) theme.borderFocused else fg
            context.text(x, 0, ">", markerFg, bg)
            context.text(x + segment.width - 1, 0, "<", markerFg, bg)
        }
        val title = tab.title.replace('\n', ' ')
        val titleWidth = segment.width - 2
        if (titleWidth > 0) {
            TextPainter.draw(
                context = context,
                line = title,
                x = x + 1,
                y = 0,
                width = titleWidth,
                alignment = TextAlignment.LEFT,
                fg = fg,
                bg = bg,
                ellipsis = TextPainter.codePointCount(title) > titleWidth,
            )
        }
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (!isFocused || event.action == KeyAction.RELEASE) {
            return false
        }
        val control = event.modifiers.has(Modifiers.CONTROL)
        val shift = event.modifiers.has(Modifiers.SHIFT)
        if (event.key == Key.TAB && control) {
            return move(if (shift) -1 else 1)
        }
        return when (event.key) {
            Key.LEFT -> {
                move(-1)
            }

            Key.RIGHT -> {
                move(1)
            }

            Key.HOME -> {
                moveToFirst()
            }

            Key.END -> {
                moveToLast()
            }

            Key.ENTER, Key.KP_ENTER, Key.SPACE -> {
                if (event.repeat) {
                    false
                } else {
                    onChanged(activeIndexValue)
                    true
                }
            }

            else -> {
                false
            }
        }
    }

    private fun move(delta: Int): Boolean {
        if (tabsValue.isEmpty()) {
            return false
        }
        var index = activeIndexValue
        var attempts = 0
        while (attempts < tabsValue.size) {
            index = (index + delta + tabsValue.size) % tabsValue.size
            if (tabsValue[index].enabled) {
                return activate(index) || index == activeIndexValue
            }
            attempts++
        }
        return false
    }

    private fun moveToFirst(): Boolean {
        val index = tabsValue.indexOfFirst { it.enabled }
        return index >= 0 && (activate(index) || index == activeIndexValue)
    }

    private fun moveToLast(): Boolean {
        val index = tabsValue.indexOfLast { it.enabled }
        return index >= 0 && (activate(index) || index == activeIndexValue)
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT || event.action != KeyAction.PRESS) {
            return false
        }
        if (event.localY != 0 || event.localX !in 0 until bounds.width) {
            return false
        }
        if (scroll.maxOffsetX > 0 && event.localX == 0) {
            return scrollStrip(-1)
        }
        if (scroll.maxOffsetX > 0 && event.localX == bounds.width - 1) {
            return scrollStrip(1)
        }
        val contentX = event.localX + scroll.offsetX
        val index = segmentsCache.indexOfFirst { contentX >= it.start && contentX < it.start + it.width }
        if (index < 0) {
            return false
        }
        activate(index)
        return true
    }

    private fun onScrollEvent(event: GuiScrollEvent): Boolean {
        if (scroll.maxOffsetX <= 0) {
            return false
        }
        val delta = if (event.dx != 0.0) event.dx.roundToInt() else event.dy.roundToInt()
        return scrollStrip(delta)
    }

    private fun scrollStrip(delta: Int): Boolean {
        if (delta == 0) {
            return false
        }
        val changed = scroll.scrollBy(delta, 0)
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun ensureActiveVisible() {
        val segment = segmentsCache.getOrNull(activeIndexValue) ?: return
        if (bounds.isEmpty) {
            return
        }
        scroll.viewportWidth = bounds.width
        scroll.contentWidth = contentWidth()
        scroll.ensureVisible(segment.start, 0, segment.width, 1)
    }

    private fun updateGeometry() {
        if (segmentsCache.size != tabsValue.size ||
            segmentsCache.indexOfFirst { it.tab !== tabsValue[it.index] } >= 0
        ) {
            rebuildSegments()
        } else {
            var index = 0
            while (index < tabsValue.size) {
                if (segmentsCache[index].title != tabsValue[index].title) {
                    rebuildSegments()
                    break
                }
                index++
            }
        }
        if (bounds.isEmpty) {
            scroll.viewportWidth = 0
        } else {
            scroll.viewportWidth = bounds.width
        }
        scroll.contentWidth = contentWidth()
    }

    private fun rebuildSegments() {
        val result = ArrayList<Segment>(tabsValue.size)
        var start = 0
        var index = 0
        while (index < tabsValue.size) {
            val tab = tabsValue[index]
            val width = TextPainter.codePointCount(tab.title) + 2
            result.add(Segment(index, start, width, tab, tab.title))
            start += width
            index++
        }
        segmentsCache = result
    }

    private fun contentWidth(): Int {
        updateSegmentsIfNeeded()
        var width = 0
        for (segment in segmentsCache) {
            width += segment.width
        }
        return width
    }

    private fun updateSegmentsIfNeeded() {
        if (segmentsCache.size != tabsValue.size) {
            rebuildSegments()
            return
        }
        var index = 0
        while (index < tabsValue.size) {
            if (segmentsCache[index].tab !== tabsValue[index] ||
                segmentsCache[index].title != tabsValue[index].title
            ) {
                rebuildSegments()
                return
            }
            index++
        }
    }

    private fun normalize(index: Int): Int {
        if (tabsValue.isEmpty()) {
            return 0
        }
        return index.coerceIn(0, tabsValue.lastIndex)
    }

    private data class Segment(
        val index: Int,
        val start: Int,
        val width: Int,
        val tab: Tab,
        val title: String,
    )
}
