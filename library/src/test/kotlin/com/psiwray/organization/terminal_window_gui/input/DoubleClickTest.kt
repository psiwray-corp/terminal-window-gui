package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleClickTest {
    @Test
    fun countsDoubleAndTripleClicksAndResetsAfterTimeout() {
        var now = 0L
        val child = InteractionWidget("child")
        val root = TestContainer(listOf(child), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root), clock = { now })

        click(router, 0, 0)
        now += 100_000_000
        click(router, 0, 0)
        now += 100_000_000
        click(router, 0, 0)
        assertEquals(listOf(1, 2, 3), clickCounts(child))

        now += 400_000_001
        click(router, 0, 0)
        assertEquals(listOf(1, 2, 3, 1), clickCounts(child))
    }

    @Test
    fun adjacentCellToleranceAndMoveReset() {
        var now = 0L
        val child = InteractionWidget("child")
        val root = TestContainer(listOf(child), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root), clock = { now })

        click(router, 0, 0)
        now += 100_000_000
        click(router, 1, 0)
        now += 100_000_000
        click(router, 1, 1)
        assertEquals(listOf(1, 2, 3), clickCounts(child))

        router.dispatch(GuiPointerMoveEvent(0, 0, 5, 5, Modifiers.NONE))
        now += 100_000_000
        click(router, 5, 5)
        assertEquals(listOf(1, 2, 3, 1), clickCounts(child))
    }

    @Test
    fun differentButtonResetsCount() {
        var now = 0L
        val child = InteractionWidget("child")
        val root = TestContainer(listOf(child), fillChildren = true)
        root.layout(Constraints.tight(IntSize(10, 5)))
        val router = EventRouter(root, FocusManager(root), clock = { now })

        click(router, 0, 0, MouseButton.LEFT)
        now += 100_000_000
        click(router, 0, 0, MouseButton.RIGHT)
        assertEquals(listOf(1, 1), clickCounts(child))
    }

    private fun click(
        router: EventRouter,
        x: Int,
        y: Int,
        button: MouseButton = MouseButton.LEFT,
    ) {
        router.dispatch(GuiPointerButtonEvent(button, KeyAction.PRESS, Modifiers.NONE, 0, 0, x, y, 1))
    }

    private fun clickCounts(widget: InteractionWidget): List<Int> =
        widget.events.filterIsInstance<GuiPointerButtonEvent>().map { it.clickCount }
}
