package com.psiwray.organization.terminal_window_gui.testkit

import com.psiwray.organization.terminal_window.TerminalMatrix
import com.psiwray.organization.terminal_window_gui.style.Theme

/**
 * Converts a rendered matrix to deterministic structural rows and style rows.
 *
 * Style rows use theme token names where possible. Cells are rendered as
 * `foreground/background` and separated by one space. Unknown colors use
 * `#AARRGGBB` in uppercase hexadecimal.
 */
object MatrixDump {
    /** Returns one structural row per matrix row. */
    fun rows(matrix: TerminalMatrix): List<String> {
        val result = ArrayList<String>(matrix.rows)
        var row = 0
        while (row < matrix.rows) {
            val builder = StringBuilder(matrix.columns)
            var column = 0
            while (column < matrix.columns) {
                val codePoint = matrix.codePointAt(column, row)
                if (codePoint == 0) {
                    builder.append(' ')
                } else {
                    builder.appendCodePoint(codePoint)
                }
                column++
            }
            result.add(builder.toString())
            row++
        }
        return result
    }

    /** Returns one style row per matrix row. */
    fun styles(
        matrix: TerminalMatrix,
        theme: Theme,
    ): List<String> {
        val result = ArrayList<String>(matrix.rows)
        val tokens = themeTokens(theme)
        var row = 0
        while (row < matrix.rows) {
            val builder = StringBuilder(matrix.columns * 12)
            var column = 0
            while (column < matrix.columns) {
                if (column > 0) {
                    builder.append(' ')
                }
                builder.append(tokens[matrix.foregroundAt(column, row)] ?: raw(matrix.foregroundAt(column, row)))
                builder.append('/')
                builder.append(tokens[matrix.backgroundAt(column, row)] ?: raw(matrix.backgroundAt(column, row)))
                column++
            }
            result.add(builder.toString())
            row++
        }
        return result
    }

    /** Returns a theme token name for [color], or a raw `#AARRGGBB` value. */
    fun token(
        theme: Theme,
        color: Int,
    ): String = themeTokens(theme)[color] ?: raw(color)

    private fun themeTokens(theme: Theme): Map<Int, String> {
        val result = LinkedHashMap<Int, String>()
        addToken(result, theme.background, "background")
        addToken(result, theme.surface, "surface")
        addToken(result, theme.surfaceAlt, "surfaceAlt")
        addToken(result, theme.text, "text")
        addToken(result, theme.textMuted, "textMuted")
        addToken(result, theme.textDisabled, "textDisabled")
        addToken(result, theme.accent, "accent")
        addToken(result, theme.success, "success")
        addToken(result, theme.warning, "warning")
        addToken(result, theme.error, "error")
        addToken(result, theme.border, "border")
        addToken(result, theme.borderFocused, "borderFocused")
        addToken(result, theme.selectionBackground, "selectionBackground")
        addToken(result, theme.selectionForeground, "selectionForeground")
        addToken(result, theme.caret, "caret")
        addToken(result, theme.shadow, "shadow")
        return result
    }

    private fun addToken(
        tokens: MutableMap<Int, String>,
        color: Int,
        name: String,
    ) {
        if (!tokens.containsKey(color)) {
            tokens[color] = name
        }
    }

    private fun raw(color: Int): String = "#${color.toUInt().toString(16).uppercase().padStart(8, '0')}"
}
