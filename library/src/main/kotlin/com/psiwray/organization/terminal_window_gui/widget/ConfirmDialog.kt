package com.psiwray.organization.terminal_window_gui.widget

/**
 * Confirmation dialog with confirm and cancel actions.
 *
 * The confirm action is the default; Escape and the cancel action deliver
 * `false`. The Boolean callback runs once.
 *
 * @property message Body message.
 * @property confirmLabel Confirm button label.
 * @property cancelLabel Cancel button label.
 * @since 1.0.0
 */
public class ConfirmDialog(
    title: String,
    message: String,
    public val confirmLabel: String = "OK",
    public val cancelLabel: String = "Cancel",
    result: (Boolean) -> Unit = {},
) : Dialog(
        title = title,
        body = Label(message),
        actions =
            listOf(
                DialogAction(cancelLabel, cancel = true),
                DialogAction(confirmLabel, default = true),
            ),
        result = { dialogResult -> result(dialogResult == DialogResult.CONFIRMED) },
    ) {
    /** Child-first constructor with the common labels. @since 1.0.0 */
    public constructor(
        title: String,
        message: String,
        onResult: (Boolean) -> Unit,
    ) : this(title, message, "OK", "Cancel", onResult)
}
