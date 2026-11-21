package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.layout.Alignment

/**
 * Screen-space anchor for an overlay.
 *
 * @since 1.0.0
 */
public sealed interface OverlayAnchor {
    /** Anchors to the whole surface. @since 1.0.0 */
    public data object Screen : OverlayAnchor

    /**
     * Anchors to a widget's screen bounds.
     *
     * @property widget Anchor widget.
     * @property alignment Alignment hint within the anchor.
     * @since 1.0.0
     */
    public data class Widget(
        public val widget: com.psiwray.organization.terminal_window_gui.widget.Widget,
        public val alignment: Alignment = Alignment.BOTTOM_START,
    ) : OverlayAnchor

    /**
     * Anchors to an explicit screen rectangle.
     *
     * @property rect Screen rectangle.
     * @since 1.0.0
     */
    public data class Rect(
        public val rect: com.psiwray.organization.terminal_window_gui.layout.Rect,
    ) : OverlayAnchor
}
