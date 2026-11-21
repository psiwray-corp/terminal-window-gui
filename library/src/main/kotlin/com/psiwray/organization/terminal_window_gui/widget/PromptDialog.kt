package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize

/**
 * Single-field input dialog.
 *
 * The field supports masking, read-only text, and display-only validation
 * strings. The application owns validation; this dialog only displays [error].
 * Enter in the field submits the confirm action.
 *
 * @property label Field label.
 * @property field Editable field.
 * @since 1.0.0
 */
public class PromptDialog private constructor(
    title: String,
    private val prompt: PromptContent,
    result: (String?) -> Unit,
    confirmLabel: String,
    cancelLabel: String,
) : Dialog(
        title = title,
        body = prompt.column,
        actions =
            listOf(
                DialogAction(cancelLabel, cancel = true),
                DialogAction(confirmLabel, default = true),
            ),
        result = { dialogResult ->
            result(if (dialogResult == DialogResult.CONFIRMED) prompt.field.controller.text else null)
        },
    ) {
    /** Editable field. @since 1.0.0 */
    public val field: TextField get() = prompt.field

    /** Current field text. @since 1.0.0 */
    public val text: String get() = prompt.field.controller.text

    /** Current display-only error, or `null`. @since 1.0.0 */
    public var error: String?
        get() = prompt.errorText
        set(value) {
            prompt.setError(value)
        }

    /** Whether the field masks its text. @since 1.0.0 */
    public val password: Boolean get() = prompt.password

    /** Whether the field is read-only. @since 1.0.0 */
    public val readOnly: Boolean get() = prompt.readOnly

    init {
        prompt.field.onSubmit = { confirm() }
    }

    override fun show(
        overlays: com.psiwray.organization.terminal_window_gui.input.OverlayHost,
        anchor: com.psiwray.organization.terminal_window_gui.input.OverlayAnchor,
    ): com.psiwray.organization.terminal_window_gui.input.OverlayEntry {
        val entry = super.show(overlays, anchor)
        overlays.focus?.focus(prompt.field)
        return entry
    }

    /** Child-first constructor using an `onResult` callback name. @since 1.0.0 */
    public constructor(
        title: String,
        label: String,
        onResult: (String?) -> Unit,
    ) : this(title, label, "", false, false, "OK", "Cancel", onResult)

    /** Primary constructor. @since 1.0.0 */
    public constructor(
        title: String,
        label: String,
        initialValue: String = "",
        password: Boolean = false,
        readOnly: Boolean = false,
        confirmLabel: String = "OK",
        cancelLabel: String = "Cancel",
        result: (String?) -> Unit = {},
    ) : this(
        title,
        PromptContent(label, initialValue, password, readOnly),
        result,
        confirmLabel,
        cancelLabel,
    )
}
