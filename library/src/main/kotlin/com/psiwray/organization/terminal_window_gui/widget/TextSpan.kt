package com.psiwray.organization.terminal_window_gui.widget

/**
 * One color run inside a [RichLabel].
 *
 * @property text Span text.
 * @property fg Optional foreground; `null` uses the theme text color.
 * @property bg Optional background; `null` uses the widget/theme background.
 * @since 1.0.0
 */
public data class TextSpan(
    public val text: String,
    public val fg: Int? = null,
    public val bg: Int? = null,
)
