package com.psiwray.organization.terminal_window_gui.style

/**
 * Foreground and background colors for one terminal cell.
 *
 * The host has no text attributes beyond color.
 *
 * @property fg Foreground ARGB color.
 * @property bg Background ARGB color.
 * @since 1.0.0
 */
public data class Style(
    public val fg: Int,
    public val bg: Int,
) {
    /**
     * Resolves [state] against [theme].
     *
     * @since 1.0.0
     */
    public fun resolve(
        theme: Theme,
        state: WidgetState,
    ): Style = theme.style(state)
}
