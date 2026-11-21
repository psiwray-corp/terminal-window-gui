package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Builds the depth-first Tab order for a widget tree.
 *
 * Only enabled, visible, non-zero-size, focusable widgets with non-skipping
 * ancestors are included. The order follows child paint order.
 *
 * @since 1.0.0
 */
public object FocusTraversal {
    /**
     * Returns focusable widgets under [root] in depth-first order.
     *
     * @since 1.0.0
     */
    public fun order(root: Widget): List<Widget> {
        val result = ArrayList<Widget>()
        collect(root, result)
        return result
    }

    /**
     * Returns whether [widget] can currently receive focus.
     *
     * @since 1.0.0
     */
    public fun isEligible(widget: Widget): Boolean {
        if (!widget.focusable || !widget.enabled || !widget.visible || widget.bounds.isEmpty) {
            return false
        }
        if (widget.focusScope == FocusScope.SKIP) {
            return false
        }
        var current = widget.parent
        while (current != null) {
            if (!current.enabled || !current.visible || current.bounds.isEmpty) {
                return false
            }
            if (current.focusScope == FocusScope.SKIP) {
                return false
            }
            current = current.parent
        }
        return true
    }

    /**
     * Returns the next widget after [focused], wrapping at the end.
     *
     * @since 1.0.0
     */
    public fun next(
        root: Widget,
        focused: Widget?,
    ): Widget? = next(order(root), focused)

    /**
     * Returns the next widget after [focused] in [order], wrapping at the end.
     *
     * @since 1.0.0
     */
    public fun next(
        order: List<Widget>,
        focused: Widget?,
    ): Widget? {
        if (order.isEmpty()) {
            return null
        }
        val index = order.indexOf(focused)
        return order[if (index < 0) 0 else (index + 1) % order.size]
    }

    /**
     * Returns the previous widget before [focused], wrapping at the start.
     *
     * @since 1.0.0
     */
    public fun previous(
        root: Widget,
        focused: Widget?,
    ): Widget? = previous(order(root), focused)

    /**
     * Returns the previous widget before [focused] in [order], wrapping at the
     * start.
     *
     * @since 1.0.0
     */
    public fun previous(
        order: List<Widget>,
        focused: Widget?,
    ): Widget? {
        if (order.isEmpty()) {
            return null
        }
        val index = order.indexOf(focused)
        return order[if (index <= 0) order.size - 1 else index - 1]
    }

    /**
     * Returns focus candidates inside [scope], including [scope] when it is
     * itself focusable.
     *
     * @since 1.0.0
     */
    internal fun orderWithin(scope: Widget): List<Widget> {
        val result = ArrayList<Widget>()
        collect(scope, result)
        return result
    }

    /**
     * Returns whether [widget] is [scope] or a descendant of [scope].
     *
     * @since 1.0.0
     */
    internal fun isWithin(
        widget: Widget,
        scope: Widget,
    ): Boolean {
        var current: Widget? = widget
        while (current != null) {
            if (current === scope) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun collect(
        widget: Widget,
        result: MutableList<Widget>,
    ) {
        if (!widget.enabled || !widget.visible || widget.bounds.isEmpty) {
            return
        }
        if (widget.focusScope == FocusScope.SKIP) {
            return
        }
        if (widget.focusable && widget.tabStop) {
            result.add(widget)
        }
        for (child in widget.traversalChildren()) {
            collect(child, result)
        }
    }
}
