package com.psiwray.organization.terminal_window_gui.examples.custom

import com.psiwray.organization.terminal_window_gui.examples.ExampleApp
import com.psiwray.organization.terminal_window_gui.examples.ExampleLoop
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.layout.MainAxisAlignment
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Column
import com.psiwray.organization.terminal_window_gui.widget.Label
import com.psiwray.organization.terminal_window_gui.widget.Panel
import com.psiwray.organization.terminal_window_gui.widget.Row
import com.psiwray.organization.terminal_window_gui.widget.Separator
import com.psiwray.organization.terminal_window_gui.widget.SizedBox
import com.psiwray.organization.terminal_window_gui.widget.Widget

/** Entry point for the custom-widget demo. */
public fun main(args: Array<String>) {
    ExampleLoop.run(CustomApp(), args)
}

private class CustomApp : ExampleApp("terminal-window-gui | custom sparkline", columns = 64, rows = 20) {
    override val initialTheme: Theme get() = Themes.dark()

    override fun root(): Widget {
        val values = listOf(0.08, 0.18, 0.31, 0.24, 0.52, 0.41, 0.68, 0.59, 0.82, 0.74, 0.93, 0.88)
        return Panel(
            title = "Custom widget: Sparkline",
            child =
                Column(
                    gap = 1,
                    mainAxisAlignment = MainAxisAlignment.START,
                    crossAxisAlignment = CrossAxisAlignment.START,
                    children =
                        listOf(
                            Label("A one-row chart painted with PaintContext and PROGRESS_* shapes."),
                            SizedBox(height = 1, child = Sparkline(values)),
                            Row(
                                gap = 2,
                                children =
                                    listOf(
                                        Label("0", foreground = null),
                                        Label("values: ${values.size}"),
                                        Label("max: 1.0"),
                                    ),
                            ),
                            Separator(),
                            Row(
                                gap = 1,
                                children =
                                    listOf(
                                        Label("legend: "),
                                        Label("empty"),
                                        Label("1/8"),
                                        Label("1/2"),
                                        Label("full"),
                                    ),
                            ),
                            Label("The widget has no glyph constants and overrides only measure/paint."),
                        ),
                ),
        )
    }
}
