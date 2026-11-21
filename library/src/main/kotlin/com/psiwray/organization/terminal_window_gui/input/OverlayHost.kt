package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.GridSize
import com.psiwray.organization.terminal_window_gui.internal.GuiContext
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.widget.MenuPopup
import com.psiwray.organization.terminal_window_gui.widget.Widget
import java.util.Collections
import kotlin.math.max

/**
 * Ordered overlay layers painted after the root tree.
 *
 * Entry order is z-order: the last entry is topmost. Modal entries block the
 * tree below, trap focus, and restore the previous focus when removed.
 *
 * @property screen Screen rectangle used for placement and clipping.
 * @property focus Focus manager shared with the surface, if any.
 * @property hover Hover tracker shared with the surface, if any.
 * @since 1.0.0
 */
public class OverlayHost(
    screen: Rect = Rect(0, 0, 80, 24),
    public val focus: FocusManager? = null,
    public val hover: HoverState? = null,
) {
    private val entriesList: MutableList<OverlayEntry> = ArrayList()
    private var surfaceInvalidation: (() -> Unit)? = null
    private var surfacePaintInvalidation: (() -> Unit)? = null
    private var guiContext: GuiContext? = null

    /** Screen rectangle used for placement and clipping. @since 1.0.0 */
    public var screen: Rect = screen
        set(value) {
            if (field == value) {
                return
            }
            field = value
            refresh()
        }

    /** Immutable view of entries in paint order. @since 1.0.0 */
    public val entries: List<OverlayEntry> get() = Collections.unmodifiableList(entriesList)

    /** Whether any modal entry is open. @since 1.0.0 */
    public val hasModal: Boolean get() = entriesList.any { it.modal }

    /** Alias for [hasModal]. @since 1.0.0 */
    public val isModal: Boolean get() = hasModal

    /** Topmost entry, or `null`. @since 1.0.0 */
    public val topmost: OverlayEntry? get() = entriesList.lastOrNull()

    /** Topmost modal entry, or `null`. @since 1.0.0 */
    public val topmostModal: OverlayEntry?
        get() {
            var index = entriesList.size - 1
            while (index >= 0) {
                val entry = entriesList[index]
                if (entry.modal) {
                    return entry
                }
                index--
            }
            return null
        }

    /**
     * Creates a host from grid dimensions.
     *
     * @since 1.0.0
     */
    public constructor(
        screen: GridSize,
        focus: FocusManager? = null,
        hover: HoverState? = null,
    ) : this(Rect(0, 0, screen.columns, screen.rows), focus, hover)

    /**
     * Creates a host sized to [focus]'s root.
     *
     * @since 1.0.0
     */
    public constructor(
        manager: FocusManager,
        hover: HoverState? = null,
    ) : this(Rect(0, 0, manager.root.bounds.width, manager.root.bounds.height), manager, hover)

    /**
     * Creates a host sized to [root].
     *
     * @since 1.0.0
     */
    public constructor(
        root: Widget,
        focus: FocusManager? = null,
        hover: HoverState? = null,
    ) : this(Rect(0, 0, root.bounds.width, root.bounds.height), focus, hover)

    /**
     * Creates a host from explicit columns and rows.
     *
     * @since 1.0.0
     */
    public constructor(
        columns: Int,
        rows: Int,
        focus: FocusManager? = null,
        hover: HoverState? = null,
    ) : this(Rect(0, 0, columns, rows), focus, hover)

    /**
     * Adds [entry] at the top of the stack.
     *
     * @since 1.0.0
     */
    public fun add(entry: OverlayEntry): OverlayEntry {
        require(entriesList.none { it === entry }) { "overlay entry is already attached" }
        entry.savedFocus = focus?.focused
        entriesList.add(entry)
        entry.widget.setSurfaceInvalidation(surfaceInvalidation)
        entry.widget.setSurfacePaintInvalidation(surfacePaintInvalidation)
        entry.widget.attachTree(guiContext)
        refreshEntry(entry)
        focus?.addRoot(entry.widget)
        val manager = focus
        if (entry.modal && manager != null) {
            manager.pushTrap(entry.widget)
            if (!manager.focusFirst()) {
                manager.clearFocus()
            }
        } else if (entry.widget is MenuPopup) {
            manager?.focus(entry.widget)
        }
        markInvalidated()
        return entry
    }

    /**
     * Creates and adds an entry.
     *
     * @since 1.0.0
     */
    public fun add(
        widget: Widget,
        anchor: OverlayAnchor = OverlayAnchor.Screen,
        placement: OverlayPlacement = OverlayPlacement.AUTO,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
        modal: Boolean = false,
        shadow: Boolean = false,
        onDismiss: (() -> Unit)? = null,
    ): OverlayEntry =
        add(
            OverlayEntry(
                widget = widget,
                anchor = anchor,
                placement = placement,
                dismissPolicy = dismissPolicy,
                modal = modal,
                shadow = shadow,
                onDismiss = onDismiss,
            ),
        )

    /** Removes [entry] without invoking its dismiss callback. @since 1.0.0 */
    public fun remove(entry: OverlayEntry): Boolean = remove(entry, invokeDismiss = false)

    /**
     * Dismisses [entry], invoking its callback.
     *
     * @since 1.0.0
     */
    public fun dismiss(entry: OverlayEntry): Boolean = remove(entry, invokeDismiss = true)

    /** Dismisses the topmost entry. @since 1.0.0 */
    public fun dismissTop(): OverlayEntry? {
        val entry = entriesList.lastOrNull() ?: return null
        dismiss(entry)
        return entry
    }

    /**
     * Dismisses the topmost entry when Escape is allowed.
     *
     * @return whether an entry was dismissed.
     * @since 1.0.0
     */
    public fun dismissEscape(): Boolean {
        val entry = entriesList.lastOrNull() ?: return false
        if (entry.dismissPolicy != DismissPolicy.ESCAPE && entry.dismissPolicy != DismissPolicy.BOTH) {
            return false
        }
        return dismiss(entry)
    }

    /**
     * Dismisses a non-modal click-outside entry when the press is outside it.
     *
     * Clicks inside the overlay, including child popups, do not dismiss.
     *
     * @return whether an entry was dismissed.
     * @since 1.0.0
     */
    public fun dismissOutside(
        screenX: Int,
        screenY: Int,
    ): Boolean {
        val entry = entriesList.lastOrNull() ?: return false
        if (entry.modal || entry.screenRect.contains(screenX, screenY)) {
            return false
        }
        val menuPopup = entry.widget as? MenuPopup
        val menuBar = menuPopup?.menuBar
        if (menuBar != null) {
            val origin = menuBar.screenOriginPoint()
            if (Rect(origin.x, origin.y, menuBar.bounds.width, menuBar.bounds.height).contains(screenX, screenY)) {
                return false
            }
        }
        if (entry.dismissPolicy == DismissPolicy.CLICK_OUTSIDE ||
            entry.dismissPolicy == DismissPolicy.BOTH
        ) {
            return dismiss(entry)
        }
        return false
    }

    /**
     * Returns the topmost entry containing the screen point.
     *
     * @since 1.0.0
     */
    public fun hitTest(
        screenX: Int,
        screenY: Int,
    ): OverlayEntry? {
        var index = entriesList.size - 1
        while (index >= 0) {
            val entry = entriesList[index]
            if (entry.widget.visible && entry.screenRect.contains(screenX, screenY)) {
                return entry
            }
            index--
        }
        return null
    }

    /**
     * Returns the deepest widget in [entry] at a screen point.
     *
     * @since 1.0.0
     */
    public fun targetAt(
        entry: OverlayEntry,
        screenX: Int,
        screenY: Int,
    ): Widget {
        val rect = entry.screenRect
        return entry.widget.hitTest(screenX - rect.x, screenY - rect.y) ?: entry.widget
    }

    /**
     * Recomputes every entry's size and screen rectangle.
     *
     * @since 1.0.0
     */
    public fun refresh() {
        for (entry in entriesList) {
            refreshEntry(entry)
        }
    }

    /**
     * Paints entries in z-order. Call after painting the root.
     *
     * @since 1.0.0
     */
    public fun paint(context: PaintContext) {
        refresh()
        for (entry in entriesList) {
            val rect = entry.screenRect
            if (rect.isEmpty) {
                continue
            }
            context.pushClip(rect)
            context.pushOrigin(rect.x, rect.y)
            try {
                entry.widget.render(context)
            } finally {
                context.popOrigin()
                context.popClip()
            }
        }
    }

    /** Removes every entry. @since 1.0.0 */
    public fun clear() {
        var index = entriesList.size - 1
        while (index >= 0) {
            remove(entriesList[index], invokeDismiss = false)
            index--
        }
    }

    private fun remove(
        entry: OverlayEntry,
        invokeDismiss: Boolean,
    ): Boolean {
        if (!entriesList.remove(entry)) {
            return false
        }
        entry.widget.detachTree()
        entry.widget.setSurfaceInvalidation(null)
        entry.widget.setSurfacePaintInvalidation(null)
        focus?.removeRoot(entry.widget)
        if (entry.modal) {
            focus?.popTrap(entry.widget)
            val previous = entry.savedFocus
            if (previous != null && previous !== entry.widget) {
                focus?.restore(previous)
            }
        }
        if (invokeDismiss) {
            entry.onDismiss?.invoke()
        }
        val hovered = hover?.hovered
        if (hovered != null && (hovered === entry.widget || entry.widget.isAncestorOf(hovered))) {
            hover.clear()
        }
        markInvalidated()
        return true
    }

    internal fun setGuiContext(context: GuiContext?) {
        guiContext = context
    }

    internal fun setSurfaceInvalidation(
        layout: (() -> Unit)?,
        paint: (() -> Unit)?,
    ) {
        surfaceInvalidation = layout
        surfacePaintInvalidation = paint
        for (entry in entriesList) {
            entry.widget.setSurfaceInvalidation(layout)
            entry.widget.setSurfacePaintInvalidation(paint)
        }
    }

    private fun markInvalidated() {
        surfaceInvalidation?.invoke()
        surfacePaintInvalidation?.invoke()
    }

    private fun refreshEntry(entry: OverlayEntry) {
        val constraints = Constraints.loose(IntSize(screen.width, screen.height))
        val size = entry.widget.layout(constraints)
        entry.screenRect = place(entry, size)
    }

    private fun place(
        entry: OverlayEntry,
        size: IntSize,
    ): Rect {
        val anchor = anchorRect(entry.anchor)
        if (anchor == null) {
            return Rect.EMPTY
        }
        val width = size.width
        val height = size.height
        if (entry.anchor is OverlayAnchor.Screen) {
            return clamp(
                Rect(
                    screen.x + max(0, (screen.width - width) / 2),
                    screen.y + max(0, (screen.height - height) / 2),
                    width,
                    height,
                ),
                width,
                height,
            )
        }
        val preferred =
            when (entry.placement) {
                OverlayPlacement.ABOVE -> {
                    OverlayPlacement.ABOVE
                }

                OverlayPlacement.BELOW -> {
                    OverlayPlacement.BELOW
                }

                OverlayPlacement.LEFT -> {
                    OverlayPlacement.LEFT
                }

                OverlayPlacement.RIGHT -> {
                    OverlayPlacement.RIGHT
                }

                OverlayPlacement.AUTO -> {
                    if (anchor.bottom + height <=
                        screen.bottom
                    ) {
                        OverlayPlacement.BELOW
                    } else {
                        OverlayPlacement.ABOVE
                    }
                }
            }
        val fitted =
            if (fits(anchor, preferred, width, height)) {
                preferred
            } else {
                val alternative =
                    when (preferred) {
                        OverlayPlacement.ABOVE -> OverlayPlacement.BELOW
                        OverlayPlacement.BELOW -> OverlayPlacement.ABOVE
                        OverlayPlacement.LEFT -> OverlayPlacement.RIGHT
                        OverlayPlacement.RIGHT -> OverlayPlacement.LEFT
                        OverlayPlacement.AUTO -> OverlayPlacement.BELOW
                    }
                if (fits(anchor, alternative, width, height)) alternative else preferred
            }
        val x: Int
        val y: Int
        when (fitted) {
            OverlayPlacement.ABOVE -> {
                x = anchor.x
                y = anchor.y - height
            }

            OverlayPlacement.BELOW -> {
                x = anchor.x
                y = anchor.bottom
            }

            OverlayPlacement.LEFT -> {
                x = anchor.x - width
                y = anchor.y
            }

            OverlayPlacement.RIGHT -> {
                x = anchor.right
                y = anchor.y
            }

            OverlayPlacement.AUTO -> {
                x = anchor.x
                y = anchor.bottom
            }
        }
        return clamp(x, y, width, height)
    }

    private fun anchorRect(anchor: OverlayAnchor): Rect? =
        when (anchor) {
            OverlayAnchor.Screen -> {
                screen
            }

            is OverlayAnchor.Widget -> {
                val origin = anchor.widget.screenOriginPoint()
                Rect(origin.x, origin.y, anchor.widget.bounds.width, anchor.widget.bounds.height)
            }

            is OverlayAnchor.Rect -> {
                anchor.rect
            }
        }

    private fun fits(
        anchor: Rect,
        placement: OverlayPlacement,
        width: Int,
        height: Int,
    ): Boolean =
        when (placement) {
            OverlayPlacement.ABOVE -> anchor.y - height >= screen.y
            OverlayPlacement.BELOW -> anchor.bottom + height <= screen.bottom
            OverlayPlacement.LEFT -> anchor.x - width >= screen.x
            OverlayPlacement.RIGHT -> anchor.right + width <= screen.right
            OverlayPlacement.AUTO -> false
        }

    private fun clamp(
        rect: Rect,
        width: Int,
        height: Int,
    ): Rect {
        val maxX = screen.x + max(0, screen.width - width)
        val maxY = screen.y + max(0, screen.height - height)
        return Rect(
            rect.x.coerceIn(screen.x, maxX),
            rect.y.coerceIn(screen.y, maxY),
            width,
            height,
        )
    }

    private fun clamp(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Rect {
        val maxX = screen.x + max(0, screen.width - width)
        val maxY = screen.y + max(0, screen.height - height)
        return Rect(x.coerceIn(screen.x, maxX), y.coerceIn(screen.y, maxY), width, height)
    }
}
