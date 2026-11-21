package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.input.FocusManager
import com.psiwray.organization.terminal_window_gui.input.HoverState
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.input.Scheduler
import com.psiwray.organization.terminal_window_gui.input.TimerHandle

/**
 * Delay-based tooltip controller for one surface.
 *
 * It observes [HoverState] and [FocusManager] changes and uses only the
 * caller-driven [Scheduler]. One tooltip is visible at a time; leaving the
 * anchor, focus loss, click outside, or Escape cancels/hides it.
 *
 * @property delayMillis Delay before showing a tooltip.
 * @since 1.0.0
 */
public class TooltipController private constructor(
    private val overlays: OverlayHost,
    private val scheduler: Scheduler,
    private val hover: HoverState?,
    private val focus: FocusManager?,
    public val delayMillis: Long,
) {
    private val bindings: MutableMap<Widget, Tooltip> = LinkedHashMap()
    private var pendingHandle: TimerHandle? = null
    private var pendingTarget: Widget? = null
    private var shownEntry: OverlayEntry? = null
    private var shownTarget: Widget? = null
    private var disposed: Boolean = false
    private var suppressRefresh: Boolean = false
    private val hoverListener: (Widget?) -> Unit = { refresh() }
    private val focusListener: (Widget?, Widget?) -> Unit = { _, _ -> refresh() }

    /** Controller-first constructor. @since 1.0.0 */
    public constructor(
        overlays: OverlayHost,
        scheduler: Scheduler,
        delayMillis: Long = 600,
    ) : this(overlays, scheduler, overlays.hover, overlays.focus, delayMillis)

    /** Surface convenience constructor. @since 1.0.0 */
    public constructor(
        surface: GuiSurface,
        delayMillis: Long = 600,
    ) : this(surface.overlays, surface.scheduler, surface.hover, surface.focus, delayMillis)

    /** Whether a delayed tooltip is pending. @since 1.0.0 */
    public val isPending: Boolean get() = pendingHandle != null

    /** Currently shown tooltip entry, or `null`. @since 1.0.0 */
    public val visibleEntry: OverlayEntry? get() = shownEntry

    /** Anchor of the visible tooltip, or `null`. @since 1.0.0 */
    public val visibleTarget: Widget? get() = shownTarget

    init {
        require(delayMillis >= 0) { "delayMillis must be non-negative, got $delayMillis" }
        hover?.addHoverListener(hoverListener)
        focus?.addFocusListener(focusListener)
    }

    /** Associates [tooltip] with [target]. @since 1.0.0 */
    public fun register(
        target: Widget,
        tooltip: Tooltip,
    ): TooltipController {
        bindings[target] = tooltip
        tooltip.onDetachHook = { if (shownTarget === target) hide() }
        return this
    }

    /** Alias for [register]. @since 1.0.0 */
    public fun bind(
        target: Widget,
        tooltip: Tooltip,
    ): TooltipController = register(target, tooltip)

    /** Alias for [register]. @since 1.0.0 */
    public fun attach(
        target: Widget,
        tooltip: Tooltip,
    ): TooltipController = register(target, tooltip)

    /** Alias for [unregister]. @since 1.0.0 */
    public fun detach(target: Widget): Tooltip? = unregister(target)

    /** Registers a fresh text tooltip. @since 1.0.0 */
    public fun register(
        target: Widget,
        text: String,
        maxWidth: Int = 40,
    ): TooltipController = register(target, Tooltip(text, maxWidth))

    /** Removes a target binding. @since 1.0.0 */
    public fun unregister(target: Widget): Tooltip? {
        val removed = bindings.remove(target)
        if (shownTarget === target || pendingTarget === target) {
            hide()
        }
        return removed
    }

    /** Shows [tooltip] immediately for [target]. @since 1.0.0 */
    public fun showNow(
        target: Widget,
        tooltip: Tooltip? = bindings[target],
    ): OverlayEntry? {
        if (disposed || tooltip == null) {
            return null
        }
        cancelPending()
        if (shownTarget === target && shownEntry != null) {
            return shownEntry
        }
        suppressRefresh = true
        hideShown()
        var created: OverlayEntry? = null
        val entry =
            overlays.add(
                widget = tooltip,
                anchor = OverlayAnchor.Widget(target),
                placement = OverlayPlacement.AUTO,
                dismissPolicy = com.psiwray.organization.terminal_window_gui.input.DismissPolicy.BOTH,
                modal = false,
                shadow = true,
                onDismiss = {
                    if (shownEntry === created) {
                        shownEntry = null
                        shownTarget = null
                    }
                },
            )
        created = entry
        shownEntry = entry
        shownTarget = target
        suppressRefresh = false
        return entry
    }

    /** Alias for [showNow]. @since 1.0.0 */
    public fun show(
        target: Widget,
        tooltip: Tooltip? = bindings[target],
    ): OverlayEntry? = showNow(target, tooltip)

    /** Schedules a delayed show for [target]. @since 1.0.0 */
    public fun schedule(target: Widget) {
        if (disposed || !bindings.containsKey(target)) {
            return
        }
        cancelPending()
        cancelShown()
        pendingTarget = target
        pendingHandle =
            scheduler.postDelayed(delayMillis) {
                pendingHandle = null
                val current = pendingTarget
                pendingTarget = null
                if (!disposed && current != null && activeTarget() === current) {
                    showNow(current)
                }
            }
    }

    /** Cancels a pending tooltip and hides the visible one. @since 1.0.0 */
    public fun hide() {
        cancelPending()
        hideShown()
    }

    /** Alias for [hide]. @since 1.0.0 */
    public fun hideNow() {
        hide()
    }

    /** Re-evaluates the current hover/focus target. @since 1.0.0 */
    public fun update() {
        refresh()
    }

    /** Cancels pending work, hides, and detaches observers. @since 1.0.0 */
    public fun dispose() {
        if (disposed) {
            return
        }
        disposed = true
        hide()
        hover?.removeHoverListener(hoverListener)
        focus?.removeFocusListener(focusListener)
        bindings.clear()
    }

    private fun refresh() {
        if (disposed || suppressRefresh) {
            return
        }
        val target = activeTarget()
        if (target === shownTarget && shownEntry != null) {
            cancelPending()
            return
        }
        if (target === pendingTarget && pendingHandle != null) {
            return
        }
        cancelPending()
        hideShown()
        if (target != null) {
            schedule(target)
        }
    }

    private fun activeTarget(): Widget? {
        val hovered = hover?.hovered
        if (hovered != null) {
            matching(hovered)?.let { return it }
        }
        val focused = focus?.focused
        if (focused != null) {
            matching(focused)?.let { return it }
        }
        return null
    }

    private fun matching(widget: Widget): Widget? =
        bindings.keys.firstOrNull { target -> target === widget || target.isAncestorOf(widget) }

    private fun cancelPending() {
        pendingHandle?.let(scheduler::cancel)
        pendingHandle = null
        pendingTarget = null
    }

    private fun cancelShown() {
        hideShown()
    }

    private fun hideShown() {
        val entry = shownEntry
        shownEntry = null
        shownTarget = null
        if (entry != null) {
            overlays.remove(entry)
        }
    }
}
