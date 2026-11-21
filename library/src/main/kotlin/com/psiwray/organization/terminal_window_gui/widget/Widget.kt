package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window_gui.input.FocusScope
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.internal.GuiContext
import com.psiwray.organization.terminal_window_gui.internal.ScreenPoint
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Base class for every element in a GUI tree.
 *
 * [bounds] is relative to the parent content box. The root is arranged at the
 * surface origin; no screen coordinate is stored in a widget. Layout uses one
 * size path, [measure], and caches by constraints plus content revision.
 *
 * @since 1.0.0
 */
public abstract class Widget protected constructor() {
    /** Bounds relative to the parent content origin. @since 1.0.0 */
    public var bounds: Rect
        get() = boundsValue
        set(value) {
            arrange(value)
        }

    /** Parent widget, or `null` for a root. @since 1.0.0 */
    public var parent: Widget? = null
        private set

    /** Whether this widget is attached to a tree. @since 1.0.0 */
    public val isAttached: Boolean get() = attached

    /** Whether this widget accepts input. Disabled widgets receive no events. @since 1.0.0 */
    public var enabled: Boolean = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (!value && isFocused) {
                val manager = guiContext?.focus
                if (manager != null) {
                    manager.clearFocus()
                } else {
                    setFocused(false)
                }
            }
            invalidatePaint()
        }

    /** Whether this widget and its subtree participate in painting and input. @since 1.0.0 */
    public var visible: Boolean = true

    /** Whether this widget can receive keyboard focus. @since 1.0.0 */
    public var focusable: Boolean = false

    /**
     * Whether Tab traversal includes this widget.
     *
     * Set to `false` for structural containers that still accept programmatic
     * or pointer focus. @since 1.0.0
     */
    public var tabStop: Boolean = true

    /** Traversal policy for this widget's subtree. @since 1.0.0 */
    public var focusScope: FocusScope = FocusScope.ENTER

    /** Whether this widget currently owns keyboard focus. @since 1.0.0 */
    public var isFocused: Boolean = false
        private set

    /** Whether the pointer is over this widget or one of its descendants. @since 1.0.0 */
    public var isHovered: Boolean = false
        private set

    /** Whether a captured pointer button is held on this widget. @since 1.0.0 */
    public var isPressed: Boolean = false
        private set

    /** Whether a press on this widget requests pointer capture. @since 1.0.0 */
    internal open var wantsPointerCapture: Boolean = true

    /** Requested host cursor while this widget is hovered, or `null` to inherit. @since 1.0.0 */
    public var cursorShape: CursorShape? = null

    private var boundsValue: Rect = Rect.EMPTY
    private var attached: Boolean = false
    private var contentRevision: Long = 0
    private var cachedConstraints: Constraints? = null
    private var cachedRevision: Long = -1
    private var cachedSize: IntSize = IntSize.ZERO
    private var surfaceInvalidation: (() -> Unit)? = null
    private var surfacePaintInvalidation: (() -> Unit)? = null
    internal var guiContext: GuiContext? = null
        private set

    /**
     * Computes the preferred size under [constraints].
     *
     * Implementations must not paint or mutate the tree. The result is clamped
     * to the constraints by [measured].
     *
     * @since 1.0.0
     */
    protected abstract fun measure(constraints: Constraints): IntSize

    /**
     * Arranges children inside this widget's local bounds.
     *
     * Called after [bounds] changes and whenever layout is invalidated.
     *
     * @since 1.0.0
     */
    protected open fun arrangeChildren() {}

    /**
     * Paints this widget in its local coordinates.
     *
     * @since 1.0.0
     */
    protected abstract fun paint(context: PaintContext)

    /** Paints children in this widget's local coordinates. @since 1.0.0 */
    protected open fun paintChildren(context: PaintContext) {}

    /** Called when the widget enters an attached tree. @since 1.0.0 */
    public open fun onAttach() {}

    /** Called when the widget leaves an attached tree. @since 1.0.0 */
    public open fun onDetach() {}

    /** Called after [bounds] changes. @since 1.0.0 */
    public open fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {}

    /**
     * Handles an event during the target and bubble phases.
     *
     * Return `true` to consume the event and stop routing. The default returns
     * `false`.
     *
     * @since 1.0.0
     */
    public open fun onEvent(event: GuiEvent): Boolean = false

    /**
     * Handles an event during the capture phase.
     *
     * Capture runs from the root toward the target before [onEvent]. Return
     * `true` to consume the event and stop routing.
     *
     * @since 1.0.0
     */
    public open fun onCaptureEvent(event: GuiEvent): Boolean = false

    /** Called when keyboard focus enters or leaves this widget. @since 1.0.0 */
    public open fun onFocusChanged(focused: Boolean) {}

    /** Called when pointer hover enters or leaves this widget. @since 1.0.0 */
    public open fun onHoverChanged(hovered: Boolean) {}

    /**
     * Measures and arranges this widget as a root at the surface origin.
     *
     * The root bounds are the constrained measured size. Callers that need the
     * full terminal grid pass tight constraints for `(columns, rows)`.
     *
     * @since 1.0.0
     */
    public fun layout(constraints: Constraints): IntSize {
        val size = measured(constraints)
        val target = Rect(0, 0, size.width, size.height)
        val old = boundsValue
        arrange(target)
        if (old == target && !target.isEmpty) {
            arrangeChildren()
        }
        return size
    }

    /**
     * Stores local [bounds] and arranges children.
     *
     * Parents pass bounds relative to their content origin. A zero-area widget
     * does not arrange or paint its children.
     *
     * @since 1.0.0
     */
    public fun arrange(bounds: Rect) {
        val old = boundsValue
        boundsValue = bounds
        if (!attached) {
            attachTree()
        }
        if (old != bounds) {
            onBoundsChanged(old, bounds)
        }
        if (!bounds.isEmpty) {
            arrangeChildren()
        }
    }

    /**
     * Paints this widget and its children at the current context origin.
     *
     * A zero-area widget is skipped. Containers push their content origin and
     * clip before calling this method on children.
     *
     * @since 1.0.0
     */
    public fun render(context: PaintContext) {
        if (!visible || bounds.isEmpty) {
            return
        }
        paint(context)
        paintChildren(context)
    }

    /**
     * Invalidates this widget and every ancestor for measurement and layout.
     *
     * @since 1.0.0
     */
    public fun invalidateLayout() {
        var current: Widget = this
        while (true) {
            current.contentRevision++
            current.cachedConstraints = null
            val next = current.parent ?: break
            current = next
        }
        current.markSurfaceInvalid()
    }

    /**
     * Invalidates the paint cache without changing layout.
     *
     * @since 1.0.0
     */
    public fun invalidatePaint() {
        markSurfacePaintInvalid()
    }

    internal fun measured(constraints: Constraints): IntSize {
        val cached = cachedConstraints
        if (cached == constraints && cachedRevision == contentRevision) {
            return cachedSize
        }
        val size = constraints.constrain(measure(constraints))
        cachedConstraints = constraints
        cachedRevision = contentRevision
        cachedSize = size
        return size
    }

    internal fun attachTree(context: GuiContext? = null) {
        if (attached) {
            return
        }
        guiContext = context
        attached = true
        onAttach()
        attachChildren()
    }

    internal fun detachTree() {
        if (!attached) {
            return
        }
        detachChildren()
        attached = false
        onDetach()
        guiContext = null
    }

    internal fun setParent(parent: Widget?) {
        this.parent = parent
    }

    /**
     * Returns the deepest interactive widget at local ([x], [y]), or `null`.
     *
     * Containers override this to walk children in reverse paint order.
     *
     * @since 1.0.0
     */
    internal open fun hitTest(
        x: Int,
        y: Int,
    ): Widget? {
        if (!visible || !enabled || bounds.isEmpty) {
            return null
        }
        if (x < 0 || y < 0 || x >= bounds.width || y >= bounds.height) {
            return null
        }
        return this
    }

    /**
     * Returns the screen origin of this widget's local origin.
     *
     * @since 1.0.0
     */
    internal fun screenOrigin(): IntSize {
        val point = screenOriginPoint()
        return IntSize(point.x.coerceAtLeast(0), point.y.coerceAtLeast(0))
    }

    internal fun screenOriginPoint(): ScreenPoint {
        var x = 0
        var y = 0
        var current: Widget = this
        while (true) {
            x += current.bounds.x
            y += current.bounds.y
            val parent = current.parent ?: break
            if (parent is ContainerWidget) {
                val content = parent.contentRect
                val scroll = parent.contentScrollOffset()
                x += content.x - parent.bounds.x - scroll.width
                y += content.y - parent.bounds.y - scroll.height
            }
            current = parent
        }
        return ScreenPoint(x, y)
    }

    internal fun setFocused(focused: Boolean) {
        if (isFocused == focused) {
            return
        }
        isFocused = focused
        invalidatePaint()
        onFocusChanged(focused)
    }

    internal fun setHovered(hovered: Boolean) {
        if (isHovered == hovered) {
            return
        }
        isHovered = hovered
        onHoverChanged(hovered)
    }

    internal fun setPressed(pressed: Boolean) {
        isPressed = pressed
    }

    internal fun setSurfaceInvalidation(callback: (() -> Unit)?) {
        surfaceInvalidation = callback
        surfacePaintInvalidation = callback
    }

    internal fun setSurfacePaintInvalidation(callback: (() -> Unit)?) {
        surfacePaintInvalidation = callback
    }

    internal fun isAncestorOf(other: Widget): Boolean {
        var current = other.parent
        while (current != null) {
            if (current === this) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /** Returns whether this widget handles [codePoint] as a mnemonic. */
    internal open fun matchesMnemonic(codePoint: Int): Boolean = false

    /** Activates this widget for [codePoint] when it is a mnemonic target. */
    internal open fun activateMnemonic(codePoint: Int): Boolean = false

    /** Handles a key before normal target routing, used by global menu keys. */
    internal open fun onGlobalKey(event: GuiKeyEvent): Boolean = false

    /** Children in paint order for focus traversal. @since 1.0.0 */
    internal open fun traversalChildren(): List<Widget> = emptyList()

    /** Attaches child widgets. Containers override this. @since 1.0.0 */
    protected open fun attachChildren() {}

    /** Detaches child widgets. Containers override this. @since 1.0.0 */
    protected open fun detachChildren() {}

    private fun markSurfaceInvalid() {
        rootWidget().surfaceInvalidation?.invoke()
    }

    private fun markSurfacePaintInvalid() {
        val root = rootWidget()
        (root.surfacePaintInvalidation ?: root.surfaceInvalidation)?.invoke()
    }

    private fun rootWidget(): Widget {
        var root: Widget = this
        while (true) {
            val next = root.parent ?: break
            root = next
        }
        return root
    }
}
