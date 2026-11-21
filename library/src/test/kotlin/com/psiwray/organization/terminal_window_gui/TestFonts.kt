package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.font.BitmapFont

internal fun fontWith(vararg codePoints: Int): BitmapFont {
    val builder = StringBuilder()
    builder.append("version 1\ncell-width 8\ncell-height 16\n")
    if (codePoints.contains(0x003F)) {
        builder.append("fallback U+003F\n")
    }
    builder.append('\n')
    for (codePoint in codePoints) {
        builder.append("glyph U+")
        builder.append(codePoint.toString(16).uppercase().padStart(4, '0'))
        builder.append('\n')
        repeat(16) {
            builder.append("........\n")
        }
        builder.append('\n')
    }
    return BitmapFont.parse(builder.toString().byteInputStream())
}

internal fun ascii95Font(): BitmapFont = fontWith(*(0x0020..0x007E).toList().toIntArray())
