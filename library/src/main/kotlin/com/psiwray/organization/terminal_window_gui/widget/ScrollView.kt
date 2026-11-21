package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Overflow
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.roundToInt

/**
 * Clipped viewport over one scrollable child.
 *
 * The child is measured with an unbounded scroll axis and the viewport cross
 * axis. Painting and hit testing shift by [ScrollModel.offsetX] and
 * [ScrollModel.offsetY]; child bounds never move.
 *
 * @property model Shared two-axis scroll state.
 * @property orientation Primary scroll axis.
 * @since 1.0.0
 */
public class ScrollView(
    public val model: ScrollModel = ScrollModel(),
    public val orientation: Orientation = Orientation.VERTICAL,
    child: Widget? = null,
) : SingleChildWidget(child, Overflow.CLIP) {
    /** Child-first constructor. @since 1.0.0 */
    public constructor(child: Widget) : this(ScrollModel(), Orientation.VERTICAL, child)

    init {
        focusable = true
        tabStop = false
    }

    override fun measure(constraints: Constraints): IntSize {
        val current = child
        if (current == null) {
            return constraints.constrain(IntSize.ZERO)
        }
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val childConstraints =
            if (orientation == Orientation.VERTICAL) {
                Constraints(0, maxWidth, 0, Int.MAX_VALUE)
            } else {
                Constraints(0, Int.MAX_VALUE, 0, maxHeight)
            }
        val childSize = current.measured(childConstraints)
        model.contentWidth = childSize.width
        model.contentHeight = childSize.height
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else childSize.width.coerceAtLeast(1)
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else childSize.height.coerceAtLeast(1)
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        model.viewportWidth = content.width
        model.viewportHeight = content.height
        val childConstraints =
            if (orientation == Orientation.VERTICAL) {
                Constraints(0, content.width, 0, Int.MAX_VALUE)
            } else {
                Constraints(0, Int.MAX_VALUE, 0, content.height)
            }
        val size = current.measured(childConstraints)
        current.arrange(Rect(0, 0, size.width, size.height))
        model.contentWidth = size.width
        model.contentHeight = size.height
    }

    override fun paint(context: PaintContext) {}

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiScrollEvent -> onScroll(event)
            else -> false
        }

    override fun contentScrollOffset(): IntSize = IntSize(model.offsetX, model.offsetY)

    internal fun reveal(widget: Widget): Boolean {
        val content = contentRect
        val scroll = contentScrollOffset()
        val origin = screenOriginPoint()
        val child = widget.screenOriginPoint()
        val x = child.x - (origin.x + content.x - bounds.x - scroll.width)
        val y = child.y - (origin.y + content.y - bounds.y - scroll.height)
        val changed = model.ensureVisible(x, y, widget.bounds.width, widget.bounds.height)
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        return when (event.key) {
            Key.UP -> scroll(0, -1)
            Key.DOWN -> scroll(0, 1)
            Key.LEFT -> scroll(-1, 0)
            Key.RIGHT -> scroll(1, 0)
            Key.PAGE_UP -> page(-1)
            Key.PAGE_DOWN -> page(1)
            Key.HOME -> changed(model.home())
            Key.END -> changed(model.end())
            else -> false
        }
    }

    private fun onScroll(event: GuiScrollEvent): Boolean {
        val vertical = if (orientation == Orientation.VERTICAL) event.dy else event.dx
        val delta = vertical.roundToInt()
        if (delta == 0) {
            return false
        }
        return if (orientation == Orientation.VERTICAL) {
            scroll(0, delta)
        } else {
            scroll(delta, 0)
        }
    }

    private fun page(direction: Int): Boolean {
        val didChange = if (direction < 0) model.pageUp() else model.pageDown()
        return changed(didChange)
    }

    private fun scroll(
        dx: Int,
        dy: Int,
    ): Boolean = changed(model.scrollBy(dx, dy))

    private fun changed(changed: Boolean): Boolean {
        if (changed) {
            invalidatePaint()
        }
        return changed
    }
}
