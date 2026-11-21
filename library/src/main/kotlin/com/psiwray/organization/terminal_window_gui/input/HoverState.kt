package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Maintains the current root-to-hovered path.
 *
 * Enter notifications run root-to-target; leave notifications run
 * target-to-root.
 *
 * @since 1.0.0
 */
public class HoverState {
    private val listeners: MutableList<(Widget?) -> Unit> = ArrayList()

    /** Deepest hovered widget, or `null`. @since 1.0.0 */
    public var hovered: Widget? = null
        private set

    /** Current root-to-hovered path. @since 1.0.0 */
    public var path: List<Widget> = emptyList()
        private set

    /**
     * Updates the hover path and dispatches enter/leave changes.
     *
     * @return whether the hovered widget changed.
     * @since 1.0.0
     */
    public fun update(target: Widget?): Boolean {
        val newPath = buildPath(target)
        val old = path
        if (newPath == old) {
            return false
        }
        var common = 0
        val limit = minOf(old.size, newPath.size)
        while (common < limit && old[common] === newPath[common]) {
            common++
        }
        var index = old.size - 1
        while (index >= common) {
            old[index].setHovered(false)
            index--
        }
        index = common
        while (index < newPath.size) {
            newPath[index].setHovered(true)
            index++
        }
        path = newPath
        hovered = target
        notifyListeners(target)
        return true
    }

    /** Clears hover and sends leave notifications. @since 1.0.0 */
    public fun clear() {
        update(null)
    }

    internal fun addHoverListener(listener: (Widget?) -> Unit) {
        listeners.add(listener)
    }

    internal fun removeHoverListener(listener: (Widget?) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(target: Widget?) {
        for (listener in listeners.toList()) {
            listener(target)
        }
    }

    private fun buildPath(target: Widget?): List<Widget> {
        if (target == null || !target.visible || !target.enabled) {
            return emptyList()
        }
        val result = ArrayList<Widget>()
        var current: Widget? = target
        while (current != null) {
            result.add(current)
            current = current.parent
        }
        result.reverse()
        return result
    }
}
