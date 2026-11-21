package com.psiwray.organization.terminal_window_gui.widget

/**
 * Mutable scroll viewport state.
 *
 * All mutations clamp offsets to non-negative values within the content bounds.
 * [revision] increments only when a value actually changes, so widgets can
 * skip redundant work.
 *
 * @param contentWidth Initial content width.
 * @param contentHeight Initial content height.
 * @param viewportWidth Initial viewport width.
 * @param viewportHeight Initial viewport height.
 * @since 1.0.0
 */
public class ScrollModel(
    contentWidth: Int = 0,
    contentHeight: Int = 0,
    viewportWidth: Int = 0,
    viewportHeight: Int = 0,
) {
    private var contentWidthValue: Int = nonNegative(contentWidth, "contentWidth")
    private var contentHeightValue: Int = nonNegative(contentHeight, "contentHeight")
    private var viewportWidthValue: Int = nonNegative(viewportWidth, "viewportWidth")
    private var viewportHeightValue: Int = nonNegative(viewportHeight, "viewportHeight")
    private var offsetXValue: Int = 0
    private var offsetYValue: Int = 0
    private var followEndValue: Boolean = false
    private val offsetListeners: MutableList<() -> Unit> = ArrayList()

    internal fun addOffsetListener(listener: () -> Unit) {
        offsetListeners.add(listener)
    }

    internal fun removeOffsetListener(listener: () -> Unit) {
        offsetListeners.remove(listener)
    }

    /** Increments whenever visible scroll state changes. @since 1.0.0 */
    public var revision: Long = 0
        private set

    /** Content width in cells. @since 1.0.0 */
    public var contentWidth: Int
        get() = contentWidthValue
        set(value) {
            updateContent(value, contentHeightValue, "contentWidth")
        }

    /** Content height in cells. @since 1.0.0 */
    public var contentHeight: Int
        get() = contentHeightValue
        set(value) {
            updateContent(contentWidthValue, value, "contentHeight")
        }

    /** Viewport width in cells. @since 1.0.0 */
    public var viewportWidth: Int
        get() = viewportWidthValue
        set(value) {
            updateViewport(value, viewportHeightValue, "viewportWidth")
        }

    /** Viewport height in cells. @since 1.0.0 */
    public var viewportHeight: Int
        get() = viewportHeightValue
        set(value) {
            updateViewport(viewportWidthValue, value, "viewportHeight")
        }

    /** Horizontal offset in cells. @since 1.0.0 */
    public var offsetX: Int
        get() = offsetXValue
        set(value) {
            setOffsets(value, offsetYValue)
        }

    /** Vertical offset in cells. @since 1.0.0 */
    public var offsetY: Int
        get() = offsetYValue
        set(value) {
            setOffsets(offsetXValue, value)
        }

    /** Whether content growth sticks to the bottom. @since 1.0.0 */
    public var followEnd: Boolean
        get() = followEndValue
        set(value) {
            if (followEndValue == value) {
                return
            }
            val revisionBefore = revision
            followEndValue = value
            if (value) {
                setOffsets(offsetXValue, maxOffsetY)
            }
            if (revision == revisionBefore) {
                bump()
            }
        }

    /** Maximum horizontal offset. @since 1.0.0 */
    public val maxOffsetX: Int get() = maxOf(0, contentWidthValue - viewportWidthValue)

    /** Maximum vertical offset. @since 1.0.0 */
    public val maxOffsetY: Int get() = maxOf(0, contentHeightValue - viewportHeightValue)

    /** Scrolls by a relative delta. @return whether the offset changed. @since 1.0.0 */
    public fun scrollBy(
        dx: Int,
        dy: Int,
    ): Boolean = setOffsets(offsetXValue + dx, offsetYValue + dy)

    /** Scrolls to an absolute offset. @return whether the offset changed. @since 1.0.0 */
    public fun scrollTo(
        x: Int,
        y: Int,
    ): Boolean = setOffsets(x, y)

    /**
     * Makes the requested content rectangle visible with the smallest offset
     * change.
     *
     * @return whether the offset changed.
     * @since 1.0.0
     */
    public fun ensureVisible(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Boolean {
        require(width >= 0) { "width must be non-negative, got $width" }
        require(height >= 0) { "height must be non-negative, got $height" }
        val targetX = axisTarget(x, width, offsetXValue, viewportWidthValue, contentWidthValue)
        val targetY = axisTarget(y, height, offsetYValue, viewportHeightValue, contentHeightValue)
        return setOffsets(targetX, targetY)
    }

    /** Scrolls up one viewport. @return whether the offset changed. @since 1.0.0 */
    public fun pageUp(): Boolean = scrollBy(0, -viewportHeightValue)

    /** Scrolls down one viewport. @return whether the offset changed. @since 1.0.0 */
    public fun pageDown(): Boolean = scrollBy(0, viewportHeightValue)

    /** Scrolls to the top-left. @return whether the offset changed. @since 1.0.0 */
    public fun home(): Boolean = scrollTo(0, 0)

    /** Scrolls to the bottom-right. @return whether the offset changed. @since 1.0.0 */
    public fun end(): Boolean = scrollTo(maxOffsetX, maxOffsetY)

    private fun updateContent(
        width: Int,
        height: Int,
        name: String,
    ) {
        val nextWidth = nonNegative(width, name)
        val nextHeight = nonNegative(height, name)
        if (nextWidth == contentWidthValue && nextHeight == contentHeightValue) {
            return
        }
        val revisionBefore = revision
        contentWidthValue = nextWidth
        contentHeightValue = nextHeight
        if (followEndValue) {
            setOffsets(offsetXValue, maxOffsetY)
        } else {
            setOffsets(offsetXValue, offsetYValue)
        }
        if (revision == revisionBefore) {
            bump()
        }
    }

    private fun updateViewport(
        width: Int,
        height: Int,
        name: String,
    ) {
        val nextWidth = nonNegative(width, name)
        val nextHeight = nonNegative(height, name)
        if (nextWidth == viewportWidthValue && nextHeight == viewportHeightValue) {
            return
        }
        val revisionBefore = revision
        viewportWidthValue = nextWidth
        viewportHeightValue = nextHeight
        if (followEndValue) {
            setOffsets(offsetXValue, maxOffsetY)
        } else {
            setOffsets(offsetXValue, offsetYValue)
        }
        if (revision == revisionBefore) {
            bump()
        }
    }

    private fun setOffsets(
        x: Int,
        y: Int,
    ): Boolean {
        val nextX = x.coerceIn(0, maxOffsetX)
        val nextY = y.coerceIn(0, maxOffsetY)
        val changed = nextX != offsetXValue || nextY != offsetYValue
        val followBefore = followEndValue
        if (changed) {
            offsetXValue = nextX
            offsetYValue = nextY
        }
        if (maxOffsetY > 0 && nextY >= maxOffsetY) {
            followEndValue = true
        } else if (nextY < maxOffsetY && followEndValue) {
            followEndValue = false
        }
        if (changed || followEndValue != followBefore) {
            bump()
        }
        if (changed) {
            for (listener in offsetListeners.toList()) {
                listener()
            }
        }
        return changed
    }

    private fun bump() {
        revision++
    }

    private fun axisTarget(
        start: Int,
        size: Int,
        current: Int,
        viewport: Int,
        content: Int,
    ): Int {
        val maxOffset = maxOf(0, content - viewport)
        if (size >= viewport) {
            return start.coerceIn(0, maxOffset)
        }
        val end = start + size
        return when {
            start < current -> start
            end > current + viewport -> end - viewport
            else -> current
        }.coerceIn(0, maxOffset)
    }

    private fun nonNegative(
        value: Int,
        name: String,
    ): Int {
        require(value >= 0) { "$name must be non-negative, got $value" }
        return value
    }
}
