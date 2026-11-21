package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.Scheduler
import com.psiwray.organization.terminal_window_gui.input.TimerHandle
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape

/**
 * One-cell animated spinner driven by the caller scheduled timer queue.
 *
 * @property frame Current frame index, modulo four.
 * @property autoStart Whether attaching starts frame advancement automatically.
 * @since 1.0.0
 */
public class Spinner(
    frame: Int = 0,
    public val autoStart: Boolean = true,
) : Widget() {
    private var frameValue: Int = frame.coerceAtLeast(0)
    private var running: Boolean = autoStart
    private var timer: TimerHandle? = null

    /** Current frame index. @since 1.0.0 */
    public var frame: Int
        get() = frameValue
        set(value) {
            frameValue = value.coerceAtLeast(0)
            invalidatePaint()
        }

    /** Whether the spinner is running. @since 1.0.0 */
    public val isRunning: Boolean get() = running

    override fun onAttach() {
        if (!running && autoStart) {
            start()
        } else if (running) {
            scheduleIfAttached()
        }
    }

    override fun onDetach() {
        stop()
    }

    /** Starts advancing frames. @since 1.0.0 */
    public fun start() {
        if (running) {
            return
        }
        running = true
        scheduleIfAttached()
    }

    /** Stops advancing frames. @since 1.0.0 */
    public fun stop() {
        running = false
        timer?.let { guiContext?.scheduler?.cancel(it) }
        timer = null
    }

    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(1, 1))

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        context.shape(frames[frameValue % frames.size], 0, 0, context.theme.accent, context.theme.surface)
    }

    private fun scheduleIfAttached() {
        val scheduler = guiContext?.scheduler ?: return
        schedule(scheduler)
    }

    private fun schedule(scheduler: Scheduler) {
        timer =
            scheduler.postDelayed(FRAME_MILLIS) {
                timer = null
                if (!running) {
                    return@postDelayed
                }
                frameValue++
                invalidatePaint()
                scheduleIfAttached()
            }
    }

    private companion object {
        private const val FRAME_MILLIS: Long = 100
        private val frames: Array<Shape> = arrayOf(Shape.SPINNER_0, Shape.SPINNER_1, Shape.SPINNER_2, Shape.SPINNER_3)
    }
}
