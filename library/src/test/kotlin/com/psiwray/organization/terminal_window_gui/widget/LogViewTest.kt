package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogViewTest {
    @Test
    fun followTailTracksAppendedLinesUntilScrolledUp() {
        val view = LogView(listOf("one", "two", "three"))
        val harness = GuiTestHarness(view, columns = 10, rows = 2)
        harness.paint()
        assertTrue(view.followTail)
        view.append("four")
        harness.paint()
        assertTrue(view.followTail)
        assertTrue(harness.rows()[1].contains("four"))

        view.scroll.scrollTo(0, 0)
        view.append("five")
        harness.paint()
        assertFalse(view.followTail)
        assertTrue(view.scrollLocked)
        assertTrue(harness.rows()[0].contains("one"))

        view.scroll.end()
        view.append("six")
        harness.paint()
        assertTrue(view.followTail)
        assertTrue(harness.rows()[1].contains("six"))
    }

    @Test
    fun wheelScrollsWithoutEditing() {
        val view = LogView((1..10).map { "line $it" })
        val harness = GuiTestHarness(view, columns = 10, rows = 3)
        harness.paint()
        val start = view.scroll.offsetY
        assertTrue(harness.scroll(0, -1, 0, 0))
        assertTrue(view.scroll.offsetY < start)
    }

    @Test
    fun controllerSourceIsReadOnly() {
        val controller = TextEditingController("x\ny")
        val view = LogView(controller = controller)
        val harness = GuiTestHarness(view, columns = 5, rows = 2)
        harness.paint()
        assertTrue(harness.rows()[0].contains("x"))
        assertTrue(harness.rows()[1].contains("y"))
        controller.setText("z")
        harness.paint()
        assertTrue(harness.rows()[0].contains("z"))
    }
}
