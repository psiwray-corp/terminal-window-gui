package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusTraversalTest {
    @Test
    fun tabOrderIsDepthFirstAndSkipsIneligibleWidgets() {
        val a = InteractionWidget("a")
        val b = InteractionWidget("b")
        val c = InteractionWidget("c")
        val disabled = InteractionWidget("disabled").also { it.enabled = false }
        val invisible = InteractionWidget("invisible").also { it.visible = false }
        val zero = InteractionWidget("zero")
        val root = TestContainer(listOf(a, b, c, disabled, invisible, zero))
        root.layout(Constraints.tight(IntSize(10, 5)))
        zero.arrange(Rect.EMPTY)

        assertEquals(listOf(a, b, c), FocusTraversal.order(root))

        val focus = FocusManager(root)
        assertTrue(focus.focusFirst())
        assertEquals(a, focus.focused)
        assertEquals(listOf(root, a), focus.focusPath)

        assertTrue(focus.focusNext())
        assertEquals(b, focus.focused)
        assertTrue(focus.focusNext())
        assertEquals(c, focus.focused)
        assertTrue(focus.focusNext())
        assertEquals(a, focus.focused)
        assertTrue(focus.focusPrevious())
        assertEquals(c, focus.focused)
        assertTrue(focus.focusLast())
        assertEquals(c, focus.focused)
    }

    @Test
    fun disabledFocusedWidgetCannotBeRestored() {
        val a = InteractionWidget("a")
        val b = InteractionWidget("b")
        val root = TestContainer(listOf(a, b))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(b))
        b.enabled = false
        assertNull(focus.focused)
        assertFalse(focus.restore(b))
        assertEquals(emptyList(), focus.focusPath)
    }

    @Test
    fun skipScopeExcludesSubtree() {
        val skippedChild = InteractionWidget("skipped")
        val skipped = TestContainer(listOf(skippedChild)).also { it.focusScope = FocusScope.SKIP }
        val visible = InteractionWidget("visible")
        val root = TestContainer(listOf(skipped, visible))
        root.layout(Constraints.tight(IntSize(10, 5)))

        assertEquals(listOf(visible), FocusTraversal.order(root))
    }

    @Test
    fun skipScopedWidgetCannotBeFocusedDirectly() {
        val skipped = InteractionWidget("skipped").also { it.focusScope = FocusScope.SKIP }
        val root = TestContainer(listOf(skipped))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertFalse(focus.focus(skipped))
    }

    @Test
    fun trapScopeWrapsInsideTheScope() {
        val a = InteractionWidget("a")
        val b = InteractionWidget("b")
        val trapped = TestContainer(listOf(a, b)).also { it.focusScope = FocusScope.TRAP }
        val outside = InteractionWidget("outside")
        val root = TestContainer(listOf(trapped, outside))
        root.layout(Constraints.tight(IntSize(10, 5)))

        val focus = FocusManager(root)
        assertTrue(focus.focusFirst())
        assertEquals(a, focus.focused)
        assertTrue(focus.focusNext())
        assertEquals(b, focus.focused)
        assertTrue(focus.focusNext())
        assertEquals(a, focus.focused)
        assertFalse(focus.focused === outside)
    }

    @Test
    fun explicitTrapRestoresPreviousFocus() {
        val first = InteractionWidget("first")
        val last = InteractionWidget("last")
        val root = TestContainer(listOf(first, last))
        root.layout(Constraints.tight(IntSize(10, 5)))
        val focus = FocusManager(root)
        assertTrue(focus.focus(first))

        focus.pushTrap(last)
        assertTrue(focus.focusFirst())
        assertEquals(last, focus.focused)
        focus.popTrap(last)
        assertTrue(focus.restore(first))
        assertEquals(first, focus.focused)
    }
}
