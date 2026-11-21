package com.psiwray.organization.terminal_window_gui.widget

/**
 * One dialog action button.
 *
 * A default action is activated by Enter when no focused widget consumes it.
 * A cancel action is activated by Escape and maps to
 * [DialogResult.CANCELLED]. Other actions map to [DialogResult.CONFIRMED]
 * unless [result] overrides the outcome.
 *
 * @property label Button label.
 * @property default Whether this is the default action.
 * @property cancel Whether this action cancels the dialog.
 * @property result Optional explicit result.
 * @property onActivate Optional callback run before the result is delivered.
 * @since 1.0.0
 */
public data class DialogAction(
    public val label: String,
    public val default: Boolean = false,
    public val cancel: Boolean = false,
    public val result: DialogResult? = null,
    public val onActivate: () -> Unit = {},
)
