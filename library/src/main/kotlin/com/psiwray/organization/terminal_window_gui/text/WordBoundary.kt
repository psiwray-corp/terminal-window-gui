package com.psiwray.organization.terminal_window_gui.text

/**
 * ASCII word boundaries for v1 editing.
 *
 * Word characters are ASCII letters, digits, and underscore. Spaces and
 * punctuation form their own runs. Unicode word segmentation is deferred to
 * v1.1.
 */
internal object WordBoundary {
    fun previous(
        text: CharSequence,
        index: Int,
    ): Int {
        var current = Caret.snap(text, index)
        if (current <= 0) {
            return 0
        }
        current = Caret.previous(text, current)
        while (current > 0 && categoryOf(text, current) == SPACE) {
            current = Caret.previous(text, current)
        }
        val category = categoryOf(text, current)
        while (current > 0) {
            val before = Caret.previous(text, current)
            if (categoryOf(text, before) != category) {
                break
            }
            current = before
        }
        return current
    }

    fun next(
        text: CharSequence,
        index: Int,
    ): Int {
        var current = Caret.snap(text, index)
        if (current >= text.length) {
            return text.length
        }
        val category = categoryOf(text, current)
        while (current < text.length) {
            val codePoint = Character.codePointAt(text, current)
            if (categoryOf(codePoint) != category) {
                break
            }
            current = Caret.next(text, current)
        }
        return current
    }

    fun wordRange(
        text: CharSequence,
        index: Int,
    ): IntRange {
        val position = Caret.snap(text, index)
        if (text.isEmpty()) {
            return 0..0
        }
        val probe = if (position >= text.length) Caret.previous(text, text.length) else position
        val category = categoryOf(text, probe)
        var start = probe
        while (start > 0) {
            val before = Caret.previous(text, start)
            if (categoryOf(text, before) != category) {
                break
            }
            start = before
        }
        var end = Caret.next(text, probe)
        while (end < text.length && categoryOf(text, end) == category) {
            end = Caret.next(text, end)
        }
        return start until end
    }

    fun isWord(codePoint: Int): Boolean =
        codePoint == '_'.code ||
            codePoint in '0'.code..'9'.code ||
            codePoint in 'a'.code..'z'.code ||
            codePoint in 'A'.code..'Z'.code

    private fun categoryOf(
        text: CharSequence,
        index: Int,
    ): Int = categoryOf(Character.codePointAt(text, Caret.snap(text, index)))

    private fun categoryOf(codePoint: Int): Int =
        when {
            isWord(codePoint) -> WORD
            codePoint == ' '.code || codePoint == '\t'.code || codePoint == '\n'.code || codePoint == '\r'.code -> SPACE
            else -> PUNCTUATION
        }

    private const val WORD: Int = 0
    private const val SPACE: Int = 1
    private const val PUNCTUATION: Int = 2
}
