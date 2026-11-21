package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.EditorTextLayout
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.style.WidgetState
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Read-only log viewport with optional wrapping and follow-tail behavior.
 *
 * Content comes from [lines] or from a read-only [controller]. While
 * [followTail] is true, new content keeps the bottom row visible. Scrolling up
 * disables follow mode and paints a scroll-lock indicator; returning to the
 * bottom re-enables it. There is no caret and no editing.
 *
 * @property lines Log lines when [controller] is `null`.
 * @property wrap Wrapping policy.
 * @property followTail Whether new content sticks to the bottom.
 * @property controller Optional read-only controller source.
 * @property onFollowChanged Called when follow mode changes.
 * @since 1.0.0
 */
public class LogView(
    lines: List<String> = emptyList(),
    wrap: TextAreaWrap = TextAreaWrap.WORD,
    followTail: Boolean = true,
    controller: TextEditingController? = null,
    public var onFollowChanged: (Boolean) -> Unit = {},
) : Widget() {
    private var linesValue: List<String> = lines
    private var wrapValue: TextAreaWrap = wrap
    private var controllerValue: TextEditingController? = controller
    private var followTailValue: Boolean = followTail
    private var layout: EditorTextLayout? = null
    private var layoutWidth: Int = -1
    private val textListener: () -> Unit = { onControllerTextChanged() }

    /** Log lines. @since 1.0.0 */
    public var lines: List<String>
        get() = linesValue
        set(value) {
            linesValue = value
            contentChanged()
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

    /** Optional read-only controller source. @since 1.0.0 */
    public var controller: TextEditingController?
        get() = controllerValue
        set(value) {
            if (value === controllerValue) {
                return
            }
            if (isAttached) {
                controllerValue?.removeTextListener(textListener)
            }
            controllerValue = value
            if (isAttached) {
                value?.addTextListener(textListener)
            }
            contentChanged()
        }

    /** Whether new content keeps the bottom visible. @since 1.0.0 */
    public var followTail: Boolean
        get() = followTailValue
        set(value) {
            if (followTailValue == value) {
                return
            }
            followTailValue = value
            scroll.followEnd = value
            if (value) {
                scroll.end()
            }
            invalidatePaint()
            onFollowChanged(value)
        }

    /** Vertical and horizontal viewport state. @since 1.0.0 */
    public val scroll: ScrollModel = ScrollModel()

    /** Alias for [scroll]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = scroll

    /** Whether the view is detached from the tail. @since 1.0.0 */
    public val scrollLocked: Boolean
        get() = !followTailValue && scroll.maxOffsetY > 0

    /** Alias for [scrollLocked]. @since 1.0.0 */
    public val followLocked: Boolean get() = scrollLocked

    init {
        focusable = true
        scroll.followEnd = followTailValue
    }

    /** Appends one line. @since 1.0.0 */
    public fun append(line: String) {
        if (controllerValue != null) {
            return
        }
        linesValue = linesValue + line
        contentChanged()
    }

    /** Replaces all lines. @since 1.0.0 */
    public fun replaceLines(value: List<String>) {
        lines = value
    }

    /** Removes all lines. @since 1.0.0 */
    public fun clear() {
        replaceLines(emptyList())
    }

    override fun measure(constraints: Constraints): IntSize {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val measuredLayout = EditorTextLayout(contentText(), maxWidth, wrapValue.toTextWrap())
        layout = measuredLayout
        layoutWidth = maxWidth
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                measuredLayout.contentWidth
                    .coerceAtLeast(
                        1,
                    )
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
        scroll.viewportWidth = bounds.width.coerceAtLeast(0)
        scroll.viewportHeight = bounds.height.coerceAtLeast(0)
        if (followTailValue) {
            scroll.end()
        }
    }

    override fun onAttach() {
        controllerValue?.addTextListener(textListener)
        layout = null
        rebuildLayout()
    }

    override fun onDetach() {
        controllerValue?.removeTextListener(textListener)
    }

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        layout = null
        rebuildLayout()
        scroll.viewportWidth = new.width.coerceAtLeast(0)
        scroll.viewportHeight = new.height.coerceAtLeast(0)
        if (followTailValue) {
            scroll.end()
        }
    }

    override fun onFocusChanged(focused: Boolean) {
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
        val currentLayout = currentLayout()
        val offsetY = scroll.offsetY.coerceIn(0, (currentLayout.rows.size - bounds.height).coerceAtLeast(0))
        val offsetX = scroll.offsetX.coerceIn(0, (currentLayout.contentWidth - bounds.width).coerceAtLeast(0))
        var row = 0
        while (row < bounds.height) {
            val visual = offsetY + row
            if (visual >= currentLayout.rows.size) {
                break
            }
            val line = currentLayout.rows[visual]
            var column = 0
            while (column < line.cells.size) {
                val x = column - offsetX
                if (x >= 0 && x < bounds.width) {
                    context.canvas.set(x, row, line.cells[column].codePoint, style.fg, bg)
                }
                column++
            }
            row++
        }
        if (scrollLocked) {
            context.shape(Shape.SCROLL_DOWN, bounds.width - 1, bounds.height - 1, theme.warning, bg)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiScrollEvent -> onScroll(event)
            else -> false
        }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        val changed =
            when (event.key) {
                Key.UP -> scroll.scrollBy(0, -1)
                Key.DOWN -> scroll.scrollBy(0, 1)
                Key.PAGE_UP -> scroll.pageUp()
                Key.PAGE_DOWN -> scroll.pageDown()
                Key.HOME -> scroll.home()
                Key.END -> scroll.end()
                else -> false
            }
        if (changed) {
            syncFollow()
            invalidatePaint()
        }
        return changed
    }

    private fun onScroll(event: GuiScrollEvent): Boolean {
        val dx = event.dx.roundToInt()
        val dy = event.dy.roundToInt()
        if (dx == 0 && dy == 0) {
            return false
        }
        val changed = scroll.scrollBy(dx, dy)
        if (changed) {
            syncFollow()
            invalidatePaint()
        }
        return changed
    }

    private fun syncFollow() {
        val atBottom = scroll.maxOffsetY <= 0 || scroll.offsetY >= scroll.maxOffsetY
        if (followTailValue != atBottom) {
            followTailValue = atBottom
            scroll.followEnd = atBottom
            onFollowChanged(atBottom)
        }
    }

    private fun contentChanged() {
        if (scroll.maxOffsetY > 0 && scroll.offsetY >= scroll.maxOffsetY && !followTailValue) {
            followTailValue = true
            scroll.followEnd = true
            onFollowChanged(true)
        } else if (followTailValue && scroll.maxOffsetY > 0 && scroll.offsetY < scroll.maxOffsetY) {
            followTailValue = false
            scroll.followEnd = false
            onFollowChanged(false)
        }
        layout = null
        rebuildLayout()
        if (followTailValue) {
            scroll.followEnd = true
            scroll.end()
        }
        invalidateLayout()
        invalidatePaint()
    }

    private fun onControllerTextChanged() {
        contentChanged()
    }

    private fun rebuildLayout() {
        val width = bounds.width.coerceAtLeast(1)
        if (layout != null && layoutWidth == width) {
            return
        }
        layoutWidth = width
        layout = EditorTextLayout(contentText(), width, wrapValue.toTextWrap())
        scroll.contentWidth = layout!!.contentWidth
        scroll.contentHeight = layout!!.rows.size
    }

    private fun currentLayout(): EditorTextLayout {
        val existing = layout
        if (existing != null) {
            return existing
        }
        val width = bounds.width.coerceAtLeast(1)
        val created = EditorTextLayout(contentText(), width, wrapValue.toTextWrap())
        layout = created
        layoutWidth = width
        return created
    }

    private fun contentText(): String {
        val value = controllerValue
        if (value != null) {
            return value.text
        }
        if (linesValue.isEmpty()) {
            return ""
        }
        return linesValue.joinToString("\n")
    }
}
