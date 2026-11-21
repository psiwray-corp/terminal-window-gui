package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.widget.testContext
import com.psiwray.organization.terminal_window_gui.widget.testMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OverlayTest {
    @Test
    fun placementFlipsAboveAndClampsToScreen() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 15, offsetY = 7)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val host = OverlayHost(screen = Rect(0, 0, 20, 10))
        val entry =
            host.add(
                InteractionWidget("popup", width = 5, height = 3),
                anchor = OverlayAnchor.Widget(anchor),
                placement = OverlayPlacement.BELOW,
            )

        assertEquals(Rect(15, 4, 5, 3), entry.screenRect)
    }

    @Test
    fun placementFlipsRightToLeftAndClamps() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 18, offsetY = 2)
        root.layout(Constraints.tight(IntSize(20, 4)))
        val host = OverlayHost(screen = Rect(0, 0, 20, 10))
        val entry =
            host.add(
                InteractionWidget("popup", width = 4, height = 2),
                anchor = OverlayAnchor.Widget(anchor),
                placement = OverlayPlacement.RIGHT,
            )

        assertEquals(Rect(14, 2, 4, 2), entry.screenRect)
    }

    @Test
    fun clickOutsideAndEscapeDismissNonModalEntry() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 5, offsetY = 5)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        val host = OverlayHost(screen = Rect(0, 0, 20, 10), focus = focus)
        var dismissed = 0
        host.add(
            InteractionWidget("popup", width = 3, height = 2),
            anchor = OverlayAnchor.Widget(anchor),
            dismissPolicy = DismissPolicy.CLICK_OUTSIDE,
            onDismiss = { dismissed++ },
        )
        val router = EventRouter(root, focus, overlays = host)

        press(router, 1, 1)
        assertEquals(0, host.entries.size)
        assertEquals(1, dismissed)

        host.add(
            InteractionWidget("popup2", width = 3, height = 2),
            anchor = OverlayAnchor.Widget(anchor),
            dismissPolicy = DismissPolicy.ESCAPE,
            onDismiss = { dismissed++ },
        )
        assertTrue(router.dispatch(GuiKeyEvent(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE, false)))
        assertEquals(0, host.entries.size)
        assertEquals(2, dismissed)
    }

    @Test
    fun clickInsideOverlayDoesNotDismiss() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 5, offsetY = 5)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val host = OverlayHost(screen = Rect(0, 0, 20, 10))
        val popup = InteractionWidget("popup", width = 4, height = 2)
        val entry =
            host.add(
                popup,
                anchor = OverlayAnchor.Widget(anchor),
                dismissPolicy = DismissPolicy.CLICK_OUTSIDE,
            )
        val router = EventRouter(root, overlays = host)

        press(router, entry.screenRect.x + 1, entry.screenRect.y + 1)
        assertEquals(1, host.entries.size)
        assertEquals(1, popup.events.size)
    }

    @Test
    fun topmostOverlayWinsHitTesting() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 2, offsetY = 2)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val host = OverlayHost(screen = Rect(0, 0, 20, 10))
        val lower = InteractionWidget("lower", width = 4, height = 3)
        val upper = InteractionWidget("upper", width = 4, height = 3)
        host.add(lower, anchor = OverlayAnchor.Widget(anchor))
        host.add(upper, anchor = OverlayAnchor.Widget(anchor))

        assertEquals(2, host.entries.size)
        assertEquals(upper, host.hitTest(3, 3)?.widget)
    }

    @Test
    fun modalOverlayTrapsFocusBlocksBelowAndRestoresFocus() {
        val first = InteractionWidget("first")
        val root = TestContainer(listOf(first))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(first))
        val host = OverlayHost(screen = Rect(0, 0, 10, 5), focus = focus)
        val modalChild = InteractionWidget("modalChild")
        val modal = TestContainer(listOf(modalChild))
        val entry =
            host.add(
                modal,
                anchor = OverlayAnchor.Rect(Rect(2, 1, 4, 3)),
                modal = true,
                dismissPolicy = DismissPolicy.BOTH,
            )
        val router = EventRouter(root, focus, overlays = host)

        assertEquals(modalChild, focus.focused)
        assertTrue(focus.focusNext())
        assertEquals(modalChild, focus.focused)

        val rootEventsBefore = root.events.size
        press(router, 0, 0)
        assertEquals(rootEventsBefore, root.events.size)

        assertTrue(router.dispatch(GuiKeyEvent(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE, false)))
        assertNull(host.hitTest(2, 1))
        assertEquals(first, focus.focused)
        assertFalse(entry.widget.isAttached)
    }

    @Test
    fun overlayPaintsAfterRootAtScreenPosition() {
        val anchor = InteractionWidget("anchor")
        val root = TestContainer(listOf(anchor), offsetX = 5, offsetY = 5)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val host = OverlayHost(screen = Rect(0, 0, 20, 10))
        val entry =
            host.add(
                InteractionWidget("popup", width = 3, height = 2),
                anchor = OverlayAnchor.Widget(anchor),
            )
        val matrix = testMatrix(20, 10)
        host.paint(testContext(matrix))
        assertNotNull(entry.screenRect)
        assertEquals('#'.code, matrix.codePointAt(entry.screenRect.x, entry.screenRect.y))
    }

    private fun press(
        router: EventRouter,
        x: Int,
        y: Int,
    ) {
        router.dispatch(GuiPointerButtonEvent(MouseButton.LEFT, KeyAction.PRESS, Modifiers.NONE, 0, 0, x, y, 1))
    }
}
