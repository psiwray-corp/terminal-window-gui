package com.psiwray.organization.terminal_window_gui.internal

/**
 * Timed prefix buffer used by collection type-ahead.
 *
 * Printable input appends to the buffer. A gap longer than [timeoutNanos]
 * starts a new query. Matching is case-insensitive and searches forward from
 * the cursor, wrapping at the end.
 */
internal class TypeAhead(
    private val timeoutNanos: Long = TIMEOUT_NANOS,
) {
    private var buffer: String = ""
    private var lastNanos: Long = Long.MIN_VALUE

    /** Appends [codePoint] and returns the active query. */
    fun append(
        codePoint: Int,
        nowNanos: Long,
    ): String {
        resetIfExpired(nowNanos)
        buffer += String(Character.toChars(codePoint))
        lastNanos = nowNanos
        return buffer
    }

    /** Returns the active query, resetting it when the timeout elapsed. */
    fun value(nowNanos: Long): String {
        resetIfExpired(nowNanos)
        return buffer
    }

    fun reset() {
        buffer = ""
        lastNanos = Long.MIN_VALUE
    }

    /**
     * Returns the next label index whose prefix matches the active query, or
     * `null` when nothing matches.
     */
    fun match(
        labels: List<String>,
        cursorIndex: Int,
        nowNanos: Long,
    ): Int? {
        val query = value(nowNanos)
        if (query.isEmpty() || labels.isEmpty()) {
            return null
        }
        val start =
            if (cursorIndex in labels.indices) {
                (cursorIndex + 1) % labels.size
            } else {
                0
            }
        var offset = 0
        while (offset < labels.size) {
            val index = (start + offset) % labels.size
            if (labels[index].startsWith(query, ignoreCase = true)) {
                return index
            }
            offset++
        }
        return null
    }

    private fun resetIfExpired(nowNanos: Long) {
        if (lastNanos == Long.MIN_VALUE || nowNanos - lastNanos > timeoutNanos) {
            buffer = ""
        }
    }

    private companion object {
        const val TIMEOUT_NANOS: Long = 500_000_000L
    }
}
