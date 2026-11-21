package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Overflow
import java.util.Collections

/**
 * Container with an ordered mutable list of children and an immutable view.
 *
 * @since 1.0.0
 */
public abstract class MultiChildWidget protected constructor(
    overflow: Overflow = Overflow.CLIP,
) : ContainerWidget(overflow) {
    /** Immutable view of the ordered children. @since 1.0.0 */
    public val children: List<Widget> get() = Collections.unmodifiableList(childList)

    /** Appends [child]. @since 1.0.0 */
    public fun add(child: Widget): MultiChildWidget {
        adopt(child)
        return this
    }

    /** Inserts [child] at [index]. @since 1.0.0 */
    public fun insert(
        index: Int,
        child: Widget,
    ): MultiChildWidget {
        require(index in 0..childList.size) { "index $index out of bounds for ${childList.size}" }
        if (child.parent === this) {
            val current = childList.indexOf(child)
            if (current == index) {
                return this
            }
            childList.remove(child)
            childList.add(if (index > current) index - 1 else index, child)
            invalidateLayout()
            return this
        }
        adopt(child, index)
        return this
    }

    /** Removes [child] and detaches its subtree. @since 1.0.0 */
    public fun remove(child: Widget): Boolean = drop(child)

    /** Removes and detaches every child. @since 1.0.0 */
    public fun clear() {
        if (childList.isEmpty()) {
            return
        }
        for (child in childList) {
            child.detachTree()
            child.setParent(null)
        }
        childList.clear()
        invalidateLayout()
    }

    final override fun detachChildInternal(child: Widget): Boolean = drop(child)
}
