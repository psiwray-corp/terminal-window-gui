package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Stable-key cache of row widgets for one collection viewport.
 *
 * Collection widgets keep only visible keys in the cache. Changing the item
 * builder or the data revision clears it.
 */
internal class VisibleRowCache<T>(
    var builder: (Int, T) -> Widget,
) {
    private val rows: LinkedHashMap<Any, Widget> = LinkedHashMap()

    val size: Int get() = rows.size

    fun row(
        index: Int,
        item: T,
        key: Any,
    ): Widget = rows.getOrPut(key) { builder(index, item) }

    fun cached(key: Any): Widget? = rows[key]

    /**
     * Drops every key outside [visible] and returns the removed widgets.
     */
    fun retain(visible: Set<Any>): List<Widget> {
        val removed = ArrayList<Widget>()
        val iterator = rows.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in visible) {
                removed.add(entry.value)
                iterator.remove()
            }
        }
        return removed
    }

    fun clear(): List<Widget> {
        val removed = ArrayList<Widget>(rows.values)
        rows.clear()
        return removed
    }
}
