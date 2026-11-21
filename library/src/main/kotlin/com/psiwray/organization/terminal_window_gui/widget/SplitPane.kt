package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.internal.BoxPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.roundToInt

/**
 * Two panes separated by a draggable one-cell divider.
 *
 * The divider is a hint, not a constraint: every allocation is clamped against
 * both minima. If the available axis cannot hold both minima plus the divider,
 * the divider is hidden and the axis is divided proportionally with no
 * negative panes. A resize preserves [ratio] and re-clamps the allocation.
 *
 * The divider is focusable when visible. Arrow keys move it one cell, Home and
 * End clamp it, and a double click resets [ratio] to `0.5`. Dragging captures
 * the pointer and updates the ratio from the pointer cell.
 *
 * @property orientation Pane axis and divider direction.
 * @property ratio Requested share of the space before the divider.
 * @property minFirst Minimum cells reserved for the first pane.
 * @property minSecond Minimum cells reserved for the second pane.
 * @since 1.0.0
 */
public class SplitPane(
    orientation: SplitOrientation,
    first: Widget,
    second: Widget,
    ratio: Double = 0.5,
    minFirst: Int = 1,
    minSecond: Int = 1,
) : ContainerWidget() {
    private var orientationValue: SplitOrientation = orientation
    private var firstValue: Widget = first
    private var secondValue: Widget = second
    private var ratioValue: Double = ratio.coerceIn(0.0, 1.0)
    private var minFirstValue: Int = minFirst
    private var minSecondValue: Int = minSecond
    private var dividerVisibleValue: Boolean = false
    private var dividerPositionValue: Int = 0
    private var firstExtentValue: Int = 0
    private var secondExtentValue: Int = 0
    private var dragging: Boolean = false

    /** Pane axis and divider direction. @since 1.0.0 */
    public var orientation: SplitOrientation
        get() = orientationValue
        set(value) {
            if (orientationValue == value) {
                return
            }
            orientationValue = value
            invalidateLayout()
            invalidatePaint()
        }

    /** Pane children in paint order. @since 1.0.0 */
    public val children: List<Widget> get() = java.util.Collections.unmodifiableList(childList)

    /** First pane. @since 1.0.0 */
    public var first: Widget
        get() = firstValue
        set(value) {
            if (firstValue === value) {
                return
            }
            drop(firstValue)
            firstValue = value
            adopt(value, 0)
            invalidateLayout()
        }

    /** Second pane. @since 1.0.0 */
    public var second: Widget
        get() = secondValue
        set(value) {
            if (secondValue === value) {
                return
            }
            drop(secondValue)
            secondValue = value
            adopt(value)
            invalidateLayout()
        }

    /** Alias for [ratio]. @since 1.0.0 */
    public var splitRatio: Double
        get() = ratio
        set(value) {
            ratio = value
        }

    /** Desired split ratio in `[0, 1]`. @since 1.0.0 */
    public var ratio: Double
        get() = ratioValue
        set(value) {
            val next = value.coerceIn(0.0, 1.0)
            if (next == ratioValue) {
                return
            }
            ratioValue = next
            invalidateLayout()
            invalidatePaint()
        }

    /** Minimum cells for the first pane. @since 1.0.0 */
    public var minFirst: Int
        get() = minFirstValue
        set(value) {
            require(value >= 0) { "minFirst must be non-negative, got $value" }
            if (minFirstValue == value) {
                return
            }
            minFirstValue = value
            invalidateLayout()
        }

    /** Minimum cells for the second pane. @since 1.0.0 */
    public var minSecond: Int
        get() = minSecondValue
        set(value) {
            require(value >= 0) { "minSecond must be non-negative, got $value" }
            if (minSecondValue == value) {
                return
            }
            minSecondValue = value
            invalidateLayout()
        }

    /** Whether the divider is visible in the current allocation. @since 1.0.0 */
    public val dividerVisible: Boolean get() = dividerVisibleValue

    /** Divider cell on the split axis in local coordinates. @since 1.0.0 */
    public val dividerPosition: Int get() = dividerPositionValue

    /** First pane extent on the split axis. @since 1.0.0 */
    public val firstExtent: Int get() = firstExtentValue

    /** Second pane extent on the split axis. @since 1.0.0 */
    public val secondExtent: Int get() = secondExtentValue

    /** Whether the divider participates in focus traversal. @since 1.0.0 */
    public var dividerFocusable: Boolean = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            focusable = value && dividerVisibleValue
            invalidatePaint()
        }

    /** Child-first constructor with side-by-side panes. @since 1.0.0 */
    public constructor(
        first: Widget,
        second: Widget,
        ratio: Double = 0.5,
        minFirst: Int = 1,
        minSecond: Int = 1,
    ) : this(SplitOrientation.HORIZONTAL, first, second, ratio, minFirst, minSecond)

    init {
        require(minFirst >= 0) { "minFirst must be non-negative, got $minFirst" }
        require(minSecond >= 0) { "minSecond must be non-negative, got $minSecond" }
        adopt(first)
        adopt(second)
        focusable = true
    }

    override fun measure(constraints: Constraints): IntSize {
        val loose = constraints.loosen()
        val firstSize = firstValue.measured(loose)
        val secondSize = secondValue.measured(loose)
        val preferred =
            if (orientation == SplitOrientation.HORIZONTAL) {
                IntSize(
                    firstSize.width + secondSize.width + 1,
                    maxOf(firstSize.height, secondSize.height).coerceAtLeast(1),
                )
            } else {
                IntSize(
                    maxOf(firstSize.width, secondSize.width).coerceAtLeast(1),
                    firstSize.height + secondSize.height + 1,
                )
            }
        return constraints.constrain(preferred)
    }

    override fun arrangeChildren() {
        val content = contentRect
        val total =
            if (orientation == SplitOrientation.HORIZONTAL) {
                content.width
            } else {
                content.height
            }
        val allocation = allocate(total)
        dividerVisibleValue = allocation.divider
        dividerPositionValue = allocation.first
        firstExtentValue = allocation.first
        secondExtentValue = allocation.second
        focusable = dividerFocusable && allocation.divider
        if (orientation == SplitOrientation.HORIZONTAL) {
            firstValue.arrange(Rect(0, 0, allocation.first, content.height))
            secondValue.arrange(
                Rect(
                    allocation.first + if (allocation.divider) 1 else 0,
                    0,
                    allocation.second,
                    content.height,
                ),
            )
        } else {
            firstValue.arrange(Rect(0, 0, content.width, allocation.first))
            secondValue.arrange(
                Rect(
                    0,
                    allocation.first + if (allocation.divider) 1 else 0,
                    content.width,
                    allocation.second,
                ),
            )
        }
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, bounds.height), theme.text, theme.surface)
        if (!dividerVisibleValue) {
            return
        }
        val fg = if (isFocused) theme.borderFocused else theme.border
        val bg = theme.surface
        if (orientation == SplitOrientation.HORIZONTAL) {
            var y = 0
            while (y < bounds.height) {
                context.shape(BoxPainter.vertical(BoxStyle.SINGLE), dividerPositionValue, y, fg, bg)
                y++
            }
        } else {
            var x = 0
            while (x < bounds.width) {
                context.shape(BoxPainter.horizontal(BoxStyle.SINGLE), x, dividerPositionValue, fg, bg)
                x++
            }
        }
    }

    override fun onEvent(event: GuiEvent): Boolean =
        when (event) {
            is GuiPointerButtonEvent -> onButton(event)
            is GuiPointerMoveEvent -> onMove(event)
            is GuiKeyEvent -> onKey(event)
            else -> false
        }

    override fun onFocusChanged(focused: Boolean) {
        invalidatePaint()
    }

    override fun onHoverChanged(hovered: Boolean) {
        invalidatePaint()
    }

    private fun onButton(event: GuiPointerButtonEvent): Boolean {
        if (event.button != MouseButton.LEFT) {
            return false
        }
        return when (event.action) {
            KeyAction.PRESS -> {
                if (!dividerVisibleValue || !onDivider(event.localX, event.localY)) {
                    return false
                }
                if (event.clickCount >= 2) {
                    dragging = false
                    ratio = 0.5
                } else {
                    dragging = true
                }
                true
            }

            KeyAction.RELEASE -> {
                if (!dragging) {
                    return false
                }
                dragging = false
                true
            }

            else -> {
                false
            }
        }
    }

    private fun onMove(event: GuiPointerMoveEvent): Boolean {
        if (!dragging) {
            return false
        }
        updateFromPointer(event.localX, event.localY)
        return true
    }

    private fun onKey(event: GuiKeyEvent): Boolean {
        if (!isFocused || !dividerVisibleValue || event.action == KeyAction.RELEASE) {
            return false
        }
        val total =
            if (orientation == SplitOrientation.HORIZONTAL) {
                contentRect.width
            } else {
                contentRect.height
            }
        val available = (total - 1).coerceAtLeast(0)
        if (available <= 0) {
            return false
        }
        val current = allocate(total).first
        return when (event.key) {
            Key.LEFT, Key.UP -> {
                if ((orientation == SplitOrientation.HORIZONTAL && event.key != Key.LEFT) ||
                    (orientation == SplitOrientation.VERTICAL && event.key != Key.UP)
                ) {
                    false
                } else {
                    setFirst(current - 1, available)
                }
            }

            Key.RIGHT, Key.DOWN -> {
                if ((orientation == SplitOrientation.HORIZONTAL && event.key != Key.RIGHT) ||
                    (orientation == SplitOrientation.VERTICAL && event.key != Key.DOWN)
                ) {
                    false
                } else {
                    setFirst(current + 1, available)
                }
            }

            Key.HOME -> {
                setFirst(minFirstValue, available)
            }

            Key.END -> {
                setFirst(available - minSecondValue, available)
            }

            else -> {
                false
            }
        }
    }

    private fun setFirst(
        first: Int,
        available: Int,
    ): Boolean {
        val clamped =
            first.coerceIn(
                minFirstValue.coerceAtMost(available),
                available - minSecondValue.coerceAtMost(available),
            )
        val current =
            allocate(
                if (orientation ==
                    SplitOrientation.HORIZONTAL
                ) {
                    contentRect.width
                } else {
                    contentRect.height
                },
            )
        if (clamped == current.first) {
            return false
        }
        ratioValue = if (available == 0) ratioValue else clamped.toDouble() / available
        invalidateLayout()
        invalidatePaint()
        return true
    }

    private fun updateFromPointer(
        localX: Int,
        localY: Int,
    ) {
        val total =
            if (orientation == SplitOrientation.HORIZONTAL) {
                contentRect.width
            } else {
                contentRect.height
            }
        if (total <= 0) {
            return
        }
        val available = (total - 1).coerceAtLeast(0)
        if (available <= 0) {
            return
        }
        val raw = if (orientation == SplitOrientation.HORIZONTAL) localX else localY
        val first =
            raw.coerceIn(
                minFirstValue.coerceAtMost(available),
                available - minSecondValue.coerceAtMost(available),
            )
        val next = first.toDouble() / available
        if (next != ratioValue) {
            ratioValue = next
            invalidateLayout()
            invalidatePaint()
        }
    }

    private fun onDivider(
        x: Int,
        y: Int,
    ): Boolean {
        if (!dividerVisibleValue) {
            return false
        }
        return if (orientation == SplitOrientation.HORIZONTAL) {
            x == dividerPositionValue
        } else {
            y == dividerPositionValue
        }
    }

    private fun allocate(total: Int): Allocation {
        if (total <= 0) {
            return Allocation(0, 0, false)
        }
        val divider = total >= minFirstValue + minSecondValue + 1
        if (!divider) {
            val first = (total * ratioValue).roundToInt().coerceIn(0, total)
            return Allocation(first, total - first, false)
        }
        val available = total - 1
        val first =
            (available * ratioValue)
                .roundToInt()
                .coerceIn(minFirstValue, available - minSecondValue)
        return Allocation(first, available - first, true)
    }

    private data class Allocation(
        val first: Int,
        val second: Int,
        val divider: Boolean,
    )
}
