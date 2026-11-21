package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.widget.ScrollView
import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Owns keyboard focus for one widget tree.
 *
 * The manager is single-thread-affine. Focus changes invoke
 * [Widget.onFocusChanged] in tree order.
 *
 * @property root Root of the normal focus tree.
 * @since 1.0.0
 */
public class FocusManager(
    public val root: Widget,
) {
    private var focusedValue: Widget? = null
    private var previousFocusedValue: Widget? = null
    private val traps: MutableList<Widget> = ArrayList()
    private val extraRoots: MutableList<Widget> = ArrayList()
    private val listeners: MutableList<(Widget?, Widget?) -> Unit> = ArrayList()

    /**
     * Currently focused widget, or `null` when focus is elsewhere or no longer
     * eligible.
     *
     * @since 1.0.0
     */
    public val focused: Widget?
        get() {
            val current = focusedValue ?: return null
            if (isInTree(current) && FocusTraversal.isEligible(current)) {
                return current
            }
            previousFocusedValue = current
            focusedValue = null
            current.setFocused(false)
            return null
        }

    /**
     * Root-to-focused path, or an empty list.
     *
     * @since 1.0.0
     */
    public val focusPath: List<Widget>
        get() {
            val current = focused ?: return emptyList()
            val path = ArrayList<Widget>()
            var widget: Widget? = current
            while (widget != null) {
                path.add(widget)
                widget = widget.parent
            }
            path.reverse()
            return path
        }

    /**
     * Focuses [widget] when it is eligible and inside this manager's tree.
     * Passing `null` clears focus.
     *
     * @since 1.0.0
     */
    public fun focus(widget: Widget?): Boolean {
        if (widget == null) {
            clearFocus()
            return true
        }
        if (!isInTree(widget) || !FocusTraversal.isEligible(widget)) {
            return false
        }
        if (focusedValue === widget) {
            return true
        }
        val old = focusedValue
        if (old != null) {
            previousFocusedValue = old
        }
        focusedValue = widget
        old?.setFocused(false)
        widget.setFocused(true)
        reveal(widget)
        notifyListeners(widget, old)
        return true
    }

    /** Clears focus and notifies the previous widget. @since 1.0.0 */
    public fun clearFocus() {
        val old = focusedValue ?: return
        previousFocusedValue = old
        focusedValue = null
        old.setFocused(false)
        notifyListeners(null, old)
    }

    /** Focuses the first eligible widget. @since 1.0.0 */
    public fun focusFirst(): Boolean {
        val order = activeOrder()
        return order.firstOrNull()?.let(::focus) ?: false
    }

    /** Focuses the last eligible widget. @since 1.0.0 */
    public fun focusLast(): Boolean {
        val order = activeOrder()
        return order.lastOrNull()?.let(::focus) ?: false
    }

    /** Moves focus to the next eligible widget, wrapping. @since 1.0.0 */
    public fun focusNext(): Boolean {
        val current = focused
        val order = activeOrder(current)
        if (order.isEmpty()) {
            return false
        }
        val next = FocusTraversal.next(order, current)
        return next != null && focus(next)
    }

    /** Moves focus to the previous eligible widget, wrapping. @since 1.0.0 */
    public fun focusPrevious(): Boolean {
        val current = focused
        val order = activeOrder(current)
        if (order.isEmpty()) {
            return false
        }
        val previous = FocusTraversal.previous(order, current)
        return previous != null && focus(previous)
    }

    /**
     * Restores [previous] when it is still eligible.
     *
     * @since 1.0.0
     */
    public fun restore(previous: Widget?): Boolean = focus(previous)

    /**
     * Pushes an explicit focus trap, used by modal overlays.
     *
     * @since 1.0.0
     */
    public fun pushTrap(scope: Widget) {
        traps.add(scope)
    }

    /**
     * Pops the most recent trap for [scope], if present.
     *
     * @since 1.0.0
     */
    public fun popTrap(scope: Widget) {
        val index = traps.lastIndexOf(scope)
        if (index >= 0) {
            traps.removeAt(index)
        }
    }

    /**
     * Returns whether [widget] belongs to the root or an attached overlay root.
     *
     * @since 1.0.0
     */
    public fun isInTree(widget: Widget): Boolean {
        var current: Widget? = widget
        while (current != null) {
            if (current === root) {
                return true
            }
            if (extraRoots.any { it === current }) {
                return true
            }
            current = current.parent
        }
        return false
    }

    internal fun addFocusListener(listener: (Widget?, Widget?) -> Unit) {
        listeners.add(listener)
    }

    internal fun removeFocusListener(listener: (Widget?, Widget?) -> Unit) {
        listeners.remove(listener)
    }

    internal fun addRoot(widget: Widget) {
        if (extraRoots.none { it === widget }) {
            extraRoots.add(widget)
        }
    }

    internal fun removeRoot(widget: Widget) {
        extraRoots.removeAll { it === widget }
        val current = focusedValue
        if (current != null && !isInTree(current)) {
            clearFocus()
        }
    }

    /** Most recently focused widget before the current one, if any. */
    internal val previousFocused: Widget? get() = previousFocusedValue

    private fun reveal(widget: Widget) {
        var current = widget.parent
        while (current != null) {
            if (current is ScrollView) {
                current.reveal(widget)
            }
            current = current.parent
        }
    }

    private fun notifyListeners(
        current: Widget?,
        previous: Widget?,
    ) {
        for (listener in listeners.toList()) {
            listener(current, previous)
        }
    }

    private fun activeOrder(current: Widget? = focused): List<Widget> {
        if (traps.isNotEmpty()) {
            return FocusTraversal.orderWithin(traps.last())
        }
        if (current != null) {
            var scope: Widget? = current
            while (scope != null) {
                if (scope.focusScope == FocusScope.TRAP) {
                    return FocusTraversal.orderWithin(scope)
                }
                scope = scope.parent
            }
        }
        val result = ArrayList<Widget>(FocusTraversal.order(root))
        for (extra in extraRoots) {
            result.addAll(FocusTraversal.order(extra))
        }
        return result
    }
}
