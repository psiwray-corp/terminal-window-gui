package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect

/**
 * Anchored arbitrary content with flip/clamp placement.
 *
 * A popover is painted through the surface's single [OverlayHost]. It never
 * owns a window or a second overlay layer. Non-modal popovers restore the
 * previous focus when closed.
 *
 * @property placement Preferred side.
 * @property dismissPolicy Dismissal gestures.
 * @property modal Whether the popover traps focus and blocks lower layers.
 * @since 1.0.0
 */
public class Popover(
    content: Widget,
    public var placement: OverlayPlacement = OverlayPlacement.AUTO,
    public var dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    public var modal: Boolean = false,
) : SingleChildWidget(content) {
    private var host: OverlayHost? = null
    private var entryValue: OverlayEntry? = null
    private var previousFocus: Widget? = null

    /** Overlay entry while open, or `null`. @since 1.0.0 */
    public val entry: OverlayEntry? get() = entryValue

    override fun paint(context: com.psiwray.organization.terminal_window_gui.paint.PaintContext) {}

    override fun measure(constraints: Constraints): IntSize {
        val current = child ?: return constraints.constrain(IntSize.ZERO)
        return constraints.constrain(current.measured(constraints.loosen()))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val size = current.measured(Constraints.loose(IntSize(contentRect.width, contentRect.height)))
        current.arrange(
            Rect(
                0,
                0,
                size.width.coerceAtMost(contentRect.width),
                size.height.coerceAtMost(contentRect.height),
            ),
        )
    }

    override fun onAttach() {
        host = guiContext?.overlays
        if (entryValue == null) {
            entryValue = host?.entries?.firstOrNull { it.widget === this }
        }
        if (previousFocus == null) {
            previousFocus = guiContext?.focus?.focused
        }
    }

    override fun onDetach() {
        if (!modal) {
            guiContext?.focus?.focus(previousFocus)
        }
        previousFocus = null
        host = null
        entryValue = null
    }

    /** Opens anchored to a screen rectangle. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        anchor: Rect,
        placement: OverlayPlacement = this.placement,
        dismissPolicy: DismissPolicy = this.dismissPolicy,
        modal: Boolean = this.modal,
    ): OverlayEntry = show(overlays, OverlayAnchor.Rect(anchor), placement, dismissPolicy, modal)

    /** Opens anchored to a widget. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        anchor: Widget,
        placement: OverlayPlacement = this.placement,
        dismissPolicy: DismissPolicy = this.dismissPolicy,
        modal: Boolean = this.modal,
    ): OverlayEntry = show(overlays, OverlayAnchor.Widget(anchor), placement, dismissPolicy, modal)

    /** Opens anchored in screen coordinates. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        column: Int,
        row: Int,
        placement: OverlayPlacement = this.placement,
        dismissPolicy: DismissPolicy = this.dismissPolicy,
        modal: Boolean = this.modal,
    ): OverlayEntry = show(overlays, OverlayAnchor.Rect(Rect(column, row, 0, 0)), placement, dismissPolicy, modal)

    /** Opens as an overlay entry. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        anchor: OverlayAnchor,
        placement: OverlayPlacement = this.placement,
        dismissPolicy: DismissPolicy = this.dismissPolicy,
        modal: Boolean = this.modal,
    ): OverlayEntry {
        if (entryValue != null) {
            return entryValue!!
        }
        this.placement = placement
        this.dismissPolicy = dismissPolicy
        this.modal = modal
        host = overlays
        previousFocus = overlays.focus?.focused
        val added =
            overlays.add(
                widget = this,
                anchor = anchor,
                placement = placement,
                dismissPolicy = dismissPolicy,
                modal = modal,
                shadow = true,
            )
        entryValue = added
        return added
    }

    /** Alias for [dismiss]. @since 1.0.0 */
    public fun hide() {
        dismiss()
    }

    /** Closes the popover. @since 1.0.0 */
    public fun dismiss() {
        val current = host
        val currentEntry = entryValue ?: current?.entries?.firstOrNull { it.widget === this }
        entryValue = null
        if (current != null && currentEntry != null) {
            current.remove(currentEntry)
        }
    }
}
