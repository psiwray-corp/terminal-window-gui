package com.psiwray.organization.terminal_window_gui.style

/**
 * Immutable theme tokens as ARGB colors.
 *
 * Focus must remain distinguishable without color; widget implementations use
 * border markers and cursors in addition to these tokens.
 *
 * @property background Surface background.
 * @property surface Normal widget background.
 * @property surfaceAlt Alternate or hover background.
 * @property text Primary text.
 * @property textMuted Secondary text.
 * @property textDisabled Disabled text.
 * @property accent Accent color.
 * @property success Success color.
 * @property warning Warning color.
 * @property error Error color.
 * @property border Normal border.
 * @property borderFocused Focused border.
 * @property selectionBackground Selection background.
 * @property selectionForeground Selection text.
 * @property caret Caret color.
 * @property shadow Shadow color.
 * @since 1.0.0
 */
public data class Theme(
    public val background: Int,
    public val surface: Int,
    public val surfaceAlt: Int,
    public val text: Int,
    public val textMuted: Int,
    public val textDisabled: Int,
    public val accent: Int,
    public val success: Int,
    public val warning: Int,
    public val error: Int,
    public val border: Int,
    public val borderFocused: Int,
    public val selectionBackground: Int,
    public val selectionForeground: Int,
    public val caret: Int,
    public val shadow: Int,
) {
    /**
     * Resolves colors for [state].
     *
     * @since 1.0.0
     */
    public fun style(state: WidgetState): Style =
        when (state) {
            WidgetState.NORMAL -> Style(text, surface)
            WidgetState.FOCUSED -> Style(altForeground(), surfaceAlt)
            WidgetState.HOVERED -> Style(altForeground(), surfaceAlt)
            WidgetState.PRESSED -> Style(selectionForeground, accent)
            WidgetState.DISABLED -> Style(textDisabled, surface)
            WidgetState.SELECTED -> Style(selectionForeground, selectionBackground)
            WidgetState.ERROR -> Style(error, surface)
        }

    private fun altForeground(): Int = if (text == surfaceAlt) background else text

    /**
     * Alias for [style].
     *
     * @since 1.0.0
     */
    public fun styleFor(state: WidgetState): Style = style(state)
}
