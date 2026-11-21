package com.psiwray.organization.terminal_window_gui.examples.dashboard

import com.psiwray.organization.terminal_window.TerminalWindow
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.examples.ExampleApp
import com.psiwray.organization.terminal_window_gui.examples.ExampleLoop
import com.psiwray.organization.terminal_window_gui.layout.CrossAxisAlignment
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.AppShell
import com.psiwray.organization.terminal_window_gui.widget.Banner
import com.psiwray.organization.terminal_window_gui.widget.Column
import com.psiwray.organization.terminal_window_gui.widget.Expanded
import com.psiwray.organization.terminal_window_gui.widget.Label
import com.psiwray.organization.terminal_window_gui.widget.LogView
import com.psiwray.organization.terminal_window_gui.widget.Menu
import com.psiwray.organization.terminal_window_gui.widget.MenuBar
import com.psiwray.organization.terminal_window_gui.widget.MenuItem
import com.psiwray.organization.terminal_window_gui.widget.Panel
import com.psiwray.organization.terminal_window_gui.widget.ProgressBar
import com.psiwray.organization.terminal_window_gui.widget.Row
import com.psiwray.organization.terminal_window_gui.widget.Severity
import com.psiwray.organization.terminal_window_gui.widget.SizedBox
import com.psiwray.organization.terminal_window_gui.widget.StatusBar
import com.psiwray.organization.terminal_window_gui.widget.Tab
import com.psiwray.organization.terminal_window_gui.widget.TabView
import com.psiwray.organization.terminal_window_gui.widget.TableCell
import com.psiwray.organization.terminal_window_gui.widget.TableColumn
import com.psiwray.organization.terminal_window_gui.widget.TableView
import com.psiwray.organization.terminal_window_gui.widget.Widget

/** Entry point for the responsive dashboard. */
public fun main(args: Array<String>) {
    ExampleLoop.run(DashboardApp(), args)
}

private data class Metric(
    val name: String,
    val value: String,
    val state: String,
)

private class DashboardApp : ExampleApp("terminal-window-gui | dashboard", columns = 110, rows = 30) {
    private lateinit var surface: GuiSurface
    private lateinit var window: TerminalWindow
    private lateinit var shell: AppShell
    private lateinit var status: StatusBar
    private lateinit var log: LogView
    private lateinit var table: TableView<Metric>
    private val bars = ArrayList<ProgressBar>()
    private var compact: Boolean? = null
    private var lastUpdate = 0L
    private var tick = 0
    private var theme = Themes.dark()
    private val metrics =
        mutableListOf(
            Metric("API", "99.9%", "healthy"),
            Metric("Renderer", "12.4k", "healthy"),
            Metric("Queue", "37", "warning"),
            Metric("Errors", "0", "healthy"),
            Metric("Workers", "8/8", "healthy"),
        )
    private val logLines =
        mutableListOf(
            "12:00:00 boot: dashboard ready",
            "12:00:01 render: 60 fps",
            "12:00:02 queue: depth 37",
            "12:00:03 worker: pool healthy",
        )

    override val initialTheme: Theme get() = theme

    override fun configure(
        surface: GuiSurface,
        window: TerminalWindow,
    ) {
        this.surface = surface
        this.window = window
        log = LogView(lines = logLines.toList(), followTail = true)
        surface.keymap.bind(KeyChord(Key.F1), Command("theme", action = { cycleTheme() }))
        buildLayout(surface.size.columns < 100)
    }

    override fun root(): Widget {
        status = StatusBar(left = "Dashboard", right = "")
        shell = AppShell(menuBar = buildMenu(), content = Label("loading"), statusBar = status)
        return shell
    }

    override fun onFrame(
        surface: GuiSurface,
        nowNanos: Long,
    ) {
        val nextCompact = surface.size.columns < 100
        if (compact != nextCompact) {
            buildLayout(nextCompact)
        }
        if (nowNanos - lastUpdate >= 600_000_000L) {
            lastUpdate = nowNanos
            tick++
            bars.getOrNull(0)?.value = 0.35 + (tick % 5) * 0.1
            bars.getOrNull(1)?.value = 0.55 + (tick % 3) * 0.1
            logLines += "12:00:${(tick % 60).toString().padStart(2, '0')} tick: queue ${37 + tick % 5}"
            log.lines = logLines.toList()
            if (log.followTail) {
                log.scroll.end()
            }
            status.right =
                "${surface.size.columns}x${surface.size.rows} | ${if (nextCompact) "compact" else "wide"} | F1 theme"
            surface.requestPaint()
        }
    }

    private fun buildLayout(nextCompact: Boolean) {
        compact = nextCompact
        val overview =
            if (nextCompact) {
                Column(
                    gap = 1,
                    crossAxisAlignment = CrossAxisAlignment.START,
                    children =
                        listOf(
                            SizedBox(height = 6, child = buildTable()),
                            buildProgressPanel(),
                            SizedBox(height = 5, child = log),
                        ),
                )
            } else {
                Row(
                    gap = 1,
                    children =
                        listOf(
                            Expanded(child = buildTable()),
                            SizedBox(width = 34, child = buildProgressPanel()),
                        ),
                )
            }
        val tabs =
            TabView(
                tabs =
                    listOf(
                        Tab("Overview", overview),
                        Tab("Logs", log),
                        Tab("Metrics", buildTable()),
                    ),
            )
        val content =
            Column(
                gap = 1,
                children =
                    listOf(
                        Banner(
                            message =
                                if (nextCompact) {
                                    "Compact layout (< 100 columns): stacked panels, tabs, and follow-tail log."
                                } else {
                                    "Wide layout (>= 100 columns): table and progress side by side, tabs and follow-tail log."
                                },
                            severity = Severity.INFO,
                            closable = false,
                        ),
                        Expanded(child = tabs),
                    ),
            )
        shell.content = content
        shell.statusBar = status
        status.right =
            "${surface.size.columns}x${surface.size.rows} | ${if (nextCompact) "compact" else "wide"} | F1 theme"
    }

    private fun buildTable(): Widget {
        table =
            TableView(
                items = metrics.toList(),
                columns =
                    listOf(
                        TableColumn("Service", cell = { TableCell(it.name) }),
                        TableColumn("Value", alignment = TextAlignment.RIGHT, cell = { TableCell(it.value) }),
                        TableColumn("State", alignment = TextAlignment.RIGHT, cell = { TableCell(it.state) }),
                    ),
            )
        return table
    }

    private fun buildProgressPanel(): Widget {
        bars.clear()
        repeat(2) { bars += ProgressBar(value = 0.5, showPercent = true) }
        return Panel(
            title = "Throughput",
            child =
                Column(
                    gap = 1,
                    children =
                        listOf(
                            Label("API requests"),
                            bars[0],
                            Label("Worker load"),
                            bars[1],
                        ),
                ),
        )
    }

    private fun buildMenu(): MenuBar {
        val quit =
            Command(
                id = "quit",
                label = "Quit",
                shortcut = KeyChord(Key.Q, Modifiers.CONTROL),
                action = { window.requestClose() },
            )
        val dark = Command(id = "theme.dark", label = "Dark", action = { setTheme(Themes.dark()) })
        val light = Command(id = "theme.light", label = "Light", action = { setTheme(Themes.light()) })
        val contrast =
            Command(id = "theme.contrast", label = "High contrast", action = { setTheme(Themes.highContrast()) })
        return MenuBar(
            Menu("File", 'F', listOf(MenuItem.CommandItem(quit))),
            Menu(
                "View",
                'V',
                listOf(
                    MenuItem.CommandItem(dark),
                    MenuItem.CommandItem(light),
                    MenuItem.CommandItem(contrast),
                ),
            ),
        )
    }

    private fun cycleTheme() {
        setTheme(
            when {
                theme == Themes.dark() -> Themes.light()
                theme == Themes.light() -> Themes.highContrast()
                else -> Themes.dark()
            },
        )
    }

    private fun setTheme(next: Theme) {
        theme = next
        surface.setTheme(next)
    }
}
