package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.roundToInt

/**
 * Sizes itself and its child as a fraction of bounded parent extents.
 *
 * @property widthFactor Width fraction in `0.0..1.0`.
 * @property heightFactor Height fraction in `0.0..1.0`.
 * @since 1.0.0
 */
public class FractionallySizedBox(
    public val widthFactor: Float = 1f,
    public val heightFactor: Float = 1f,
    child: Widget? = null,
) : SingleChildWidget(child) {
    /** Double-precision convenience constructor. @since 1.0.0 */
    public constructor(
        widthFactor: Double,
        heightFactor: Double,
        child: Widget? = null,
    ) : this(widthFactor.toFloat(), heightFactor.toFloat(), child)

    init {
        require(widthFactor.isFinite() && widthFactor in 0f..1f) {
            "widthFactor must be in 0.0..1.0, got $widthFactor"
        }
        require(heightFactor.isFinite() && heightFactor in 0f..1f) {
            "heightFactor must be in 0.0..1.0, got $heightFactor"
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        val width =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth * widthFactor).roundToInt().coerceAtLeast(0)
            } else {
                constraints.minWidth
            }
        val height =
            if (constraints.hasBoundedHeight) {
                (constraints.maxHeight * heightFactor).roundToInt().coerceAtLeast(0)
            } else {
                constraints.minHeight
            }
        val size = constraints.constrain(IntSize(width, height))
        child?.measured(Constraints.tight(size))
        return size
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        current.arrange(Rect(0, 0, content.width, content.height))
    }

    override fun paint(context: PaintContext) {}
}
