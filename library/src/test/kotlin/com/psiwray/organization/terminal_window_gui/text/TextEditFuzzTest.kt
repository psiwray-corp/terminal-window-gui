package com.psiwray.organization.terminal_window_gui.text

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TextEditFuzzTest {
    @Test
    fun randomEditsMatchReferenceStringModel() {
        val random = Random(0x5EED)
        val controller = TextEditingController()
        var expected = ""
        repeat(600) {
            val boundaries = boundaries(expected)
            when (random.nextInt(5)) {
                0 -> {
                    val start = boundaries[random.nextInt(boundaries.size)]
                    val end = boundaries[random.nextInt(boundaries.size)]
                    val selection = if (random.nextBoolean()) TextSelection(start, end) else TextSelection(end, start)
                    val inserted = randomText(random)
                    controller.selection = selection
                    controller.insert(inserted)
                    expected =
                        expected.substring(0, selection.start) +
                        inserted +
                        expected.substring(selection.end)
                }

                1 -> {
                    val index = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(index, index)
                    val count = random.nextInt(1, 4)
                    controller.deleteBefore(count)
                    val start = boundariesOfPrefix(expected, index, count, forward = false)
                    expected = expected.substring(0, start) + expected.substring(index)
                }

                2 -> {
                    val index = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(index, index)
                    val count = random.nextInt(1, 4)
                    controller.deleteAfter(count)
                    val end = boundariesOfPrefix(expected, index, count, forward = true)
                    expected = expected.substring(0, index) + expected.substring(end)
                }

                3 -> {
                    val start = boundaries[random.nextInt(boundaries.size)]
                    val end = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(start, end)
                    assertEquals(expected, controller.text)
                    assertEquals(minOf(start, end), controller.selection.start)
                    assertEquals(maxOf(start, end), controller.selection.end)
                }

                else -> {
                    val index = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(index, index)
                }
            }
            assertEquals(expected, controller.text)
            assertBoundary(controller.text, controller.selection.anchor)
            assertBoundary(controller.text, controller.selection.focus)
        }
    }

    @Test
    fun randomEditsAndUndoRedoMatchReferenceHistory() {
        val random = Random(0xC0FFEE)
        var now = 0L
        val controller = TextEditingController(clock = { now })
        var expected = ""
        val undo = ArrayDeque<String>()
        val redo = ArrayDeque<String>()
        repeat(500) {
            val boundaries = boundaries(expected)
            when (random.nextInt(7)) {
                0, 1 -> {
                    val start = boundaries[random.nextInt(boundaries.size)]
                    val end = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(start, end)
                    val inserted = if (random.nextBoolean()) randomText(random) else randomCodePoint(random)
                    if (inserted.isNotEmpty() || start != end) {
                        undo.addLast(expected)
                        redo.clear()
                        controller.insert(inserted)
                        expected =
                            expected.substring(0, minOf(start, end)) + inserted + expected.substring(maxOf(start, end))
                        now += 600_000_000L
                    }
                }

                2 -> {
                    val index = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(index, index)
                    val count = random.nextInt(1, 3)
                    if (index > 0) {
                        undo.addLast(expected)
                        redo.clear()
                        controller.deleteBefore(count)
                        expected = deleteBefore(expected, index, count)
                        now += 600_000_000L
                    }
                }

                3 -> {
                    val index = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(index, index)
                    val count = random.nextInt(1, 3)
                    if (index < expected.length) {
                        undo.addLast(expected)
                        redo.clear()
                        controller.deleteAfter(count)
                        expected = deleteAfter(expected, index, count)
                        now += 600_000_000L
                    }
                }

                4 -> {
                    val start = boundaries[random.nextInt(boundaries.size)]
                    val end = boundaries[random.nextInt(boundaries.size)]
                    controller.selection = TextSelection(start, end)
                }

                5 -> {
                    if (controller.canUndo) {
                        redo.addLast(expected)
                        controller.undo()
                        undo.removeLastOrNull()?.let { expected = it }
                        now += 600_000_000L
                    }
                }

                else -> {
                    if (controller.canRedo) {
                        undo.addLast(expected)
                        controller.redo()
                        redo.removeLastOrNull()?.let { expected = it }
                        now += 600_000_000L
                    }
                }
            }
            assertEquals(expected, controller.text)
        }
    }

    private fun randomCodePoint(random: Random): String {
        val pool = listOf("a", "Z", "0", " ", "é", "😀")
        return pool[random.nextInt(pool.size)]
    }

    private fun deleteBefore(
        text: String,
        index: Int,
        count: Int,
    ): String {
        val start = boundariesOfPrefix(text, index, count, forward = false)
        return text.substring(0, start) + text.substring(index)
    }

    private fun deleteAfter(
        text: String,
        index: Int,
        count: Int,
    ): String {
        val end = boundariesOfPrefix(text, index, count, forward = true)
        return text.substring(0, index) + text.substring(end)
    }

    private fun randomText(random: Random): String {
        val pool = listOf("a", "Z", "0", "_", ".", " ", "é", "😀", "界")
        val result = StringBuilder()
        repeat(random.nextInt(0, 4)) {
            result.append(pool[random.nextInt(pool.size)])
        }
        return result.toString()
    }

    private fun boundaries(text: String): List<Int> {
        val result = ArrayList<Int>()
        var index = 0
        while (index < text.length) {
            result.add(index)
            index = Caret.next(text, index)
        }
        result.add(text.length)
        return result
    }

    private fun boundariesOfPrefix(
        text: String,
        index: Int,
        count: Int,
        forward: Boolean,
    ): Int {
        var position = index
        if (forward) {
            repeat(count) {
                if (position < text.length) {
                    position = Caret.next(text, position)
                }
            }
        } else {
            repeat(count) {
                if (position > 0) {
                    position = Caret.previous(text, position)
                }
            }
        }
        return position
    }

    private fun assertBoundary(
        text: String,
        index: Int,
    ) {
        assertEquals(index, Caret.snap(text, index))
    }
}
