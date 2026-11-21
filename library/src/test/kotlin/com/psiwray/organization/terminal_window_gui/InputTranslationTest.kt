package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.FocusEvent
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window.input.MouseMoveEvent
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.input.GuiCharEvent
import com.psiwray.organization.terminal_window_gui.input.GuiFocusEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerButtonEvent
import com.psiwray.organization.terminal_window_gui.input.GuiPointerMoveEvent
import com.psiwray.organization.terminal_window_gui.input.GuiScrollEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class InputTranslationTest {
    @Test
    fun keyCopiesKeyActionModifiersAndRepeat() {
        val event = KeyEvent(Key.TAB, 43, KeyAction.REPEAT, Modifiers.SHIFT, true)
        assertEquals(
            GuiKeyEvent(Key.TAB, KeyAction.REPEAT, Modifiers.SHIFT, true),
            GuiInput.key(event),
        )
    }

    @Test
    fun charCopiesCodePoint() {
        assertEquals(GuiCharEvent(0x1F600), GuiInput.char(CharEvent(0x1F600)))
    }

    @Test
    fun moveUsesCellCoordinatesForLocalAndScreen() {
        assertEquals(
            GuiPointerMoveEvent(7, 9, 7, 9, Modifiers.NONE),
            GuiInput.move(MouseMoveEvent(pixelX = 112, pixelY = 144, column = 7, row = 9)),
        )
    }

    @Test
    fun buttonUsesCellCoordinatesAndSingleClick() {
        assertEquals(
            GuiPointerButtonEvent(
                button = MouseButton.RIGHT,
                action = KeyAction.PRESS,
                modifiers = Modifiers.ALT,
                localX = 2,
                localY = 3,
                screenX = 2,
                screenY = 3,
                clickCount = 1,
            ),
            GuiInput.button(
                MouseButtonEvent(
                    button = MouseButton.RIGHT,
                    action = KeyAction.PRESS,
                    modifiers = Modifiers.ALT,
                    column = 2,
                    row = 3,
                ),
            ),
        )
    }

    @Test
    fun scrollUsesSuppliedCellPosition() {
        assertEquals(
            GuiScrollEvent(1.5, -2.0, 4, 5),
            GuiInput.scroll(ScrollEvent(1.5, -2.0), 4, 5),
        )
    }

    @Test
    fun focusCopiesFlag() {
        assertEquals(GuiFocusEvent(false), GuiInput.focus(FocusEvent(false)))
    }
}
