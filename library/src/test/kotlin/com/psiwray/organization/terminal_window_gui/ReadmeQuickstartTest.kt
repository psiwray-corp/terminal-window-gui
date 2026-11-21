package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.TerminalWindow
import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.FocusEvent
import com.psiwray.organization.terminal_window.input.InputListener
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window.input.MouseMoveEvent
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget
import kotlin.test.Test

/**
 * Compile-time proof for the README quickstart. The body is not executed; the
 * library never starts a loop as part of a test.
 */
class ReadmeQuickstartTest {
    @Test
    fun readmeQuickstartCompiles() {
        // The function below is deliberately compile-only.
        reference(::readmeQuickstart)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun reference(function: (Widget, TerminalWindow) -> Unit) {
        // Keeps the compile-only function referenced without executing it.
    }

    @Suppress("unused")
    private fun readmeQuickstart(
        root: Widget,
        window: TerminalWindow,
    ) {
        val surface = GuiSurface(root, Themes.dark(), ShapeSets.unicode().resolve(GuiFonts.bundled()))
        surface.resize(window.matrix.columns, window.matrix.rows)
        var pointerColumn = 0
        var pointerRow = 0
        var appliedCursor: CursorShape? = null
        window.addInputListener(
            object : InputListener {
                override fun onKey(event: KeyEvent) {
                    surface.dispatch(GuiInput.key(event))
                }

                override fun onChar(event: CharEvent) {
                    surface.dispatch(GuiInput.char(event))
                }

                override fun onMouseMove(event: MouseMoveEvent) {
                    pointerColumn = event.column
                    pointerRow = event.row
                    surface.dispatch(GuiInput.move(event))
                }

                override fun onMouseButton(event: MouseButtonEvent) {
                    surface.dispatch(GuiInput.button(event))
                }

                override fun onScroll(event: ScrollEvent) {
                    surface.dispatch(GuiInput.scroll(event, pointerColumn, pointerRow))
                }

                override fun onFocus(event: FocusEvent) {
                    surface.dispatch(GuiInput.focus(event))
                }
            },
        )
        while (!window.shouldClose) {
            window.frame()
            if (surface.size.columns != window.matrix.columns ||
                surface.size.rows != window.matrix.rows
            ) {
                surface.resize(window.matrix.columns, window.matrix.rows)
            }
            surface.tick(System.nanoTime())
            if (surface.needsPaint) {
                surface.paint(CellCanvas(window.matrix))
                window.requestRepaint()
            }
            val cursor = surface.pointerShape
            if (cursor != null && cursor != appliedCursor) {
                window.setCursor(cursor)
                appliedCursor = cursor
            }
            Thread.sleep(15)
        }
        window.close()
    }
}
