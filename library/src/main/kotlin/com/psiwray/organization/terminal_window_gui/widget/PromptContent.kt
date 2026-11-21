package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize

/** Internal label, field, and optional error stack for [PromptDialog]. */
internal class PromptContent(
    label: String,
    initialValue: String,
    val password: Boolean,
    val readOnly: Boolean,
) {
    val field: TextField =
        TextField(
            text = initialValue,
            mask = if (password) '*' else null,
            readOnly = readOnly,
        )
    private val errorLabel: Label = Label("")
    val column: Column =
        Column(
            mainAxisSize = MainAxisSize.MIN,
            children = listOf(Label(label), field),
        )

    var errorText: String? = null
        private set

    fun setError(value: String?) {
        val text = value?.takeIf { it.isNotEmpty() }
        if (errorText == text) {
            return
        }
        errorText = text
        if (text == null) {
            if (column.children.contains(errorLabel)) {
                column.remove(errorLabel)
            }
        } else {
            errorLabel.text = text
            if (!column.children.contains(errorLabel)) {
                column.add(errorLabel)
            }
        }
    }
}
