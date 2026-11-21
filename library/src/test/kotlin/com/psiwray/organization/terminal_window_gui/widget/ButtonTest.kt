package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window_gui.GuiInput
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ButtonTest {
    @Test
    fun pressAndReleaseActivateOnce() {
        var count = 0
        val button = Button("OK") { count++ }
        val harness = GuiTestHarness(button, columns = 10, rows = 1)
        harness.paint()
        harness.click(0, 0)
        assertEquals(1, count)
        assertTrue(button.isFocused)
        assertTrue(harness.key(Key.SPACE))
        assertEquals(2, count)
        harness.key(Key.SPACE, action = KeyAction.REPEAT, repeat = true)
        assertEquals(2, count)
    }

    @Test
    fun releaseOutsideCancelsAndFocusLossCancels() {
        var count = 0
        val button = Button("OK") { count++ }
        val harness = GuiTestHarness(button, columns = 10, rows = 1)
        harness.paint()
        harness.move(0, 0)
        press(harness, 0, 0)
        harness.move(20, 0)
        release(harness, 20, 0)
        assertEquals(0, count)

        harness.surface.focus.focus(button)
        harness.move(0, 0)
        press(harness, 0, 0)
        harness.surface.focus.clearFocus()
        release(harness, 0, 0)
        assertEquals(0, count)
    }

    @Test
    fun onlyDefaultUsesEnterWhileSpaceAlwaysWorks() {
        var defaultCount = 0
        var normalCount = 0
        val default = Button("Default", default = true) { defaultCount++ }
        val normal = Button("Normal") { normalCount++ }
        val defaultHarness = GuiTestHarness(default, columns = 12, rows = 1)
        defaultHarness.paint()
        assertTrue(defaultHarness.surface.focus.focus(default))
        assertTrue(defaultHarness.key(Key.ENTER))
        assertEquals(1, defaultCount)

        val normalHarness = GuiTestHarness(normal, columns = 12, rows = 1)
        normalHarness.paint()
        assertTrue(normalHarness.surface.focus.focus(normal))
        assertFalse(normalHarness.key(Key.ENTER))
        assertTrue(normalHarness.key(Key.SPACE))
        assertEquals(1, normalCount)
    }

    @Test
    fun disabledButtonReceivesNoFocusOrActivation() {
        var count = 0
        val button = Button("Disabled", enabled = false) { count++ }
        val harness = GuiTestHarness(button, columns = 12, rows = 1)
        harness.paint()
        assertFalse(harness.surface.focus.focus(button))
        harness.click(0, 0)
        harness.key(Key.SPACE)
        assertEquals(0, count)
        assertFalse(button.isFocused)
    }

    @Test
    fun mnemonicActivatesFirstVisibleEnabledButton() {
        var first = 0
        var second = 0
        val a = Button("Alpha", mnemonic = 'a') { first++ }
        val b = Button("Again", mnemonic = 'a') { second++ }
        val harness = GuiTestHarness(Row(children = listOf(a, b)), columns = 30, rows = 1)
        harness.paint()
        assertTrue(harness.key(Key.A, Modifiers.ALT))
        assertEquals(1, first)
        assertEquals(0, second)

        a.enabled = false
        assertTrue(harness.key(Key.A, Modifiers.ALT))
        assertEquals(1, first)
        assertEquals(1, second)
    }

    @Test
    fun mnemonicIsHighlightedWithoutAttributes() {
        val button = Button("Save", mnemonic = 'S')
        val harness = GuiTestHarness(button, columns = 10, rows = 1)
        harness.paint()
        val row = harness.rows()[0]
        val column = row.indexOf('S')
        assertTrue(column > 0)
        assertEquals(harness.surface.theme.selectionBackground, harness.matrix.backgroundAt(column, 0))
    }

    @Test
    fun focusDefaultAndTruncationAreVisibleInEveryTheme() {
        for (theme in listOf(Themes.dark(), Themes.light(), Themes.highContrast())) {
            val button = Button("Long label", default = true)
            val harness = GuiTestHarness(button, theme, columns = 5, rows = 1)
            harness.surface.focus.focus(button)
            harness.paint()
            assertTrue(harness.rows()[0].contains('*'))
            assertTrue(harness.rows()[0].contains('…'))
            assertNotEquals(harness.surface.theme.surface, harness.matrix.backgroundAt(0, 0))
        }
    }

    @Test
    fun hoverAndPressedHaveDistinctStateStyles() {
        val button = Button("OK")
        val harness = GuiTestHarness(button, columns = 10, rows = 1)
        harness.paint()
        val normal = harness.matrix.backgroundAt(0, 0)
        harness.move(0, 0)
        harness.paint()
        val hovered = harness.matrix.backgroundAt(0, 0)
        assertNotEquals(normal, hovered)
        press(harness, 0, 0)
        harness.paint()
        val pressed = harness.matrix.backgroundAt(0, 0)
        assertNotEquals(hovered, pressed)
    }

    @Test
    fun disablingAFocusedButtonClearsFocusAndEvents() {
        val button = Button("OK")
        val harness = GuiTestHarness(button, columns = 10, rows = 1)
        harness.paint()
        harness.click(0, 0)
        assertTrue(button.isFocused)
        button.enabled = false
        assertFalse(button.isFocused)
        assertFalse(harness.surface.focus.focus(button))
    }

    @Test
    fun iconAndTruncatedLabelStayInsideBounds() {
        val harness = GuiTestHarness(Button("Wide", icon = Shape.CHECK), columns = 3, rows = 1)
        harness.paint()
        assertEquals(3, harness.rows()[0].length)
        GoldenAssert.assertRows("button-icon-narrow.txt", harness.rows())
    }

    @Test
    fun normalAndFocusedStatesMatchGoldens() {
        val normal = Button("OK")
        val normalHarness = GuiTestHarness(normal, columns = 8, rows = 1)
        normalHarness.paint()
        GoldenAssert.assertRows("button-normal.txt", normalHarness.rows())

        val focused = Button("OK", default = true)
        val focusedHarness = GuiTestHarness(focused, columns = 8, rows = 1)
        focusedHarness.surface.focus.focus(focused)
        focusedHarness.paint()
        GoldenAssert.assertRows("button-default-focused.txt", focusedHarness.rows())
    }

    @Test
    fun asciiShapesUseAsciiEllipsis() {
        val button = Button("Long")
        val harness =
            GuiTestHarness(
                button,
                shapes =
                    com.psiwray.organization.terminal_window_gui.shape.ShapeSets
                        .ascii()
                        .resolve(
                            com.psiwray.organization.terminal_window_gui.shape.GuiFonts
                                .bundled(),
                        ),
                columns = 4,
                rows = 1,
            )
        harness.paint()
        assertTrue(harness.rows()[0].contains('.'))
        GoldenAssert.assertRows("button-ascii.txt", harness.rows())
    }

    private fun press(
        harness: GuiTestHarness,
        column: Int,
        row: Int,
    ) {
        harness.surface.dispatch(
            GuiInput.button(
                MouseButtonEvent(
                    button = MouseButton.LEFT,
                    action = KeyAction.PRESS,
                    modifiers = Modifiers.NONE,
                    column = column,
                    row = row,
                ),
            ),
        )
    }

    private fun release(
        harness: GuiTestHarness,
        column: Int,
        row: Int,
    ) {
        harness.surface.dispatch(
            GuiInput.button(
                MouseButtonEvent(
                    button = MouseButton.LEFT,
                    action = KeyAction.RELEASE,
                    modifiers = Modifiers.NONE,
                    column = column,
                    row = row,
                ),
            ),
        )
    }
}
