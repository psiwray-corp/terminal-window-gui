package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PointerCaptureTest {
    @Test
    fun captureAndReleaseUpdatePressedState() {
        val widget = InteractionWidget("target")
        val capture = PointerCapture()
        capture.capture(widget, MouseButton.LEFT)
        assertTrue(capture.isActive)
        assertEquals(widget, capture.captured)
        assertEquals(MouseButton.LEFT, capture.pressedButton)
        assertTrue(widget.isPressed)
        capture.release()
        assertFalse(capture.isActive)
        assertNull(capture.captured)
        assertFalse(widget.isPressed)
    }

    @Test
    fun focusLossClearsCaptureAndHoverButKeepsKeyboardFocus() {
        val child = InteractionWidget("child")
        val root = TestContainer(listOf(child), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(child))
        val router = EventRouter(root, focus)

        router.dispatch(GuiPointerMoveEvent(0, 0, 0, 0, Modifiers.NONE))
        router.dispatch(GuiPointerButtonEvent(MouseButton.LEFT, KeyAction.PRESS, Modifiers.NONE, 0, 0, 0, 0, 1))
        assertTrue(router.pointerCapture.isActive)
        assertTrue(child.isHovered)
        router.dispatch(GuiFocusEvent(false))
        assertFalse(router.pointerCapture.isActive)
        assertFalse(child.isHovered)
        assertEquals(child, focus.focused)
    }
}
