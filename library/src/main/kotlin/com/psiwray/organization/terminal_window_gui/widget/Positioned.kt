package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Alignment
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Length
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.roundToInt

/**
 * Positions one child inside a [Stack] using edges and/or extents.
 *
 * Left plus right derives width; top plus bottom derives height. An explicit
 * size combined with the same edge pair throws. Fractions are rounded and
 * clamped to non-negative extents.
 *
 * @property left Left edge or fraction.
 * @property top Top edge or fraction.
 * @property right Right edge or fraction.
 * @property bottom Bottom edge or fraction.
 * @property width Explicit width.
 * @property height Explicit height.
 * @since 1.0.0
 */
public class Positioned(
    public val left: Length? = null,
    public val top: Length? = null,
    public val right: Length? = null,
    public val bottom: Length? = null,
    public val width: Length? = null,
    public val height: Length? = null,
    child: Widget,
) : SingleChildWidget(child) {
    init {
        require(!(width != null && left != null && right != null)) {
            "width cannot be combined with both left and right"
        }
        require(!(height != null && top != null && bottom != null)) {
            "height cannot be combined with both top and bottom"
        }
        require(width !is Length.Cells || width.value >= 0) { "width must be non-negative" }
        require(height !is Length.Cells || height.value >= 0) { "height must be non-negative" }
    }

    override fun measure(constraints: Constraints): IntSize {
        val parent =
            IntSize(
                if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth,
                if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight,
            )
        val rect = resolveRect(parent)
        return constraints.constrain(IntSize(rect.width, rect.height))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        current.arrange(Rect(0, 0, bounds.width, bounds.height))
    }

    override fun paint(context: PaintContext) {}

    internal fun resolveRect(parent: IntSize): Rect {
        val current = child
        val measured =
            current?.measured(Constraints(0, parent.width, 0, parent.height))
                ?: IntSize.ZERO
        val resolvedWidth =
            resolveExtent(width, parent.width, measured.width)
                ?: deriveExtent(left, right, parent.width)
                ?: measured.width
        val resolvedHeight =
            resolveExtent(height, parent.height, measured.height)
                ?: deriveExtent(top, bottom, parent.height)
                ?: measured.height
        val x =
            when {
                left != null -> resolvePosition(left, parent.width)
                right != null -> parent.width - resolvePosition(right, parent.width) - resolvedWidth
                else -> 0
            }
        val y =
            when {
                top != null -> resolvePosition(top, parent.height)
                bottom != null -> parent.height - resolvePosition(bottom, parent.height) - resolvedHeight
                else -> 0
            }
        return Rect(x, y, resolvedWidth.coerceAtLeast(0), resolvedHeight.coerceAtLeast(0))
    }

    private fun resolveExtent(
        length: Length?,
        parent: Int,
        content: Int,
    ): Int? =
        when (length) {
            null -> null
            is Length.Cells -> length.value.coerceAtLeast(0)
            is Length.Fraction -> (parent * length.value).roundToInt().coerceAtLeast(0)
            Length.Content -> content
            Length.Fill -> parent
        }

    private fun deriveExtent(
        first: Length?,
        second: Length?,
        parent: Int,
    ): Int? {
        if (first == null || second == null) {
            return null
        }
        return (parent - resolvePosition(first, parent) - resolvePosition(second, parent)).coerceAtLeast(0)
    }

    private fun resolvePosition(
        length: Length,
        parent: Int,
    ): Int =
        when (length) {
            is Length.Cells -> length.value
            is Length.Fraction -> (parent * length.value).roundToInt()
            Length.Content -> 0
            Length.Fill -> 0
        }
}
