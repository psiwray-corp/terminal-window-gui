package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window.input.Modifiers

/** Shared input rules for editable widgets. */
internal object TextInput {
    fun singleLine(value: String): String {
        if (value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value
        }
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val codePoint = Character.codePointAt(value, index)
            when (codePoint) {
                '\r'.code, '\n'.code -> result.append(' ')
                else -> result.appendCodePoint(codePoint)
            }
            index += Character.charCount(codePoint)
        }
        return result.toString()
    }

    fun command(modifiers: Modifiers): Boolean = modifiers.has(Modifiers.CONTROL) || modifiers.has(Modifiers.SUPER)

    fun shift(modifiers: Modifiers): Boolean = modifiers.has(Modifiers.SHIFT)
}
