package com.psiwray.organization.terminal_window_gui.paint

/** Fixed tab width in cells for text measurement and painting. @since 1.0.0 */
public const val DEFAULT_TAB_WIDTH: Int = 4

/**
 * Deterministic text measurement for the one-cell-per-code-point model.
 *
 * Tabs expand to the next multiple of [tabWidth] from the start of the current
 * line. `\n` starts a new line. Other C0 controls become a space. Unpaired
 * surrogates become U+FFFD. Word wrapping collapses runs of spaces and trims
 * leading and trailing spaces; character wrapping preserves spaces.
 *
 * @property tabWidth Tab stop width in cells; must be positive.
 * @since 1.0.0
 */
public class TextMetrics(
    public val tabWidth: Int = DEFAULT_TAB_WIDTH,
) {
    init {
        require(tabWidth > 0) { "tabWidth must be positive, got $tabWidth" }
    }

    /**
     * Measures [text] into at most [maxWidth] cells and [maxLines] lines.
     *
     * [TextWrap.NONE] truncates lines wider than [maxWidth]. [TextWrap.WORD]
     * breaks at spaces and hard-breaks words wider than [maxWidth].
     * [TextWrap.CHAR] breaks at every code-point boundary. [TextLayout.truncated]
     * is true when a width or line limit omitted content.
     *
     * @throws IllegalArgumentException if [maxWidth] or [maxLines] is negative.
     * @since 1.0.0
     */
    public fun measure(
        text: CharSequence,
        maxWidth: Int,
        wrap: TextWrap = TextWrap.NONE,
        maxLines: Int = Int.MAX_VALUE,
    ): TextLayout {
        require(maxWidth >= 0) { "maxWidth must be non-negative, got $maxWidth" }
        require(maxLines >= 0) { "maxLines must be non-negative, got $maxLines" }

        val normalized = normalize(text)
        if (normalized.isEmpty()) {
            return TextLayout(emptyList(), 0, 0, false)
        }
        if (maxWidth == 0) {
            return TextLayout(emptyList(), 0, 0, true)
        }

        val lines = ArrayList<String>()
        var truncated = false
        var start = 0
        while (true) {
            val end = normalized.indexOf('\n', start)
            val line = if (end < 0) normalized.substring(start) else normalized.substring(start, end)
            when (wrap) {
                TextWrap.NONE -> {
                    val width = codePointCount(line)
                    if (width > maxWidth) {
                        lines.add(takeCodePoints(line, maxWidth))
                        truncated = true
                    } else {
                        lines.add(line)
                    }
                }

                TextWrap.CHAR -> {
                    appendCharacterWrapped(line, maxWidth, lines)
                }

                TextWrap.WORD -> {
                    appendWordWrapped(line, maxWidth, lines)
                }
            }
            if (end < 0) {
                break
            }
            start = end + 1
        }

        if (lines.size > maxLines) {
            lines.subList(maxLines, lines.size).clear()
            truncated = true
        }
        val width = lines.maxOfOrNull(::codePointCount) ?: 0
        return TextLayout(lines, width, lines.size, truncated)
    }

    private fun normalize(text: CharSequence): String {
        val output = StringBuilder(text.length)
        var index = 0
        var column = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            when {
                codePoint == '\n'.code -> {
                    output.append('\n')
                    column = 0
                }

                codePoint == '\t'.code -> {
                    val spaces = tabWidth - column % tabWidth
                    repeat(spaces) {
                        output.append(' ')
                    }
                    column += spaces
                }

                codePoint in 0x0000..0x001F -> {
                    output.append(' ')
                    column++
                }

                codePoint in 0xD800..0xDFFF -> {
                    output.append('\uFFFD')
                    column++
                }

                else -> {
                    output.appendCodePoint(codePoint)
                    column++
                }
            }
            index += Character.charCount(codePoint)
        }
        return output.toString()
    }

    private fun appendCharacterWrapped(
        line: String,
        maxWidth: Int,
        lines: MutableList<String>,
    ) {
        if (line.isEmpty()) {
            lines.add("")
            return
        }
        val current = StringBuilder()
        var width = 0
        var index = 0
        while (index < line.length) {
            if (width == maxWidth) {
                lines.add(current.toString())
                current.setLength(0)
                width = 0
            }
            val codePoint = Character.codePointAt(line, index)
            current.appendCodePoint(codePoint)
            width++
            index += Character.charCount(codePoint)
        }
        lines.add(current.toString())
    }

    private fun appendWordWrapped(
        line: String,
        maxWidth: Int,
        lines: MutableList<String>,
    ) {
        if (line.isEmpty()) {
            lines.add("")
            return
        }
        val words = words(line)
        if (words.isEmpty()) {
            lines.add("")
            return
        }

        val current = StringBuilder()
        var currentWidth = 0
        for (word in words) {
            val wordWidth = codePointCount(word)
            if (currentWidth == 0) {
                if (wordWidth <= maxWidth) {
                    current.append(word)
                    currentWidth = wordWidth
                } else {
                    appendHardBreaks(word, maxWidth, lines, current)
                    currentWidth = codePointCount(current)
                }
                continue
            }

            if (currentWidth + 1 + wordWidth <= maxWidth) {
                current.append(' ').append(word)
                currentWidth += 1 + wordWidth
            } else {
                lines.add(current.toString())
                current.setLength(0)
                if (wordWidth <= maxWidth) {
                    current.append(word)
                    currentWidth = wordWidth
                } else {
                    appendHardBreaks(word, maxWidth, lines, current)
                    currentWidth = codePointCount(current)
                }
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
    }

    private fun words(line: String): List<String> {
        val result = ArrayList<String>()
        var index = 0
        while (index < line.length) {
            while (index < line.length && line[index] == ' ') {
                index++
            }
            if (index >= line.length) {
                break
            }
            val start = index
            while (index < line.length && line[index] != ' ') {
                index++
            }
            result.add(line.substring(start, index))
        }
        return result
    }

    private fun appendHardBreaks(
        word: String,
        maxWidth: Int,
        lines: MutableList<String>,
        remainder: StringBuilder,
    ) {
        val current = StringBuilder()
        var width = 0
        var index = 0
        while (index < word.length) {
            if (width == maxWidth) {
                lines.add(current.toString())
                current.setLength(0)
                width = 0
            }
            val codePoint = Character.codePointAt(word, index)
            current.appendCodePoint(codePoint)
            width++
            index += Character.charCount(codePoint)
        }
        remainder.append(current)
    }

    private fun takeCodePoints(
        text: String,
        count: Int,
    ): String {
        if (count <= 0) {
            return ""
        }
        val output = StringBuilder()
        var index = 0
        var taken = 0
        while (index < text.length && taken < count) {
            val codePoint = Character.codePointAt(text, index)
            output.appendCodePoint(codePoint)
            taken++
            index += Character.charCount(codePoint)
        }
        return output.toString()
    }

    private fun codePointCount(text: CharSequence): Int = Character.codePointCount(text, 0, text.length)
}
