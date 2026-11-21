package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Axis
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.MainAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Vertical flex container.
 *
 * Non-flex children are measured with an unbounded main axis and bounded cross
 * axis. Remaining main-axis space goes to [Flexible] children by weight.
 *
 * @property mainAxisAlignment Main-axis distribution.
 * @property crossAxisAlignment Cross-axis alignment.
 * @property mainAxisSize Whether to shrink or fill the main axis.
 * @property gap Cells between children.
 * @since 1.0.0
 */
public class Column(
    public val mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.START,
    public val crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.START,
    public val mainAxisSize: MainAxisSize = MainAxisSize.MAX,
    public val gap: Int = 0,
    children: List<Widget> = emptyList(),
) : MultiChildWidget() {
    init {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        for (child in children) {
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize =
        FlexEngine.layout(
            children = childList,
            axis = Axis.VERTICAL,
            constraints = constraints,
            mainAxisAlignment = mainAxisAlignment,
            crossAxisAlignment = crossAxisAlignment,
            mainAxisSize = mainAxisSize,
            gap = gap,
            arrange = false,
        )

    override fun arrangeChildren() {
        FlexEngine.layout(
            children = childList,
            axis = Axis.VERTICAL,
            constraints = Constraints.tight(IntSize(contentRect.width, contentRect.height)),
            mainAxisAlignment = mainAxisAlignment,
            crossAxisAlignment = crossAxisAlignment,
            mainAxisSize = mainAxisSize,
            gap = gap,
            arrange = true,
        )
    }

    override fun paint(context: PaintContext) {}
}
