package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.input.FocusTraversal
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatusBarTest {
    @Test
    fun sectionsUseLeftCenterAndRightCells() {
        val bar = StatusBar("L", "C", "R")
        val harness = GuiTestHarness(bar, columns = 20, rows = 1)
        harness.paint()
        val row = harness.rows()[0]
        assertEquals('L', row[0])
        assertEquals('C', row[9])
        assertEquals('R', row[19])
        assertTrue(FocusTraversal.order(bar).isEmpty())
    }

    @Test
    fun longSectionsTruncateWithoutOverlappingRight() {
        val bar = StatusBar("LeftLong", "CenterText", "Right")
        val harness = GuiTestHarness(bar, columns = 10, rows = 1)
        harness.paint()
        val row = harness.rows()[0]
        assertEquals(10, row.length)
        assertEquals("Right", row.substring(5))
        assertEquals('…', row[4])
    }

    @Test
    fun hintSectionsAreSeparatedAndMuted() {
        val bar =
            StatusBar(
                hints =
                    listOf(
                        KeyHint("Ctrl+S", "Save"),
                        KeyHint("Ctrl+Q", "Quit"),
                    ),
            )
        val harness = GuiTestHarness(bar, columns = 40, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("Ctrl+S Save | Ctrl+Q Quit"))
        assertTrue(harness.styles()[0].contains("textMuted/surface"))
    }

    @Test
    fun keyHintFormatsChordAndLabel() {
        val hint = KeyHint(KeyChord(Key.S, Modifiers.CONTROL), "Save")
        val harness = GuiTestHarness(hint, columns = 20, rows = 1)
        harness.paint()
        assertEquals("Ctrl+S Save", harness.rows()[0].trimEnd())
        assertEquals(20, hint.bounds.width)
    }

    @Test
    fun oneCellStatusBarPaintsWithoutOverflow() {
        val bar = StatusBar("Left", "Center", "Right")
        val harness = GuiTestHarness(bar, columns = 1, rows = 1)
        harness.paint()
        assertEquals(1, harness.rows()[0].length)
        assertEquals('…', harness.rows()[0][0])
    }

    @Test
    fun hintListConstructorsRenderSeparatedText() {
        val bar = StatusBar(listOf(KeyHint("Ctrl+S", "Save"), KeyHint("Ctrl+Q", "Quit")))
        val harness = GuiTestHarness(bar, columns = 40, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].contains("Ctrl+S Save | Ctrl+Q Quit"))

        val sections =
            StatusBar(
                left = listOf(KeyHint("A", "alpha")),
                center = listOf(KeyHint("B", "beta")),
                right = listOf(KeyHint("C", "gamma")),
            )
        val sectionHarness = GuiTestHarness(sections, columns = 40, rows = 1)
        sectionHarness.paint()
        assertTrue(sectionHarness.rows()[0].contains("A alpha"))
        assertTrue(sectionHarness.rows()[0].contains("C gamma"))
    }

    @Test
    fun statusBarMatchesGolden() {
        val bar = StatusBar("Ready", "Build 7", "12:30")
        val harness = GuiTestHarness(bar, columns = 30, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("status-bar-30x1.txt", harness.rows())
    }
}
