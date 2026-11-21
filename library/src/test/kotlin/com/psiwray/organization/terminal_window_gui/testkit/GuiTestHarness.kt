package com.psiwray.organization.terminal_window_gui.testkit

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.GuiInput
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.input.Clipboard
import com.psiwray.organization.terminal_window_gui.input.InMemoryClipboard
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Headless component-test harness around [GuiSurface].
 *
 * The harness owns a [TerminalMatrix], a fake monotonic clock starting at zero,
 * and synthetic event helpers. It never uses GL, a window, or a thread.
 */
class GuiTestHarness(
    root: Widget,
    theme: Theme = Themes.dark(),
    shapes: ResolvedShapeSet = ShapeSets.unicode().resolve(GuiFonts.bundled()),
    columns: Int = 80,
    rows: Int = 24,
    clipboard: Clipboard = InMemoryClipboard(),
) {
    /** Surface under test. */
    val surface: GuiSurface = GuiSurface(root, theme, shapes, clipboard) { clockNanos }

    private var clockNanos: Long = 0
    private var matrixValue: TerminalMatrix = TerminalMatrix(1, 1)
    private var lastPointerColumn: Int = 0
    private var lastPointerRow: Int = 0

    init {
        resize(columns, rows)
    }

    /** Current matrix receiving painted cells. */
    val matrix: TerminalMatrix get() = matrixValue

    /** Number of completed layout passes. */
    val layoutCount: Int get() = surface.layoutCount

    /** Number of completed paint passes. */
    val paintCount: Int get() = surface.paintCount

    /** Current fake clock value in nanoseconds. */
    val nowNanos: Long get() = clockNanos

    /** Resizes the surface and its matrix. */
    fun resize(
        columns: Int,
        rows: Int,
    ) {
        surface.resize(columns, rows)
        matrixValue = TerminalMatrix(columns, rows)
    }

    /** Paints the complete surface into the harness matrix. */
    fun paint() {
        surface.paint(CellCanvas(matrixValue))
    }

    /** Returns structural rows of the last paint. */
    fun rows(): List<String> = MatrixDump.rows(matrixValue)

    /** Returns style rows of the last paint. */
    fun styles(): List<String> = MatrixDump.styles(matrixValue, surface.theme)

    /** Dispatches a key event and returns whether it was consumed. */
    fun key(
        key: Key,
        action: KeyAction = KeyAction.PRESS,
        modifiers: Modifiers = Modifiers.NONE,
        repeat: Boolean = action == KeyAction.REPEAT,
    ): Boolean =
        surface.dispatch(
            GuiInput.key(
                KeyEvent(
                    key = key,
                    scancode = 0,
                    action = action,
                    modifiers = modifiers,
                    repeat = repeat,
                ),
            ),
        )

    /** Dispatches a key press with explicit modifiers. */
    fun key(
        key: Key,
        modifiers: Modifiers,
    ): Boolean = key(key, KeyAction.PRESS, modifiers, false)

    /** Dispatches a host key event through [GuiInput]. */
    fun key(event: KeyEvent): Boolean = surface.dispatch(GuiInput.key(event))

    /** Dispatches a text event and returns whether it was consumed. */
    fun char(codePoint: Int): Boolean = surface.dispatch(GuiInput.char(CharEvent(codePoint)))

    /** Dispatches a character and returns whether it was consumed. */
    fun char(value: Char): Boolean = char(value.code)

    /** Moves the pointer and returns whether the move was consumed. */
    fun move(
        column: Int,
        row: Int,
    ): Boolean {
        lastPointerColumn = column
        lastPointerRow = row
        return surface.dispatch(
            GuiInput.move(
                com.psiwray.organization.terminal_window.input.MouseMoveEvent(
                    pixelX = column,
                    pixelY = row,
                    column = column,
                    row = row,
                ),
            ),
        )
    }

    /** Moves, presses, and releases the pointer at one cell. */
    fun click(
        column: Int,
        row: Int,
        button: MouseButton = MouseButton.LEFT,
        modifiers: Modifiers = Modifiers.NONE,
    ): Boolean {
        var consumed = move(column, row)
        consumed = pointerButton(button, KeyAction.PRESS, column, row, modifiers) || consumed
        consumed = pointerButton(button, KeyAction.RELEASE, column, row, modifiers) || consumed
        return consumed
    }

    /** Presses at the start cell, moves to the end cell, then releases. */
    fun drag(
        startColumn: Int,
        startRow: Int,
        endColumn: Int,
        endRow: Int,
        button: MouseButton = MouseButton.LEFT,
    ): Boolean {
        var consumed = move(startColumn, startRow)
        consumed = pointerButton(button, KeyAction.PRESS, startColumn, startRow) || consumed
        consumed = move(endColumn, endRow) || consumed
        consumed = pointerButton(button, KeyAction.RELEASE, endColumn, endRow) || consumed
        return consumed
    }

    /** Dispatches a scroll event at the last pointer cell. */
    fun scroll(
        dx: Double,
        dy: Double,
    ): Boolean = scroll(dx, dy, lastPointerColumn, lastPointerRow)

    /** Dispatches a scroll event at an explicit cell. */
    fun scroll(
        dx: Double,
        dy: Double,
        column: Int,
        row: Int,
    ): Boolean = surface.dispatch(GuiInput.scroll(ScrollEvent(dx, dy), column, row))

    /** Dispatches an integer scroll event at the last pointer cell. */
    fun scroll(
        dx: Int,
        dy: Int,
    ): Boolean = scroll(dx.toDouble(), dy.toDouble())

    /** Dispatches an integer scroll event at an explicit cell. */
    fun scroll(
        dx: Int,
        dy: Int,
        column: Int,
        row: Int,
    ): Boolean = scroll(dx.toDouble(), dy.toDouble(), column, row)

    /** Advances the fake clock by [millis] and runs due timers. */
    fun advance(millis: Int): Boolean = advance(millis.toLong())

    /** Advances the fake clock and runs due timers. */
    fun advance(millis: Long): Boolean {
        require(millis >= 0) { "millis must be non-negative, got $millis" }
        clockNanos += millis * 1_000_000L
        return surface.tick(clockNanos)
    }

    /** Runs due timers at the current fake clock. */
    fun tick(): Boolean = surface.tick(clockNanos)

    /** Runs due timers at an explicit monotonic timestamp. */
    fun tick(nowNanos: Long): Boolean = surface.tick(nowNanos)

    /** Runs due timers at an explicit integer timestamp. */
    fun tick(nowNanos: Int): Boolean = surface.tick(nowNanos.toLong())

    /** Returns the current root-to-focused path. */
    fun focusPath(): List<Widget> = surface.focus.focusPath

    private fun pointerButton(
        button: MouseButton,
        action: KeyAction,
        column: Int,
        row: Int,
        modifiers: Modifiers = Modifiers.NONE,
    ): Boolean {
        lastPointerColumn = column
        lastPointerRow = row
        return surface.dispatch(
            GuiInput.button(
                com.psiwray.organization.terminal_window.input.MouseButtonEvent(
                    button = button,
                    action = action,
                    modifiers = modifiers,
                    column = column,
                    row = row,
                ),
            ),
        )
    }
}
