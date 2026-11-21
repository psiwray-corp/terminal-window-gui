package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.input.Clipboard
import com.psiwray.organization.terminal_window_gui.input.FocusManager
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.Scheduler
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.style.Theme

internal class GuiContext(
    var theme: Theme,
    var shapes: ResolvedShapeSet,
    val focus: FocusManager,
    val overlays: OverlayHost,
    val scheduler: Scheduler,
    val clipboard: Clipboard,
)
