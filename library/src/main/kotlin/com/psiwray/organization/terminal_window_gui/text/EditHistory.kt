package com.psiwray.organization.terminal_window_gui.text

/** Bounded undo and redo stacks. */
internal class EditHistory(
    private val limit: Int = 200,
) {
    private val undoStack: ArrayDeque<TextEdit> = ArrayDeque()
    private val redoStack: ArrayDeque<TextEdit> = ArrayDeque()

    init {
        require(limit > 0) { "limit must be positive, got $limit" }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun record(edit: TextEdit) {
        undoStack.addLast(edit)
        if (undoStack.size > limit) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: TextEdit): TextEdit? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: TextEdit): TextEdit? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        if (undoStack.size > limit) {
            undoStack.removeFirst()
        }
        return next
    }
}
