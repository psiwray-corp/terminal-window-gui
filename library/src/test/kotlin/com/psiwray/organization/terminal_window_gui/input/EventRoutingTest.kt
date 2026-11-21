package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.command.Keymap
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EventRoutingTest {
    @Test
    fun pointerEventsUseReceivingLocalCoordinates() {
        val child = InteractionWidget("child", width = 1, height = 1)
        val parent = TestContainer(listOf(child), offsetX = 2, offsetY = 3)
        val root = TestContainer(listOf(parent), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root))

        assertFalse(router.dispatch(GuiPointerMoveEvent(0, 0, 2, 3, Modifiers.NONE)))

        val childEvent = assertIs<GuiPointerMoveEvent>(child.events.single())
        assertEquals(0, childEvent.localX)
        assertEquals(0, childEvent.localY)
        assertEquals(2, childEvent.screenX)
        assertEquals(3, childEvent.screenY)
        val parentEvent = assertIs<GuiPointerMoveEvent>(parent.events.single())
        assertEquals(2, parentEvent.localX)
        assertEquals(3, parentEvent.localY)
        val rootEvent = assertIs<GuiPointerMoveEvent>(root.events.single())
        assertEquals(2, rootEvent.localX)
        assertEquals(3, rootEvent.localY)
    }

    @Test
    fun captureRunsBeforeTargetAndTargetConsumptionStopsBubble() {
        val child = InteractionWidget("child", handler = { true })
        val parent = TestContainer(listOf(child), offsetX = 2, offsetY = 3)
        val root = TestContainer(listOf(parent), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root))

        assertTrue(router.dispatch(GuiPointerMoveEvent(0, 0, 2, 3, Modifiers.NONE)))

        assertEquals(1, root.captureEvents.size)
        assertEquals(1, parent.captureEvents.size)
        assertEquals(0, child.captureEvents.size)
        assertEquals(0, root.events.size)
        assertEquals(0, parent.events.size)
        assertEquals(1, child.events.size)
    }

    @Test
    fun onEventOnlyWidgetsReceiveAnImplicitCaptureCall() {
        val child = InteractionWidget("child", handler = { true })
        val root = BubbleOnlyContainer(listOf(child))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root))

        assertTrue(router.dispatch(GuiPointerMoveEvent(0, 0, 0, 0, Modifiers.NONE)))
        assertEquals(1, root.events.size)
        assertEquals(1, child.events.size)
    }

    @Test
    fun disabledWidgetsAreSkippedByHitTestingAndFocus() {
        val disabled = InteractionWidget("disabled").also { it.enabled = false }
        val enabled = InteractionWidget("enabled")
        val root = TestContainer(listOf(disabled, enabled))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        val router = EventRouter(root, focus)

        assertEquals(listOf(enabled), FocusTraversal.order(root))
        assertFalse(router.dispatch(GuiPointerMoveEvent(0, 0, 1, 0, Modifiers.NONE)))
        assertEquals(0, disabled.events.size)
        assertEquals(1, enabled.events.size)
    }

    @Test
    fun pointerCaptureReceivesOutsideMoveAndRelease() {
        val child = InteractionWidget("child", width = 1, height = 1)
        val root = TestContainer(listOf(child), fillChildren = false)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root))
        val press = GuiPointerButtonEvent(MouseButton.LEFT, KeyAction.PRESS, Modifiers.NONE, 0, 0, 0, 0, 1)

        assertFalse(router.dispatch(press))
        assertTrue(router.pointerCapture.captured === child)
        assertTrue(child.isPressed)

        assertFalse(router.dispatch(GuiPointerMoveEvent(0, 0, 9, 4, Modifiers.NONE)))
        val move = assertIs<GuiPointerMoveEvent>(child.events.last())
        assertEquals(9, move.localX)
        assertEquals(4, move.localY)

        assertFalse(
            router.dispatch(GuiPointerButtonEvent(MouseButton.LEFT, KeyAction.RELEASE, Modifiers.NONE, 0, 0, 9, 4, 1)),
        )
        val release = assertIs<GuiPointerButtonEvent>(child.events.last())
        assertEquals(9, release.localX)
        assertEquals(4, release.localY)
        assertEquals(null, router.pointerCapture.captured)
        assertFalse(child.isPressed)
    }

    @Test
    fun focusedWidgetReceivesKeyBeforeGlobalCommand() {
        val child = InteractionWidget("child", handler = { true })
        val root = TestContainer(listOf(child))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(child))
        var runs = 0
        val keymap =
            Keymap().bind(
                KeyChord(Key.S, Modifiers.CONTROL),
                Command("save", action = { runs++ }),
            )
        val router = EventRouter(root, focus, keymap = keymap)
        val key = GuiKeyEvent(Key.S, KeyAction.PRESS, Modifiers.CONTROL, false)

        assertTrue(router.dispatch(key))
        assertEquals(1, child.events.size)
        assertEquals(0, runs)

        val passive = InteractionWidget("passive", handler = { false })
        val passiveRoot = TestContainer(listOf(passive))
        passiveRoot.layout(Constraints.tight(IntSize(10, 5)))
        val passiveFocus = FocusManager(passiveRoot)
        assertTrue(passiveFocus.focus(passive))
        val passiveRouter = EventRouter(passiveRoot, passiveFocus, keymap = keymap)
        assertTrue(passiveRouter.dispatch(GuiKeyEvent(Key.S, KeyAction.PRESS, Modifiers.CONTROL, true)))
        assertEquals(1, runs)
    }

    @Test
    fun scrollTargetsWidgetUnderPointerThenFocusedWidget() {
        val child = InteractionWidget("child", width = 1, height = 1)
        val other = InteractionWidget("other", width = 1, height = 1)
        val root = TestContainer(listOf(child, other))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(other))
        val router = EventRouter(root, focus)

        assertFalse(router.dispatch(GuiScrollEvent(0.0, 1.0, 0, 0)))
        assertEquals(1, child.events.size)
        assertEquals(0, other.events.size)
    }
}
