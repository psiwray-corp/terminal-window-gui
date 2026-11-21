package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfirmPromptTest {
    @Test
    fun confirmDialogReturnsBoolean() {
        var result: Boolean? = null
        val dialog = ConfirmDialog("Delete", "Delete file?", result = { result = it })
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        dialog.show(harness.surface.overlays)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(true, result)

        var cancelled: Boolean? = null
        val second = ConfirmDialog("Delete", "Delete file?", result = { cancelled = it })
        second.show(harness.surface.overlays)
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(false, cancelled)
    }

    @Test
    fun promptSubmitsTextAndDisplaysValidation() {
        var result: String? = "unchanged"
        val dialog = PromptDialog("Name", "Name", initialValue = "Ada", result = { result = it })
        val harness = GuiTestHarness(Button("root"), columns = 50, rows = 12)
        dialog.show(harness.surface.overlays)
        harness.paint()
        assertEquals(dialog.field, harness.surface.focus.focused)
        assertEquals("Ada", dialog.text)
        assertTrue(harness.char('!'))
        assertEquals("Ada!", dialog.text)
        dialog.error = "Already used"
        harness.paint()
        assertTrue(harness.rows().any { it.contains("Already used") })
        assertTrue(harness.key(Key.ENTER))
        assertEquals("Ada!", result)

        var cancelled: String? = "not null"
        val second = PromptDialog("Name", "Name", initialValue = "Ada", result = { cancelled = it })
        second.show(harness.surface.overlays)
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(null, cancelled)
    }

    @Test
    fun promptSupportsPasswordAndReadOnly() {
        val dialog = PromptDialog("Secret", "Secret", initialValue = "value", password = true, readOnly = true)
        assertTrue(dialog.password)
        assertTrue(dialog.readOnly)
        assertEquals('*', dialog.field.mask)
        val harness = GuiTestHarness(Button("root"), columns = 40, rows = 10)
        dialog.show(harness.surface.overlays)
        assertTrue(harness.char('x'))
        assertEquals("value", dialog.text)
        harness.paint()
        assertTrue(harness.rows().any { it.contains("***") })
    }
}
