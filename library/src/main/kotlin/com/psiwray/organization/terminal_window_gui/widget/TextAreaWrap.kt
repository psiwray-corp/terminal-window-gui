package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.paint.TextWrap

/**
 * Wrapping policy for [TextArea] and [LogView].
 *
 * @since 1.0.0
 */
public enum class TextAreaWrap {
    /** One visual row per logical line; horizontal scrolling. @since 1.0.0 */
    NONE,

    /** Break at spaces, hard-breaking words wider than the viewport. @since 1.0.0 */
    WORD,

    /** Break at every code point. @since 1.0.0 */
    CHAR,
    ;

    internal fun toTextWrap(): TextWrap =
        when (this) {
            NONE -> TextWrap.NONE
            WORD -> TextWrap.WORD
            CHAR -> TextWrap.CHAR
        }
}
