package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Overflow

/**
 * Container with zero or one child.
 *
 * @since 1.0.0
 */
public abstract class SingleChildWidget protected constructor(
    child: Widget? = null,
    overflow: Overflow = Overflow.CLIP,
) : ContainerWidget(overflow) {
    init {
        if (child != null) {
            adopt(child)
        }
    }

    /** The child, or `null`. @since 1.0.0 */
    public val child: Widget? get() = childList.firstOrNull()

    /** Replaces the child, detaching the old subtree. @since 1.0.0 */
    public fun setChild(child: Widget?) {
        if (this.child === child) {
            return
        }
        val old = this.child
        if (old != null) {
            drop(old)
        }
        if (child != null) {
            adopt(child)
        } else {
            invalidateLayout()
        }
    }

    final override fun detachChildInternal(child: Widget): Boolean = drop(child)
}
