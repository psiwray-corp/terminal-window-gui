package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.OverlayPlacement
import com.psiwray.organization.terminal_window_gui.layout.Rect

/**
 * Non-modal popup menu opened by the caller at a screen cell or widget anchor.
 *
 * It uses the same entry rules as [MenuPopup] but has no owning [MenuBar].
 * Dismissal on click-outside or Escape restores the previous focus.
 *
 * @since 1.0.0
 */
public class ContextMenu(
    items: List<MenuItem>,
) : MenuPopup(items) {
    /** Child-first constructor for a menu model. @since 1.0.0 */
    public constructor(menu: Menu) : this(menu.items)

    /** Opens at a screen cell. @since 1.0.0 */
    public fun open(
        overlays: OverlayHost,
        column: Int,
        row: Int,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = openAt(overlays, column, row, placement, dismissPolicy)

    /** Opens against a widget anchor. @since 1.0.0 */
    public fun open(
        overlays: OverlayHost,
        anchor: Widget,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = open(overlays, OverlayAnchor.Widget(anchor), placement, dismissPolicy)

    /** Opens against an explicit screen rectangle. @since 1.0.0 */
    public fun open(
        overlays: OverlayHost,
        anchor: Rect,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = open(overlays, OverlayAnchor.Rect(anchor), placement, dismissPolicy)

    /** Opens at a screen cell. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        column: Int,
        row: Int,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = openAt(overlays, column, row, placement, dismissPolicy)

    /** Opens at a screen cell. @since 1.0.0 */
    public fun showAt(
        overlays: OverlayHost,
        column: Int,
        row: Int,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = openAt(overlays, column, row, placement, dismissPolicy)

    /** Opens against a widget anchor. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        anchor: Widget,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = open(overlays, OverlayAnchor.Widget(anchor), placement, dismissPolicy)

    /** Opens against an explicit screen rectangle. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        anchor: Rect,
        placement: OverlayPlacement = OverlayPlacement.BELOW,
        dismissPolicy: DismissPolicy = DismissPolicy.BOTH,
    ): OverlayEntry = open(overlays, OverlayAnchor.Rect(anchor), placement, dismissPolicy)
}
