package com.psiwray.organization.terminal_window_gui.text

/** Code-point-safe caret indexing over a UTF-16 string. */
internal object Caret {
    fun snap(
        text: CharSequence,
        index: Int,
    ): Int {
        val value = index.coerceIn(0, text.length)
        if (value <= 0 || value >= text.length) {
            return value
        }
        return if (Character.isLowSurrogate(text[value]) && Character.isHighSurrogate(text[value - 1])) {
            value - 1
        } else {
            value
        }
    }

    fun previous(
        text: CharSequence,
        index: Int,
    ): Int {
        val value = snap(text, index)
        if (value <= 0) {
            return 0
        }
        return value - Character.charCount(Character.codePointBefore(text, value))
    }

    fun next(
        text: CharSequence,
        index: Int,
    ): Int {
        val value = snap(text, index)
        if (value >= text.length) {
            return text.length
        }
        return value + Character.charCount(Character.codePointAt(text, value))
    }

    fun count(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
    ): Int = Character.codePointCount(text, start, end)

    fun take(
        text: CharSequence,
        count: Int,
    ): String {
        if (count <= 0) {
            return ""
        }
        val result = StringBuilder()
        var index = 0
        var taken = 0
        while (index < text.length && taken < count) {
            val codePoint = Character.codePointAt(text, index)
            result.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
            taken++
        }
        return result.toString()
    }

    fun advance(
        text: CharSequence,
        start: Int,
        count: Int,
    ): Int {
        var index = snap(text, start)
        if (count >= 0) {
            repeat(count) { index = next(text, index) }
        } else {
            repeat(-count) { index = previous(text, index) }
        }
        return index
    }
}
