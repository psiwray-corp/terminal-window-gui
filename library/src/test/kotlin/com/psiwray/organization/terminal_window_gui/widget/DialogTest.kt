package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogTest {
    @Test
    fun modalDialogTrapsFocusAndMapsKeys() {
        var result: DialogResult? = null
        val dialog =
            Dialog(
                title = "Edit",
                body = Label("hello"),
                actions =
                    listOf(
                        DialogAction("Cancel", cancel = true),
                        DialogAction("Save", default = true),
                    ),
                result = { result = it },
            )
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 12)
        harness.paint()
        val entry = dialog.show(harness.surface.overlays)
        harness.paint()
        assertTrue(entry.modal)
        assertTrue(dialog.bounds.width <= 40)
        assertTrue(dialog.bounds.height <= 12)
        assertTrue(
            harness.surface.focus.focused
                ?.let { it === dialog.bodyScroll } == true,
        )
        assertTrue(harness.key(Key.TAB))
        assertTrue(
            harness.surface.focus.focused
                ?.let { it === dialog.actionButtons[0] } == true,
        )
        assertTrue(harness.key(Key.TAB))
        assertTrue(
            harness.surface.focus.focused
                ?.let { it === dialog.actionButtons[1] } == true,
        )
        assertTrue(harness.key(Key.TAB))
        assertTrue(
            harness.surface.focus.focused
                ?.let { it === dialog.bodyScroll } == true,
        )
        harness.click(0, 0)
        assertTrue(
            harness.surface.overlays.entries
                .contains(entry),
        )
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(DialogResult.CANCELLED, result)
        assertTrue(
            harness.surface.overlays.entries
                .isEmpty(),
        )
    }

    @Test
    fun enterUsesDefaultActionAndResultIsDeliveredOnce() {
        var count = 0
        var result: DialogResult? = null
        val dialog =
            Dialog(
                title = "Save",
                body = Label("body"),
                actions =
                    listOf(
                        DialogAction("Cancel", cancel = true),
                        DialogAction("Save", default = true),
                    ),
                result = {
                    count++
                    result = it
                },
            )
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 10)
        dialog.show(harness.surface.overlays)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(DialogResult.CONFIRMED, result)
        assertEquals(1, count)
        dialog.finish(DialogResult.CANCELLED)
        dialog.finish(DialogResult.CONFIRMED)
        assertEquals(1, count)
    }

    @Test
    fun longBodyScrollsAndActionsStayVisible() {
        val lines = (0 until 30).joinToString("\n") { "line $it" }
        val dialog =
            Dialog(
                title = "Scroll",
                body = Label(lines),
                actions = listOf(DialogAction("Close", default = true)),
            )
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 10)
        dialog.show(harness.surface.overlays)
        harness.paint()
        assertTrue(dialog.bodyScroll.model.maxOffsetY > 0)
        assertTrue(
            dialog.actionButtons
                .first()
                .bounds.bottom <= dialog.bounds.height,
        )
        assertTrue(
            dialog.actionButtons
                .first()
                .bounds.y >= dialog.bodyScroll.bounds.bottom,
        )
    }

    @Test
    fun textFieldConsumesEnterBeforeDialogDefault() {
        var submits = 0
        val field = TextField("initial")
        field.onSubmit = { submits++ }
        var result: DialogResult? = null
        val dialog =
            Dialog(
                title = "Edit",
                body = field,
                actions = listOf(DialogAction("Save", default = true)),
                result = { result = it },
            )
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 10)
        dialog.show(harness.surface.overlays)
        assertTrue(harness.surface.focus.focus(field))
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, submits)
        assertEquals(null, result)
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(DialogResult.CANCELLED, result)
    }

    @Test
    fun addingDialogDirectlyToOverlayHostStillCloses() {
        var result: DialogResult? = null
        val dialog =
            Dialog(
                title = "Direct",
                body = Label("body"),
                actions = listOf(DialogAction("OK", default = true)),
                result = { result = it },
            )
        val harness = GuiTestHarness(Button("root"), columns = 30, rows = 10)
        val entry = harness.surface.overlays.add(dialog, modal = true)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(DialogResult.CONFIRMED, result)
        assertFalse(
            harness.surface.overlays.entries
                .contains(entry),
        )
    }

    @Test
    fun smallScreensClampDialogWithoutThrowing() {
        for ((columns, rows) in listOf(1 to 1, 1 to 5, 80 to 24, 240 to 67)) {
            val dialog =
                Dialog(
                    title = "A very long title",
                    body = Label("body"),
                    actions = listOf(DialogAction("OK", default = true), DialogAction("Cancel", cancel = true)),
                )
            val harness = GuiTestHarness(Button("root"), columns = columns, rows = rows)
            dialog.show(harness.surface.overlays)
            harness.paint()
            assertTrue(dialog.bounds.width <= columns)
            assertTrue(dialog.bounds.height <= rows)
            assertFalse(dialog.isFinished)
        }
    }
}
