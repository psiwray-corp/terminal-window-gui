package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Flex fit for a child of Row or Column.
 *
 * @since 1.0.0
 */
public enum class FlexFit {
    /** Use the preferred main-axis size up to the allocation. @since 1.0.0 */
    LOOSE,

    /** Fill the allocation. @since 1.0.0 */
    TIGHT,
}

/**
 * A child that receives a share of a Row or Column's free main-axis space.
 *
 * [FlexFit.LOOSE] keeps the child at its preferred size inside its allocation;
 * [FlexFit.TIGHT] fills the allocation. [Expanded] is tight. Flex values must
 * be positive.
 *
 * @property flex Relative weight.
 * @property fit Loose or tight allocation.
 * @since 1.0.0
 */
public open class Flexible(
    public val flex: Int = 1,
    public val fit: FlexFit = FlexFit.LOOSE,
    child: Widget? = null,
) : SingleChildWidget(child) {
    init {
        require(flex > 0) { "flex must be positive, got $flex" }
    }

    /** Positional flex and child convenience constructor. @since 1.0.0 */
    public constructor(
        flex: Int,
        child: Widget?,
    ) : this(flex, FlexFit.LOOSE, child)

    override fun measure(constraints: Constraints): IntSize {
        val current = child ?: return constraints.constrain(IntSize.ZERO)
        return constraints.constrain(current.measured(constraints.loosen()))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        if (fit == FlexFit.TIGHT) {
            current.arrange(Rect(0, 0, content.width, content.height))
        } else {
            val measured = current.measured(Constraints.loose(IntSize(content.width, content.height)))
            current.arrange(Rect(0, 0, measured.width, measured.height))
        }
    }

    override fun paint(context: PaintContext) {}
}
