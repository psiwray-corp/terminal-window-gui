package com.psiwray.organization.terminal_window_gui.text

/** One reversible editing snapshot. */
internal data class TextEdit(
    val text: String,
    val selection: TextSelection,
)

/** Whether a new edit may join the current undo entry. */
internal enum class EditKind {
    TYPING,
    OTHER,
}
