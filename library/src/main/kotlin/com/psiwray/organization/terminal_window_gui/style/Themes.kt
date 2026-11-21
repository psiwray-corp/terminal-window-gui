package com.psiwray.organization.terminal_window_gui.style

/**
 * Factory methods for the built-in themes.
 *
 * @since 1.0.0
 */
public object Themes {
    /**
     * Dark terminal-friendly palette.
     *
     * @since 1.0.0
     */
    public fun dark(): Theme =
        Theme(
            background = 0xFF101214.toInt(),
            surface = 0xFF171A1D.toInt(),
            surfaceAlt = 0xFF22272B.toInt(),
            text = 0xFFE6E9EC.toInt(),
            textMuted = 0xFF9AA3AB.toInt(),
            textDisabled = 0xFF5C656D.toInt(),
            accent = 0xFF5FA8D3.toInt(),
            success = 0xFF6FBF73.toInt(),
            warning = 0xFFD6A84B.toInt(),
            error = 0xFFE06C75.toInt(),
            border = 0xFF3A4249.toInt(),
            borderFocused = 0xFF8CC8F0.toInt(),
            selectionBackground = 0xFF2F5D7C.toInt(),
            selectionForeground = 0xFFFFFFFF.toInt(),
            caret = 0xFF8CC8F0.toInt(),
            shadow = 0xFF000000.toInt(),
        )

    /**
     * Light palette.
     *
     * @since 1.0.0
     */
    public fun light(): Theme =
        Theme(
            background = 0xFFF7F7F5.toInt(),
            surface = 0xFFFFFFFF.toInt(),
            surfaceAlt = 0xFFECEEF0.toInt(),
            text = 0xFF1B1F23.toInt(),
            textMuted = 0xFF5E6870.toInt(),
            textDisabled = 0xFF9AA3AB.toInt(),
            accent = 0xFF1F6FB2.toInt(),
            success = 0xFF2E7D32.toInt(),
            warning = 0xFF8A5A00.toInt(),
            error = 0xFFB3261E.toInt(),
            border = 0xFFC7CDD2.toInt(),
            borderFocused = 0xFF1F6FB2.toInt(),
            selectionBackground = 0xFF1F6FB2.toInt(),
            selectionForeground = 0xFFFFFFFF.toInt(),
            caret = 0xFF1F6FB2.toInt(),
            shadow = 0xFF000000.toInt(),
        )

    /**
     * Monochrome high-contrast palette.
     *
     * @since 1.0.0
     */
    public fun highContrast(): Theme =
        Theme(
            background = 0xFF000000.toInt(),
            surface = 0xFF000000.toInt(),
            surfaceAlt = 0xFF202020.toInt(),
            text = 0xFFFFFFFF.toInt(),
            textMuted = 0xFFFFFFFF.toInt(),
            textDisabled = 0xFF808080.toInt(),
            accent = 0xFFFFFFFF.toInt(),
            success = 0xFFFFFFFF.toInt(),
            warning = 0xFFFFFFFF.toInt(),
            error = 0xFFFFFFFF.toInt(),
            border = 0xFFFFFFFF.toInt(),
            borderFocused = 0xFFFFFFFF.toInt(),
            selectionBackground = 0xFFFFFFFF.toInt(),
            selectionForeground = 0xFF000000.toInt(),
            caret = 0xFFFFFFFF.toInt(),
            shadow = 0xFFFFFFFF.toInt(),
        )
}
