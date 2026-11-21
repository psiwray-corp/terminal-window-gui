package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets

internal fun testMatrix(
    columns: Int,
    rows: Int,
): TerminalMatrix = TerminalMatrix(columns, rows)

internal fun testContext(matrix: TerminalMatrix): PaintContext =
    PaintContext(CellCanvas(matrix), ShapeSets.unicode().resolve(GuiFonts.bundled()))

internal open class FixedSizeWidget(
    private val preferredWidth: Int,
    private val preferredHeight: Int,
    private val mark: Char = '#',
) : Widget() {
    var measureCount: Int = 0
    var paintCount: Int = 0

    override fun measure(constraints: Constraints): IntSize {
        measureCount++
        return constraints.constrain(IntSize(preferredWidth, preferredHeight))
    }

    override fun paint(context: PaintContext) {
        paintCount++
        val width = bounds.width
        val height = bounds.height
        if (width == 0 || height == 0) {
            return
        }
        context.fill(Rect(0, 0, width, height), Colors.WHITE, Colors.BLACK)
        val line = mark.toString().repeat(width)
        for (row in 0 until height) {
            context.text(0, row, line, Colors.WHITE, Colors.BLACK)
        }
    }
}

internal class TrackingWidget(
    width: Int = 1,
    height: Int = 1,
) : FixedSizeWidget(width, height) {
    var attachCount: Int = 0
    var detachCount: Int = 0

    override fun onAttach() {
        attachCount++
    }

    override fun onDetach() {
        detachCount++
    }
}

internal class SpillWidget : Widget() {
    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(5, 1))

    override fun paint(context: PaintContext) {
        context.text(0, 0, "AB", Colors.WHITE, Colors.BLACK)
        context.text(10, 0, "ZZ", Colors.WHITE, Colors.BLACK)
    }
}

internal class BoundsRecordingWidget(
    width: Int = 1,
    height: Int = 1,
) : FixedSizeWidget(width, height) {
    val changes: MutableList<Pair<Rect, Rect>> = ArrayList()

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        changes.add(old to new)
    }
}
