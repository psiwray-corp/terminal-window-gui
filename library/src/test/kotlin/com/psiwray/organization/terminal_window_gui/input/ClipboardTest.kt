package com.psiwray.organization.terminal_window_gui.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClipboardTest {
    @Test
    fun inMemoryClipboardStoresAndClearsText() {
        val clipboard: Clipboard = InMemoryClipboard()
        assertNull(clipboard.getText())
        clipboard.setText("hello")
        assertEquals("hello", clipboard.getText())
        assertEquals("hello", (clipboard as InMemoryClipboard).text)
        clipboard.clear()
        assertNull(clipboard.getText())
    }

    @Test
    fun initialTextIsPreserved() {
        assertEquals("initial", InMemoryClipboard("initial").getText())
    }
}
