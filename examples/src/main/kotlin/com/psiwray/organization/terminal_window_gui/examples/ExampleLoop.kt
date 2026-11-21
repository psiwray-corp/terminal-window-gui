package com.psiwray.organization.terminal_window_gui.examples

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window.TerminalWindow
import com.psiwray.organization.terminal_window.TerminalWindowConfig
import com.psiwray.organization.terminal_window.TerminalWindowService
import com.psiwray.organization.terminal_window.font.BitmapFont
import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.FocusEvent
import com.psiwray.organization.terminal_window.input.InputListener
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window.input.MouseMoveEvent
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.GuiInput
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget
import org.lwjgl.glfw.GLFW
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * An example screen: one root widget plus optional per-surface hooks.
 *
 * This abstraction is example-only. The library never owns a window, thread,
 * timer, or frame loop.
 */
internal abstract class ExampleApp(
    val title: String,
    val columns: Int,
    val rows: Int,
) {
    abstract fun root(): Widget

    open val initialTheme: Theme get() = Themes.dark()

    open val initialShapes: com.psiwray.organization.terminal_window_gui.shape.ShapeSet
        get() = ShapeSets.unicode()

    open fun configure(
        surface: GuiSurface,
        window: TerminalWindow,
    ) {}

    open fun onFrame(
        surface: GuiSurface,
        nowNanos: Long,
    ) {}
}

/** Options understood by every example. */
private data class ExampleOptions(
    val smoke: Boolean,
    val screenshot: Path?,
)

/**
 * Caller-owned example host loop.
 *
 * The process main thread owns the terminal-window service and its event loop.
 * A dedicated owner thread owns the GL context, the [GuiSurface], input
 * translation, paint scheduling, cursor/title application, and clean shutdown.
 */
internal object ExampleLoop {
    private const val SMOKE_FRAMES = 10
    private const val SMOKE_TIMEOUT_NANOS = 20_000_000_000L
    private const val FRAME_SLEEP_MILLIS = 15L

    fun run(
        app: ExampleApp,
        args: Array<String>,
    ) {
        val options = parseOptions(args)
        val font = GuiFonts.bundled()
        val service = initializeService()
        service.use { terminal ->
            val window =
                terminal.openWindow(
                    TerminalWindowConfig(
                        font = font,
                        title = app.title,
                        columns = app.columns,
                        rows = app.rows,
                        defaultForeground = app.initialTheme.text,
                        defaultBackground = app.initialTheme.background,
                    ),
                )
            window.releaseContext()
            val failure = AtomicReference<Throwable?>()
            val owner =
                thread(name = "terminal-window-gui-example", isDaemon = false, start = false) {
                    runOwner(window, app, font, options, failure)
                }
            owner.start()
            try {
                terminal.runEventLoop()
            } catch (cause: Throwable) {
                window.requestClose()
                throw cause
            } finally {
                owner.join()
            }
            failure.get()?.let { throw it }
        }
    }

    private fun runOwner(
        window: TerminalWindow,
        app: ExampleApp,
        font: BitmapFont,
        options: ExampleOptions,
        failure: AtomicReference<Throwable?>,
    ) {
        try {
            window.use { owned ->
                owned.acquireContext()
                try {
                    val surface =
                        GuiSurface(
                            root = app.root(),
                            theme = app.initialTheme,
                            shapes = app.initialShapes.resolve(font),
                        )
                    val initialSize = owned.matrix
                    surface.resize(initialSize.columns, initialSize.rows)
                    app.configure(surface, owned)
                    surface.keymap.bind(
                        KeyChord(Key.Q, Modifiers.CONTROL),
                        Command("quit", action = { owned.requestClose() }),
                    )
                    owned.addInputListener(InputHandler(surface))
                    owned.forceRepaint = true
                    frames(owned, surface, app, options)
                    owned.requestClose()
                } finally {
                    owned.releaseContext()
                }
            }
        } catch (cause: Throwable) {
            failure.compareAndSet(null, cause)
            window.requestClose()
        }
    }

    private fun frames(
        window: TerminalWindow,
        surface: GuiSurface,
        app: ExampleApp,
        options: ExampleOptions,
    ) {
        var frameIndex = 0
        var drawCalls = 0
        var captured = false
        var lastCursor: CursorShape? = null
        var lastTitle: String? = null
        val smokeDeadline = if (options.smoke) System.nanoTime() + SMOKE_TIMEOUT_NANOS else Long.MAX_VALUE
        while (!window.shouldClose) {
            window.frame()
            if (window.shouldClose) {
                break
            }
            drawCalls += window.renderStats().drawCalls
            val matrix = window.matrix
            if (surface.size.columns != matrix.columns || surface.size.rows != matrix.rows) {
                surface.resize(matrix.columns, matrix.rows)
            }
            val now = System.nanoTime()
            check(now <= smokeDeadline) { "smoke exceeded 20 s" }
            surface.tick(now)
            if (surface.needsPaint) {
                surface.paint(CellCanvas(matrix))
                window.requestRepaint()
            }
            val requested = surface.pointerShape
            if (requested != lastCursor) {
                window.setCursor(requested ?: CursorShape.ARROW)
                lastCursor = requested
            }
            val title = "${app.title} | ${surface.size.columns}x${surface.size.rows}"
            if (title != lastTitle) {
                window.setTitle(title)
                lastTitle = title
            }
            if (options.screenshot != null && !captured && frameIndex >= 3) {
                captureFrontBuffer(options.screenshot)
                captured = true
            }
            app.onFrame(surface, now)
            frameIndex++
            if (options.smoke && frameIndex >= SMOKE_FRAMES) {
                check(drawCalls > 0) { "smoke observed no draw call" }
                break
            }
            try {
                Thread.sleep(FRAME_SLEEP_MILLIS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    private fun initializeService(): TerminalWindowService {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("linux") && !System.getenv("DISPLAY").isNullOrBlank()) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11)
        }
        return TerminalWindowService.initialize()
    }

    private fun parseOptions(args: Array<String>): ExampleOptions {
        var smoke = false
        var screenshot: Path? = null
        for (arg in args) {
            when {
                arg == "--smoke" -> {
                    smoke = true
                }

                arg.startsWith("--screenshot=") -> {
                    val value = arg.substringAfter('=')
                    require(value.isNotBlank()) { "--screenshot requires a path" }
                    screenshot = Path.of(value)
                }
            }
        }
        return ExampleOptions(smoke, screenshot)
    }

    private class InputHandler(
        private val surface: GuiSurface,
    ) : InputListener {
        private var pointerColumn = 0
        private var pointerRow = 0

        override fun onKey(event: KeyEvent) {
            surface.dispatch(GuiInput.key(event))
        }

        override fun onChar(event: CharEvent) {
            surface.dispatch(GuiInput.char(event))
        }

        override fun onMouseMove(event: MouseMoveEvent) {
            pointerColumn = event.column
            pointerRow = event.row
            surface.dispatch(GuiInput.move(event))
        }

        override fun onMouseButton(event: MouseButtonEvent) {
            pointerColumn = event.column
            pointerRow = event.row
            surface.dispatch(GuiInput.button(event))
        }

        override fun onScroll(event: ScrollEvent) {
            surface.dispatch(GuiInput.scroll(event, pointerColumn, pointerRow))
        }

        override fun onFocus(event: FocusEvent) {
            surface.dispatch(GuiInput.focus(event))
        }
    }
}
