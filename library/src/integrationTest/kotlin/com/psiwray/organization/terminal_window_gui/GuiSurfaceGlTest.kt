package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.Colors
import com.psiwray.organization.terminal_window.TerminalWindowConfig
import com.psiwray.organization.terminal_window.TerminalWindowService
import com.psiwray.organization.terminal_window.input.CharEvent
import com.psiwray.organization.terminal_window.input.FocusEvent
import com.psiwray.organization.terminal_window.input.InputListener
import com.psiwray.organization.terminal_window.input.KeyEvent
import com.psiwray.organization.terminal_window.input.MouseButtonEvent
import com.psiwray.organization.terminal_window.input.MouseMoveEvent
import com.psiwray.organization.terminal_window.input.ScrollEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.GuiFonts
import com.psiwray.organization.terminal_window_gui.shape.ShapeSets
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.widget.Widget
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertTrue

@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuiSurfaceGlTest {
    private val mainThread =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "main").apply { isDaemon = true }
        }

    @AfterAll
    fun stopMainThread() {
        mainThread.shutdownNow()
    }

    @Test
    @Timeout(60)
    fun surfacePaintsOneFrameAndCloses() {
        val service = onMain { TerminalWindowService.initialize() }
        try {
            val window = onMain { service.openWindow(config()) }
            try {
                val surface =
                    GuiSurface(
                        BannerWidget(),
                        Themes.dark(),
                        ShapeSets.unicode().resolve(window.font),
                    )
                surface.resize(window.matrix.columns, window.matrix.rows)
                val listener = SurfaceListener(surface)
                window.addInputListener(listener)
                surface.paint(CellCanvas(window.matrix))

                onMain { window.frame() }
                val stats = window.renderStats()
                assertTrue(stats.drawCalls > 0, "expected at least one draw call, got $stats")
                assertTrue(stats.uploads > 0, "expected the painted matrix to upload")
                assertTrue(stats.cellsDrawn > 0, "expected painted cells to reach the renderer")
            } finally {
                onMain { window.close() }
            }
        } finally {
            onMain { service.close() }
        }
    }

    private fun config(): TerminalWindowConfig =
        TerminalWindowConfig(
            font = GuiFonts.bundled(),
            title = "gui surface integration",
            columns = 60,
            rows = 12,
            visible = false,
            vsync = false,
        )

    private fun <T> onMain(block: () -> T): T {
        val future = mainThread.submit(Callable { block() })
        return try {
            future.get(30, TimeUnit.SECONDS)
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        } catch (failure: TimeoutException) {
            future.cancel(true)
            throw AssertionError("main-thread task timed out", failure)
        }
    }

    private class BannerWidget : Widget() {
        override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(0, 0))

        override fun paint(context: PaintContext) {
            context.text(0, 0, "GUI SURFACE", Colors.WHITE, Colors.BLACK)
        }
    }

    private class SurfaceListener(
        private val surface: GuiSurface,
    ) : InputListener {
        override fun onKey(event: KeyEvent) {
            surface.dispatch(GuiInput.key(event))
        }

        override fun onChar(event: CharEvent) {
            surface.dispatch(GuiInput.char(event))
        }

        override fun onMouseMove(event: MouseMoveEvent) {
            surface.dispatch(GuiInput.move(event))
        }

        override fun onMouseButton(event: MouseButtonEvent) {
            surface.dispatch(GuiInput.button(event))
        }

        override fun onScroll(event: ScrollEvent) {
            surface.dispatch(GuiInput.scroll(event, 0, 0))
        }

        override fun onFocus(event: FocusEvent) {
            surface.dispatch(GuiInput.focus(event))
        }
    }
}
