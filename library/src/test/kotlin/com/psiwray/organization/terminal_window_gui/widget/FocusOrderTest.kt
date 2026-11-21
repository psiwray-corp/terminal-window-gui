package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FocusOrderTest {
    @Test
    fun tabSkipsMenuBarPassiveBannerAndStructuralScrollView() {
        val button = Button("OK")
        val root =
            Column(
                children =
                    listOf(
                        MenuBar(listOf(Menu("File"))),
                        Banner(message = "notice", closable = false),
                        ScrollView(Label("content")),
                        button,
                    ),
            )
        val harness = GuiTestHarness(root, columns = 40, rows = 8)
        harness.paint()

        assertTrue(harness.key(Key.TAB))
        assertSame(button, harness.focusPath().last())
        assertTrue(harness.surface.needsPaint)
    }
}
