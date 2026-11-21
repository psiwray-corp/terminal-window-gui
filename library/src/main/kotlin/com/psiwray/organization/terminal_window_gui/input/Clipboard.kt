package com.psiwray.organization.terminal_window_gui.input

/**
 * Text clipboard used by editable widgets.
 *
 * The library never accesses the operating-system clipboard; callers supply an
 * implementation.
 *
 * @since 1.0.0
 */
public interface Clipboard {
    /** Returns the current clipboard text, or `null` when empty. @since 1.0.0 */
    public fun getText(): String?

    /** Replaces the clipboard text. @since 1.0.0 */
    public fun setText(text: String)
}
