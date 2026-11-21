package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

internal class RowsWidget(
    private val width: Int,
    private val height: Int,
) : Widget() {
    var lastLocalY: Int = -1

    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(width, height))

    override fun paint(context: PaintContext) {
        var row = 0
        while (row < bounds.height) {
            val character = ('0'.code + (row % 10)).toChar()
            context.text(0, row, character.toString().repeat(bounds.width), Colors.WHITE, Colors.BLACK)
            row++
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        if (event is GuiPointerMoveEvent) {
            lastLocalY = event.localY
        }
        return false
    }
}
