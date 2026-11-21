package com.psiwray.organization.terminal_window_gui.input

import kotlin.jvm.JvmName

/**
 * Single-thread-affine in-memory clipboard.
 *
 * @since 1.0.0
 */
public class InMemoryClipboard(
    initialText: String? = null,
) : Clipboard {
    /** Current clipboard text, or `null`. @since 1.0.0 */
    @get:JvmName("textValue")
    public var text: String? = initialText
        private set

    override fun getText(): String? = text

    override fun setText(text: String) {
        this.text = text
    }

    /** Clears the clipboard. @since 1.0.0 */
    public fun clear() {
        text = null
    }
}
