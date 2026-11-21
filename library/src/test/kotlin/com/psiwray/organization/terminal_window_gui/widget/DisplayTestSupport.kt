package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness

internal fun displayHarness(
    widget: Widget,
    columns: Int,
    rows: Int,
    theme: Theme = Themes.dark(),
    shapes: ResolvedShapeSet = ShapeSets.unicode().resolve(GuiFonts.bundled()),
): GuiTestHarness =
    GuiTestHarness(
        Center(SizedBox(width = columns, height = rows, child = widget)),
        theme,
        shapes,
        columns,
        rows,
    )
