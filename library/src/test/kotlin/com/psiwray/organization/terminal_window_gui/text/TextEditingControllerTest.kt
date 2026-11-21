package com.psiwray.organization.terminal_window_gui.text

import com.psiwray.organization.terminal_window_gui.input.InMemoryClipboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextEditingControllerTest {
    @Test
    fun insertDeleteAndReplaceSelection() {
        val controller = TextEditingController()
        controller.insert("abc")
        controller.moveCaret(-1)
        controller.insert("X")
        assertEquals("abXc", controller.text)
        assertEquals(3, controller.caret)
        controller.selection = TextSelection(1, 3)
        assertTrue(controller.replaceSelection("Q"))
        assertEquals("aQc", controller.text)
        assertEquals(2, controller.caret)
        assertTrue(controller.deleteBefore())
        assertEquals("ac", controller.text)
        assertTrue(controller.deleteAfter())
        assertEquals("a", controller.text)
    }

    @Test
    fun selectionKeepsDirectionAndSurrogatePairs() {
        val controller = TextEditingController("A😀B")
        controller.selection = TextSelection(3, 1)
        assertEquals(1, controller.selection.start)
        assertEquals(3, controller.selection.end)
        assertEquals(1, controller.caret)
        assertTrue(controller.deleteBefore())
        assertEquals("AB", controller.text)
        assertEquals(1, controller.caret)
        controller.insert("😀")
        assertEquals("A😀B", controller.text)
        controller.moveCaret(-1)
        assertEquals(1, controller.caret)
    }

    @Test
    fun maxLengthCountsCodePointsAndNeverSplits() {
        val controller = TextEditingController(maxLength = 2)
        controller.insert("😀")
        controller.insert("ab")
        assertEquals("😀a", controller.text)
        assertEquals(3, controller.caret)
        controller.deleteBefore()
        assertEquals("😀", controller.text)
        controller.deleteAfter()
        assertEquals("😀", controller.text)
    }

    @Test
    fun typingCoalescesAndOtherEditsBreakIt() {
        var now = 0L
        val controller = TextEditingController(clock = { now })
        controller.insert("a")
        now = 100_000_000L
        controller.insert("b")
        now = 200_000_000L
        controller.insert("c")
        assertTrue(controller.canUndo)
        assertTrue(controller.undo())
        assertEquals("", controller.text)
        assertEquals(0, controller.caret)
        assertTrue(controller.redo())
        assertEquals("abc", controller.text)

        val split = TextEditingController(clock = { now })
        split.insert("a")
        split.home()
        now += 600_000_000L
        split.insert("b")
        assertTrue(split.undo())
        assertEquals("a", split.text)
    }

    @Test
    fun undoRestoresSelectionAndRedoIsClearedByEdit() {
        val controller = TextEditingController("hello")
        controller.selection = TextSelection(1, 4)
        controller.replaceSelection("ey")
        assertEquals("heyo", controller.text)
        assertTrue(controller.undo())
        assertEquals("hello", controller.text)
        assertEquals(TextSelection(1, 4), controller.selection)
        controller.insert("!")
        assertFalse(controller.canRedo)
    }

    @Test
    fun wordMovementAndDeletionUseAsciiBoundaries() {
        val controller = TextEditingController("one.two three")
        controller.textEnd()
        controller.moveWord(-1)
        assertEquals(8, controller.caret)
        controller.moveWord(-1)
        assertEquals(4, controller.caret)
        controller.moveWord(1)
        assertEquals(7, controller.caret)
        controller.textEnd()
        controller.deleteWordBefore()
        assertEquals("one.two ", controller.text)
    }

    @Test
    fun clipboardHonoursSelectionAndUndo() {
        val clipboard = InMemoryClipboard()
        val controller = TextEditingController("abc")
        controller.selection = TextSelection(1, 3)
        assertTrue(controller.copy(clipboard))
        assertEquals("bc", clipboard.getText())
        assertTrue(controller.cut(clipboard))
        assertEquals("a", controller.text)
        assertTrue(controller.paste(clipboard))
        assertEquals("abc", controller.text)
        assertTrue(controller.undo())
        assertEquals("a", controller.text)
    }

    @Test
    fun lineMovementAndHomeEnd() {
        val controller = TextEditingController("abc\ndefg\nhi")
        controller.textHome()
        controller.lineDown()
        assertEquals(4, controller.caret)
        controller.end()
        assertEquals(8, controller.caret)
        controller.lineDown()
        assertEquals(11, controller.caret)
        controller.home()
        assertEquals(9, controller.caret)
        controller.textEnd()
        controller.lineUp()
        assertEquals(6, controller.caret)
    }
}
