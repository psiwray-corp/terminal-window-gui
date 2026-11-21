package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import com.psiwray.organization.terminal_window_gui.internal.ScrollThumb
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.roundToInt

/**
 * Track, thumb, and arrow controls for one [ScrollModel].
 *
 * The bar paints its whole local rectangle. A vertical bar is one column wide;
 * a horizontal bar is one row high. Arrow cells scroll one line, a track click
 * pages, and dragging the thumb maps cell movement to the model offset.
 *
 * @property model Scroll state controlled by this bar.
 * @property orientation Bar axis.
 * @property visibility When the bar is shown.
 * @since 1.0.0
 */
public class ScrollBar(
    public val model: ScrollModel = ScrollModel(),
    public val orientation: Orientation = Orientation.VERTICAL,
    public val visibility: ScrollBarVisibility = ScrollBarVisibility.AUTO,
) : Widget() {
    private var dragging: Boolean = false
    private var dragStartOffset: Int = 0
    private var dragStartCell: Int = 0

    /** Alias for [model]. @since 1.0.0 */
    public val scrollModel: ScrollModel get() = model

    /** Alias for [visibility]. @since 1.0.0 */
    public val scrollBarVisibility: ScrollBarVisibility get() = visibility

    /** Whether the current content/viewport pair hides this bar. @since 1.0.0 */
    public val hidden: Boolean
        get() =
            when (visibility) {
                ScrollBarVisibility.NEVER -> true
                ScrollBarVisibility.ALWAYS -> false
                ScrollBarVisibility.AUTO -> contentFits()
            }

    /** Whether the bar is shown. @since 1.0.0 */
    public val shown: Boolean get() = !hidden

    init {
        focusable = true
    }

    override fun measure(constraints: Constraints): IntSize {
        if (hidden) {
            return constraints.constrain(IntSize.ZERO)
        }
        val width =
            if (orientation == Orientation.VERTICAL) {
                1
            } else if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                model.viewportWidth.coerceAtLeast(1)
            }
        val height =
            if (orientation == Orientation.HORIZONTAL) {
                1
            } else if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                model.viewportHeight.coerceAtLeast(1)
            }
        return constraints.constrain(IntSize(width, height))
    }

    override fun paint(context: PaintContext) {
        if (hidden || bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val active = isFocused
        val border = if (active) theme.borderFocused else theme.border
        val trackShape = if (orientation == Orientation.VERTICAL) Shape.SCROLL_V_TRACK else Shape.SCROLL_H_TRACK
        val thumbShape = if (orientation == Orientation.VERTICAL) Shape.SCROLL_V_THUMB else Shape.SCROLL_H_THUMB
        val arrowBack = if (orientation == Orientation.VERTICAL) Shape.SCROLL_UP else Shape.SCROLL_LEFT
        val arrowForward = if (orientation == Orientation.VERTICAL) Shape.SCROLL_DOWN else Shape.SCROLL_RIGHT
        val length = trackLength()
        val start = trackStart()
        var index = 0
        while (index < length) {
            if (orientation == Orientation.VERTICAL) {
                context.shape(trackShape, 0, start + index, border, theme.surface)
            } else {
                context.shape(trackShape, start + index, 0, border, theme.surface)
            }
            index++
        }
        if (length > 0) {
            val thumb = ScrollThumb.length(viewport(), content(), length)
            val position =
                ScrollThumb.position(
                    offset(),
                    maxOffset(),
                    length,
                    thumb,
                )
            var thumbIndex = 0
            while (thumbIndex < thumb) {
                if (orientation == Orientation.VERTICAL) {
                    context.shape(thumbShape, 0, start + position + thumbIndex, theme.accent, theme.surface)
                } else {
                    context.shape(thumbShape, start + position + thumbIndex, 0, theme.accent, theme.surface)
                }
                thumbIndex++
            }
        }
        if (orientation == Orientation.VERTICAL) {
            if (bounds.height >= 2) {
                context.shape(arrowBack, 0, 0, border, theme.surface)
                context.shape(arrowForward, 0, bounds.height - 1, border, theme.surface)
            }
        } else {
            if (bounds.width >= 2) {
                context.shape(arrowBack, 0, 0, border, theme.surface)
                context.shape(arrowForward, bounds.width - 1, 0, border, theme.surface)
            }
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> onKey(event)
            is GuiPointerButtonEvent -> onButton(event)
            is GuiPointerMoveEvent -> onMove(event)
            is GuiScrollEvent -> onScroll(event)
            else -> false
        }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (event.action == KeyAction.RELEASE || hidden) {
            return false
        }
        return when (event.key) {
            Key.UP, Key.LEFT -> {
                when {
                    orientation == Orientation.VERTICAL && event.key == Key.UP -> scroll(0, -1)
                    orientation == Orientation.HORIZONTAL && event.key == Key.LEFT -> scroll(-1, 0)
                    else -> false
                }
            }

            Key.DOWN, Key.RIGHT -> {
                when {
                    orientation == Orientation.VERTICAL && event.key == Key.DOWN -> scroll(0, 1)
                    orientation == Orientation.HORIZONTAL && event.key == Key.RIGHT -> scroll(1, 0)
                    else -> false
                }
            }

            Key.PAGE_UP -> {
                page(-1)
            }

            Key.PAGE_DOWN -> {
                page(1)
            }

            Key.HOME -> {
                model.home().also { if (it) invalidatePaint() }
            }

            Key.END -> {
                model.end().also { if (it) invalidatePaint() }
            }

            else -> {
                false
            }
        }
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (hidden || event.action == KeyAction.RELEASE) {
            if (dragging) {
                dragging = false
                return true
            }
            return false
        }
        if (event.action != KeyAction.PRESS) {
            return false
        }
        val cell = if (orientation == Orientation.VERTICAL) event.localY else event.localX
        val total = if (orientation == Orientation.VERTICAL) bounds.height else bounds.width
        if (cell == 0) {
            return scroll(
                if (orientation ==
                    Orientation.VERTICAL
                ) {
                    0
                } else {
                    -1
                },
                if (orientation == Orientation.VERTICAL) -1 else 0,
            )
        }
        if (cell == total - 1) {
            return scroll(
                if (orientation ==
                    Orientation.VERTICAL
                ) {
                    0
                } else {
                    1
                },
                if (orientation == Orientation.VERTICAL) 1 else 0,
            )
        }
        val length = trackLength()
        val start = trackStart()
        if (length <= 0) {
            return false
        }
        val thumb = ScrollThumb.length(viewport(), content(), length)
        val thumbPosition = ScrollThumb.position(offset(), maxOffset(), length, thumb)
        val trackCell = cell - start
        if (trackCell in thumbPosition until (thumbPosition + thumb)) {
            dragging = true
            dragStartOffset = offset()
            dragStartCell = cell
            return true
        }
        return page(if (trackCell < thumbPosition) -1 else 1)
    }

    private fun onMove(event: GuiPointerMoveEvent): Boolean {
        if (!dragging) {
            return false
        }
        val length = trackLength()
        val thumb = ScrollThumb.length(viewport(), content(), length)
        val cell = if (orientation == Orientation.VERTICAL) event.localY else event.localX
        val position = ScrollThumb.position(dragStartOffset, maxOffset(), length, thumb) + (cell - dragStartCell)
        val value = ScrollThumb.offsetForPosition(position, maxOffset(), length, thumb)
        val changed =
            if (orientation == Orientation.VERTICAL) {
                model.scrollTo(model.offsetX, value)
            } else {
                model.scrollTo(value, model.offsetY)
            }
        if (changed) {
            invalidatePaint()
        }
        return true
    }

    private fun onScroll(event: GuiScrollEvent): Boolean {
        if (hidden) {
            return false
        }
        val delta =
            if (orientation == Orientation.VERTICAL) {
                event.dy.roundToInt()
            } else {
                event.dx.roundToInt()
            }
        if (delta == 0) {
            return false
        }
        return scroll(
            if (orientation ==
                Orientation.VERTICAL
            ) {
                0
            } else {
                delta
            },
            if (orientation == Orientation.VERTICAL) delta else 0,
        )
    }

    private fun scroll(
        dx: Int,
        dy: Int,
    ): Boolean {
        val changed = model.scrollBy(dx, dy)
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun page(direction: Int): Boolean {
        val changed = if (direction < 0) model.pageUp() else model.pageDown()
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun changed(changed: Boolean): Boolean {
        if (changed) {
            invalidatePaint()
        }
        return changed
    }

    private fun contentFits(): Boolean {
        val viewport = viewport()
        return viewport <= 0 || content() <= viewport
    }

    private fun viewport(): Int = if (orientation == Orientation.VERTICAL) model.viewportHeight else model.viewportWidth

    private fun content(): Int = if (orientation == Orientation.VERTICAL) model.contentHeight else model.contentWidth

    private fun offset(): Int = if (orientation == Orientation.VERTICAL) model.offsetY else model.offsetX

    private fun maxOffset(): Int = if (orientation == Orientation.VERTICAL) model.maxOffsetY else model.maxOffsetX

    private fun trackStart(): Int = if (bounds.height >= 2 || bounds.width >= 2) 1 else 0

    private fun trackLength(): Int {
        val total = if (orientation == Orientation.VERTICAL) bounds.height else bounds.width
        return (total - 2).coerceAtLeast(0)
    }
}
