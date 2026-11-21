package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.input.Scheduler
import com.psiwray.organization.terminal_window_gui.input.TimerHandle
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.MainAxisSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import java.util.Collections

/**
 * Stacked non-modal toasts in one screen corner.
 *
 * At most [maxVisible] messages are shown. Auto-dismiss timers use only the
 * caller-driven [Scheduler]; cancellation happens on manual dismissal, eviction,
 * clear, or dispose.
 *
 * @property maxVisible Maximum simultaneous toasts.
 * @property corner Screen corner.
 * @property defaultDurationMillis Default auto-dismiss delay.
 * @since 1.0.0
 */
public class ToastManager(
    private val overlays: OverlayHost,
    private val scheduler: Scheduler,
    public val maxVisible: Int = 3,
    public val corner: ToastCorner = ToastCorner.BOTTOM_RIGHT,
    public val defaultDurationMillis: Long = 3000,
) {
    private val layer: ToastLayer = ToastLayer()
    private val active: MutableList<Toast> = ArrayList()
    private val timers: MutableMap<Toast, TimerHandle> = LinkedHashMap()
    private var entry: OverlayEntry? = null

    /** Surface convenience constructor. @since 1.0.0 */
    public constructor(
        surface: GuiSurface,
        maxVisible: Int = 3,
        corner: ToastCorner = ToastCorner.BOTTOM_RIGHT,
        defaultDurationMillis: Long = 3000,
    ) : this(surface.overlays, surface.scheduler, maxVisible, corner, defaultDurationMillis)

    init {
        require(maxVisible > 0) { "maxVisible must be positive, got $maxVisible" }
        require(defaultDurationMillis >= 0) { "duration must be non-negative, got $defaultDurationMillis" }
        layer.onDetachHook = { onLayerDetached() }
    }

    /** Currently visible toasts in display order. @since 1.0.0 */
    public val toasts: List<Toast> get() = Collections.unmodifiableList(active)

    /** Shows a message and returns its toast. @since 1.0.0 */
    public fun show(
        message: String,
        severity: Severity = Severity.INFO,
        durationMillis: Long = defaultDurationMillis,
    ): Toast {
        val toast = Toast(message, severity, durationMillis)
        toast.onClose = { dismiss(toast) }
        active.add(toast)
        while (active.size > maxVisible) {
            val oldest = active.first()
            dismiss(oldest)
        }
        updateLayer()
        ensureEntry()
        schedule(toast)
        return toast
    }

    /** Shows a prebuilt toast. @since 1.0.0 */
    public fun show(toast: Toast): Toast {
        toast.onClose = { dismiss(toast) }
        active.add(toast)
        while (active.size > maxVisible) {
            dismiss(active.first())
        }
        updateLayer()
        ensureEntry()
        schedule(toast)
        return toast
    }

    /** Dismisses [toast] and cancels its timer. @since 1.0.0 */
    public fun dismiss(toast: Toast) {
        timers.remove(toast)?.let(scheduler::cancel)
        if (!active.remove(toast)) {
            return
        }
        updateLayer()
        if (active.isEmpty()) {
            removeEntry()
        }
    }

    /** Dismisses every toast. @since 1.0.0 */
    public fun clear() {
        for (timer in timers.values) {
            scheduler.cancel(timer)
        }
        timers.clear()
        active.clear()
        updateLayer()
        removeEntry()
    }

    /** Alias for [clear]. @since 1.0.0 */
    public fun dismissAll() {
        clear()
    }

    /** Cancels timers and removes the overlay. @since 1.0.0 */
    public fun dispose() {
        clear()
    }

    private fun schedule(toast: Toast) {
        if (toast.durationMillis <= 0) {
            return
        }
        timers.remove(toast)?.let(scheduler::cancel)
        timers[toast] =
            scheduler.postDelayed(toast.durationMillis) {
                dismiss(toast)
            }
    }

    private fun updateLayer() {
        layer.setToasts(active.toList())
    }

    private fun ensureEntry() {
        if (entry != null) {
            return
        }
        val screen = overlays.screen
        val x = if (corner == ToastCorner.BOTTOM_RIGHT || corner == ToastCorner.TOP_RIGHT) screen.right else screen.x
        val y = if (corner == ToastCorner.BOTTOM_RIGHT || corner == ToastCorner.BOTTOM_LEFT) screen.bottom else screen.y
        val placement =
            if (corner == ToastCorner.BOTTOM_RIGHT || corner == ToastCorner.BOTTOM_LEFT) {
                OverlayPlacement.ABOVE
            } else {
                OverlayPlacement.BELOW
            }
        entry =
            overlays.add(
                widget = layer,
                anchor = OverlayAnchor.Rect(Rect(x, y, 0, 0)),
                placement = placement,
                dismissPolicy = DismissPolicy.NONE,
                modal = false,
                shadow = true,
            )
    }

    private fun removeEntry() {
        val current = entry
        entry = null
        if (current != null) {
            overlays.remove(current)
        }
    }

    private fun onLayerDetached() {
        if (entry == null) {
            return
        }
        entry = null
        for (timer in timers.values) {
            scheduler.cancel(timer)
        }
        timers.clear()
        active.clear()
    }
}

/** Internal vertical stack used by [ToastManager]. */
internal class ToastLayer : com.psiwray.organization.terminal_window_gui.widget.ContainerWidget() {
    private val column: Column = Column(mainAxisSize = MainAxisSize.MIN)
    internal var onDetachHook: (() -> Unit)? = null

    init {
        adopt(column)
    }

    fun setToasts(toasts: List<Toast>) {
        column.clear()
        for (toast in toasts) {
            column.add(toast)
        }
        invalidateLayout()
    }

    override fun paint(context: PaintContext) {}

    override fun onDetach() {
        onDetachHook?.invoke()
    }

    override fun measure(constraints: Constraints): IntSize {
        val size = column.measured(Constraints(0, constraints.maxWidth, 0, constraints.maxHeight))
        return constraints.constrain(size)
    }

    override fun arrangeChildren() {
        column.arrange(Rect(0, 0, contentRect.width, contentRect.height))
    }
}
