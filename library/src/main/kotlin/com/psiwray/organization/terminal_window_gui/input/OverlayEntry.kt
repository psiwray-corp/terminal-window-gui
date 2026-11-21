package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * One overlay layer with its anchor, placement, and dismissal rules.
 *
 * @property widget Overlay content.
 * @property anchor Screen-space anchor.
 * @property placement Preferred side.
 * @property dismissPolicy Dismissal gestures.
 * @property modal Whether the layer blocks the tree below and traps focus.
 * @property shadow Whether a drop shadow is requested.
 * @property onDismiss Optional callback invoked after dismissal.
 * @since 1.0.0
 */
public class OverlayEntry(
    public val widget: Widget,
    public val anchor: OverlayAnchor = OverlayAnchor.Screen,
    public val placement: OverlayPlacement = OverlayPlacement.AUTO,
    public val dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    public val modal: Boolean = false,
    public val shadow: Boolean = false,
    public val onDismiss: (() -> Unit)? = null,
) {
    /** Computed screen rectangle after layout. @since 1.0.0 */
    public var screenRect: Rect = Rect.EMPTY
        internal set

    /** Alias for [screenRect]. @since 1.0.0 */
    public val bounds: Rect get() = screenRect

    internal var savedFocus: Widget? = null
}
