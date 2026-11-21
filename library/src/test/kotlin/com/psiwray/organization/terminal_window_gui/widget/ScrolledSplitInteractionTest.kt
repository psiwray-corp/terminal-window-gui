package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrolledSplitInteractionTest {
    private fun paneInScrolledPanel(): Pair<SplitPane, GuiTestHarness> {
        val pane = SplitPane(SplitOrientation.HORIZONTAL, Label("left"), Label("right"))
        val section =
            Panel(
                title = "Containers",
                child = Column(children = listOf(SizedBox(height = 10), SizedBox(height = 5, child = pane))),
            )
        val page = Column(children = listOf(SizedBox(height = 10), section))
        val view = ScrollView(page)
        val harness = GuiTestHarness(view, columns = 20, rows = 10)
        harness.paint()
        view.model.scrollTo(0, view.model.maxOffsetY)
        harness.paint()
        return pane to harness
    }

    @Test
    fun dividerDragUsesScreenCoordinatesInsideScrolledContent() {
        val (pane, harness) = paneInScrolledPanel()
        val origin = pane.screenOriginPoint()
        assertTrue(harness.drag(origin.x + pane.dividerPosition, origin.y + 2, origin.x + 8, origin.y + 2))
        harness.paint()
        assertEquals(8, pane.dividerPosition)
    }

    @Test
    fun dividerIsReachableByFocusInsideScrolledContent() {
        val (pane, harness) = paneInScrolledPanel()
        assertTrue(harness.surface.focus.focus(pane))
        assertTrue(harness.key(com.psiwray.organization.terminal_window.input.Key.RIGHT))
        harness.paint()
        assertEquals(6, pane.dividerPosition)
    }

    @Test
    fun promptShowsWholeInitialValue() {
        val dialog = PromptDialog("Prompt", "Enter a value", initialValue = "Terminal")
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 12)
        dialog.show(harness.surface.overlays)
        harness.paint()
        assertEquals("Terminal", dialog.text)
        assertTrue(harness.rows().any { it.contains("Terminal") })
    }

    @Test
    fun dialogBodyScrollsInsideOverlay() {
        val lines = (0 until 40).joinToString("\n") { "line $it" }
        val dialog =
            Dialog(title = "Scroll", body = Label(lines), actions = listOf(DialogAction("Close", default = true)))
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 10)
        val entry = dialog.show(harness.surface.overlays)
        harness.paint()
        harness.move(entry.screenRect.x + 2, entry.screenRect.y + 2)
        harness.scroll(0, 3)
        assertTrue(dialog.bodyScroll.model.offsetY > 0)
    }
}
