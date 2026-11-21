package com.psiwray.organization.terminal_window_gui.testkit

import com.psiwray.organization.terminal_window_gui.SurfaceWidget
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuiTestHarnessTest {
    @Test
    fun structuralRowsMatchGolden() {
        val harness = GuiTestHarness(SurfaceWidget(10, 3, 'H'), columns = 10, rows = 3)
        harness.paint()
        assertEquals(1, harness.paintCount)
        assertTrue(harness.layoutCount >= 1)
        GoldenAssert.assertRows("surface-10x3.txt", harness.rows())
    }

    @Test
    fun stylesUseThemeNamesNotRawArgb() {
        val theme = Themes.dark()
        val harness = GuiTestHarness(ThemeWidget(theme), theme, columns = 4, rows = 1)
        harness.paint()
        val styles = harness.styles()
        assertEquals(1, styles.size)
        assertTrue(styles[0].contains("text"))
        assertTrue(styles[0].contains("background"))
        assertFalse(styles[0].contains("#FF"))
    }

    @Test
    fun fakeClockStartsAtZeroAndAdvanceRunsDueTimers() {
        val harness = GuiTestHarness(SurfaceWidget(2, 1, 'H'), columns = 2, rows = 1)
        assertEquals(0, harness.nowNanos)
        var fired = false
        harness.surface.scheduler.postDelayed(50) {
            fired = true
            harness.surface.requestPaint()
        }
        assertFalse(harness.advance(49))
        assertFalse(fired)
        assertTrue(harness.advance(1))
        assertTrue(fired)
        assertEquals(50_000_000, harness.nowNanos)
        assertTrue(harness.surface.needsPaint)
    }

    @Test
    fun themeIsAvailableToTheHarness() {
        val harness = GuiTestHarness(SurfaceWidget(2, 1, 'H'), Themes.light(), columns = 2, rows = 1)
        assertEquals(Themes.light(), harness.surface.theme)
    }
}

private class ThemeWidget(
    private val theme: Theme,
) : Widget() {
    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(4, 1))

    override fun paint(context: PaintContext) {
        context.text(0, 0, "H", theme.text, theme.background)
    }
}
