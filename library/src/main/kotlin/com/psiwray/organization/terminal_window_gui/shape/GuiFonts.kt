package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window.font.BitmapFont

/**
 * Access to the fonts bundled with the library.
 *
 * @since 1.0.0
 */
public object GuiFonts {
    private const val BUNDLED_FONT = "/fonts/terminal-window-gui-8x16.twf"

    /**
     * Parses the bundled 8x16 CC0 font.
     *
     * The returned font covers printable ASCII, every standard shape primary,
     * the reserved heavy box set, and every ASCII shape fallback.
     *
     * @throws IllegalStateException if the bundled resource is missing.
     * @since 1.0.0
     */
    public fun bundled(): BitmapFont {
        val stream =
            GuiFonts::class.java.getResourceAsStream(BUNDLED_FONT)
                ?: error("bundled font is missing: $BUNDLED_FONT")
        return stream.use { BitmapFont.parse(it) }
    }
}
