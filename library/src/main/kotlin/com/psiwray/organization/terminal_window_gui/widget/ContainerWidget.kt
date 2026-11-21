package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Overflow
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Base for widgets that own children and a content rectangle.
 *
 * Children are positioned relative to [contentRect] within this widget's local
 * coordinates. The default [Overflow.CLIP] clips children to that rectangle.
 *
 * @property overflow How children outside [contentRect] are painted.
 * @since 1.0.0
 */
public abstract class ContainerWidget protected constructor(
    public val overflow: Overflow = Overflow.CLIP,
) : Widget() {
    /** Ordered child storage. @since 1.0.0 */
    protected val childList: MutableList<Widget> = ArrayList()

    /** Content rectangle relative to this widget's bounds. @since 1.0.0 */
    public val contentRect: Rect get() = computeContentRect(bounds)

    /** Scroll offset subtracted from child paint and hit coordinates. */
    internal open fun contentScrollOffset(): IntSize = IntSize.ZERO

    /**
     * Computes the content rectangle for [bounds].
     *
     * @since 1.0.0
     */
    protected open fun computeContentRect(bounds: Rect): Rect = bounds

    final override fun paintChildren(context: PaintContext) {
        val content = localContentRect()
        if (childList.isEmpty() || content.isEmpty) {
            return
        }
        val scroll = contentScrollOffset()
        if (overflow == Overflow.CLIP) {
            context.pushClip(content)
        }
        context.pushOrigin(content.x - scroll.width, content.y - scroll.height)
        try {
            for (child in childList) {
                val childBounds = child.bounds
                if (childBounds.isEmpty) {
                    continue
                }
                context.pushOrigin(childBounds.x, childBounds.y)
                try {
                    child.render(context)
                } finally {
                    context.popOrigin()
                }
            }
        } finally {
            context.popOrigin()
            if (overflow == Overflow.CLIP) {
                context.popClip()
            }
        }
    }

    private fun localContentRect(): Rect {
        val content = contentRect
        return Rect(content.x - bounds.x, content.y - bounds.y, content.width, content.height)
    }

    final override fun hitTest(
        x: Int,
        y: Int,
    ): Widget? {
        if (!visible || !enabled || bounds.isEmpty) {
            return null
        }
        if (x < 0 || y < 0 || x >= bounds.width || y >= bounds.height) {
            return null
        }
        if (childList.isEmpty()) {
            return this
        }
        val content = localContentRect()
        val scroll = contentScrollOffset()
        val insideContent =
            x >= content.x && y >= content.y && x < content.right && y < content.bottom
        if (insideContent) {
            val localX = x - content.x + scroll.width
            val localY = y - content.y + scroll.height
            var index = childList.size - 1
            while (index >= 0) {
                val child = childList[index]
                val childBounds = child.bounds
                if (!childBounds.isEmpty) {
                    val hit = child.hitTest(localX - childBounds.x, localY - childBounds.y)
                    if (hit != null) {
                        return hit
                    }
                }
                index--
            }
        }
        return this
    }

    override fun traversalChildren(): List<Widget> = childList

    final override fun attachChildren() {
        for (child in childList) {
            child.attachTree(guiContext)
        }
    }

    final override fun detachChildren() {
        for (child in childList) {
            child.detachTree()
        }
    }

    /**
     * Adds [child] at [index], detaching it from a previous parent.
     *
     * @since 1.0.0
     */
    protected fun adopt(
        child: Widget,
        index: Int = childList.size,
    ) {
        require(child !== this) { "a widget cannot parent itself" }
        require(!child.isAncestorOf(this)) { "a widget cannot be reparented under its descendant" }
        require(index in 0..childList.size) { "index $index out of bounds for ${childList.size}" }
        if (child.parent === this) {
            return
        }
        val previous = child.parent
        if (previous is ContainerWidget) {
            previous.detachChildInternal(child)
        }
        childList.add(index, child)
        child.setParent(this)
        if (isAttached) {
            child.attachTree(guiContext)
        }
        invalidateLayout()
    }

    /**
     * Removes [child] and detaches its subtree.
     *
     * @since 1.0.0
     */
    protected fun drop(child: Widget): Boolean {
        if (!childList.remove(child)) {
            return false
        }
        child.detachTree()
        child.setParent(null)
        invalidateLayout()
        return true
    }

    /**
     * Detaches [child] during reparenting.
     *
     * @since 1.0.0
     */
    internal open fun detachChildInternal(child: Widget): Boolean = false
}
