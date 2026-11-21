package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Alignment
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Positions a child at fractional alignment within its box.
 *
 * When the incoming constraints bound an axis, Align fills that axis; when
 * unbounded, it uses the child size.
 *
 * @property alignment Child alignment.
 * @since 1.0.0
 */
public open class Align(
    public val alignment: Alignment = Alignment.CENTER,
    child: Widget? = null,
) : SingleChildWidget(child) {
    override fun measure(constraints: Constraints): IntSize {
        val current = child ?: return constraints.constrain(IntSize.ZERO)
        val childSize = current.measured(constraints.loosen())
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else childSize.width
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else childSize.height
        return constraints.constrain(IntSize(width, height))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        val childSize = current.measured(Constraints.loose(IntSize(content.width, content.height)))
        val x = alignment.offsetX(content.width, childSize.width)
        val y = alignment.offsetY(content.height, childSize.height)
        current.arrange(Rect(x, y, childSize.width, childSize.height))
    }

    override fun paint(context: PaintContext) {}
}

/**
 * Centers a child.
 *
 * @since 1.0.0
 */
public class Center(
    child: Widget? = null,
) : Align(Alignment.CENTER, child)
