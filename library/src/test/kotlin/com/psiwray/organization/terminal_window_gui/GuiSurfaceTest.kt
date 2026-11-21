package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.InMemoryClipboard
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuiSurfaceTest {
    private val shapes: ResolvedShapeSet = ShapeSets.unicode().resolve(GuiFonts.bundled())

    @Test
    fun constructionStartsAtZeroAndRequiresResize() {
        val root = SurfaceWidget(10, 3)
        val surface = GuiSurface(root, Themes.dark(), shapes)
        assertEquals(GridSize.ZERO, surface.size)
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
        assertEquals(Rect.EMPTY, surface.overlays.screen)
        assertFailsWith<GuiException> {
            surface.dispatch(GuiKeyEvent(Key.A, KeyAction.PRESS, Modifiers.NONE, false))
        }
    }

    @Test
    fun resizeSetsRootBoundsAndInvalidatesWithoutPainting() {
        val root = SurfaceWidget(10, 3)
        val surface = GuiSurface(root, Themes.dark(), shapes)
        surface.resize(10, 3)
        assertEquals(GridSize(10, 3), surface.size)
        assertEquals(Rect(0, 0, 10, 3), root.bounds)
        assertTrue(surface.needsLayout)
        assertTrue(surface.needsPaint)
        assertEquals(0, root.paintCount)
        assertEquals(Rect(0, 0, 10, 3), surface.overlays.screen)
    }

    @Test
    fun dispatchLaysOutWithoutPainting() {
        val root = SurfaceWidget(10, 3, consumeEvents = true)
        val surface = GuiSurface(root, Themes.dark(), shapes)
        surface.resize(10, 8)
        assertTrue(surface.dispatch(GuiKeyEvent(Key.ENTER, KeyAction.PRESS, Modifiers.NONE, false)))
        assertEquals(1, root.eventCount)
        assertEquals(0, root.paintCount)
        assertEquals(1, surface.layoutCount)
        assertEquals(0, surface.paintCount)
        assertFalse(surface.needsLayout)
        assertTrue(surface.needsPaint)
        assertEquals(Rect(0, 0, 10, 8), root.bounds)
    }

    @Test
    fun paintDoesNotDispatchAndClearsOnlyPaintFlag() {
        val root = SurfaceWidget(10, 3)
        val surface = GuiSurface(root, Themes.dark(), shapes)
        surface.resize(10, 3)
        surface.paint(
            CellCanvas(
                com.psiwray.organization.terminal_window
                    .TerminalMatrix(10, 3),
            ),
        )
        assertEquals(0, root.eventCount)
        assertEquals(1, root.paintCount)
        assertFalse(surface.needsPaint)
        assertFalse(surface.needsLayout)
        assertEquals(1, surface.paintCount)
    }

    @Test
    fun themeChangeInvalidatesPaintOnly() {
        val root = SurfaceWidget(10, 3)
        val surface = GuiSurface(root, Themes.dark(), shapes)
        surface.resize(10, 3)
        surface.paint(
            CellCanvas(
                com.psiwray.organization.terminal_window
                    .TerminalMatrix(10, 3),
            ),
        )
        val layoutCount = surface.layoutCount
        surface.setTheme(Themes.light())
        assertEquals(Themes.light(), surface.theme)
        assertTrue(surface.needsPaint)
        assertFalse(surface.needsLayout)
        assertEquals(layoutCount, surface.layoutCount)
    }

    @Test
    fun pointerShapeComesFromHoveredWidget() {
        val root = SurfaceWidget(10, 3)
        root.cursorShape = CursorShape.IBEAM
        val surface = GuiSurface(root, Themes.dark(), shapes)
        surface.resize(10, 3)
        surface.dispatch(GuiPointerMoveEvent(1, 1, 1, 1, Modifiers.NONE))
        assertEquals(CursorShape.IBEAM, surface.pointerShape)
        surface.dispatch(GuiPointerMoveEvent(99, 99, 99, 99, Modifiers.NONE))
        assertNull(surface.pointerShape)
    }

    @Test
    fun overlaysDispatchFirstAndPaintLast() {
        val root = SurfaceWidget(10, 3, 'R')
        val surface = GuiSurface(root, Themes.dark(), shapes)
        val overlay = SurfaceWidget(3, 1, 'O', consumeEvents = true)
        surface.overlays.add(overlay)
        surface.resize(10, 3)
        val click =
            GuiPointerButtonEvent(
                button = MouseButton.LEFT,
                action = KeyAction.PRESS,
                modifiers = Modifiers.NONE,
                localX = 4,
                localY = 1,
                screenX = 4,
                screenY = 1,
                clickCount = 1,
            )
        assertTrue(surface.dispatch(click))
        assertEquals(1, overlay.eventCount)
        assertEquals(0, root.eventCount)
        surface.paint(
            CellCanvas(
                com.psiwray.organization.terminal_window
                    .TerminalMatrix(10, 3),
            ),
        )
        val matrix = lastMatrix(surface)
        assertEquals('R'.code, matrix.codePointAt(0, 0))
        assertEquals('O'.code, matrix.codePointAt(3, 1))
        assertEquals('O'.code, matrix.codePointAt(4, 1))
        assertEquals('O'.code, matrix.codePointAt(5, 1))
    }

    @Test
    fun tickUsesCallerClockAndNeverPaints() {
        var now = 0L
        val surface = GuiSurface(SurfaceWidget(2, 1), Themes.dark(), shapes, InMemoryClipboard()) { now }
        surface.resize(2, 1)
        var fired = false
        surface.scheduler.postDelayed(100) {
            fired = true
            surface.requestPaint()
        }
        now = 0
        assertFalse(surface.tick(0))
        assertFalse(fired)
        now = 100_000_000
        assertTrue(surface.tick(now))
        assertTrue(fired)
        assertTrue(surface.needsPaint)
        assertEquals(0, surface.paintCount)
    }

    private fun lastMatrix(surface: GuiSurface): com.psiwray.organization.terminal_window.TerminalMatrix {
        val matrix =
            com.psiwray.organization.terminal_window
                .TerminalMatrix(surface.size.columns, surface.size.rows)
        surface.paint(CellCanvas(matrix))
        return matrix
    }
}

internal open class SurfaceWidget(
    private val width: Int,
    private val height: Int,
    private val mark: Char = '#',
    private val consumeEvents: Boolean = false,
) : Widget() {
    var paintCount: Int = 0
    var eventCount: Int = 0

    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(width, height))

    override fun paint(context: PaintContext) {
        paintCount++
        context.text(0, 0, mark.toString().repeat(bounds.width.coerceAtMost(width)), Colors.WHITE, Colors.BLACK)
    }

    override fun onEvent(event: GuiEvent): Boolean {
        eventCount++
        return consumeEvents
    }
}

internal class ThrowSurfaceWidget : SurfaceWidget(3, 1) {
    var fail: Boolean = false

    override fun paint(context: PaintContext) {
        if (fail) {
            throw IllegalStateException("paint failure")
        }
        super.paint(context)
    }
}
