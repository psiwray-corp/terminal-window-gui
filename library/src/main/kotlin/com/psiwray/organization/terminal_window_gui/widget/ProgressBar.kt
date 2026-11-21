package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.TimerHandle
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import com.psiwray.organization.terminal_window_gui.shape.Shape
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * One-row determinate or indeterminate progress bar.
 *
 * Determinate values map to full cells, one of the seven partial eighth-block
 * shapes, and empty cells. The indeterminate phase is advanced by the caller
 * driven scheduler and stops when detached.
 *
 * @property value Progress in `0.0..1.0`.
 * @property showPercent Whether to overlay a percentage label.
 * @property indeterminate Whether to animate a moving fill.
 * @property foreground Optional filled color.
 * @property background Optional background color.
 * @since 1.0.0
 */
public class ProgressBar(
    value: Double = 0.0,
    public var showPercent: Boolean = false,
    indeterminate: Boolean = false,
    public var foreground: Int? = null,
    public var background: Int? = null,
) : Widget() {
    private var valueValue: Double = requireValue(value)
    private var indeterminateValue: Boolean = indeterminate
    private var running: Boolean = false
    private var phase: Int = 0
    private var timer: TimerHandle? = null

    /** Progress in `0.0..1.0`. @since 1.0.0 */
    public var value: Double
        get() = valueValue
        set(value) {
            valueValue = requireValue(value)
            invalidatePaint()
        }

    /** Whether to animate a moving fill. @since 1.0.0 */
    public var indeterminate: Boolean
        get() = indeterminateValue
        set(value) {
            if (indeterminateValue == value) {
                return
            }
            indeterminateValue = value
            if (value) {
                start()
            } else {
                stop()
            }
            invalidatePaint()
        }

    /** Current animation frame. @since 1.0.0 */
    public var frame: Int
        get() = phase
        set(value) {
            phase = value.coerceAtLeast(0)
            invalidatePaint()
        }

    override fun onAttach() {
        if (indeterminateValue) {
            start()
        }
    }

    override fun onDetach() {
        stop()
    }

    /** Starts the indeterminate timer when attached. @since 1.0.0 */
    public fun start() {
        if (running) {
            return
        }
        running = true
        val context = guiContext ?: return
        schedule(context.scheduler)
    }

    /** Stops the indeterminate timer. @since 1.0.0 */
    public fun stop() {
        running = false
        timer?.let { guiContext?.scheduler?.cancel(it) }
        timer = null
    }

    override fun measure(constraints: Constraints): IntSize =
        constraints.constrain(
            IntSize(
                if (constraints.hasBoundedWidth) constraints.maxWidth else MIN_WIDTH,
                1,
            ),
        )

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val emptyFg = theme.textMuted
        val filledFg = foreground ?: theme.accent
        val bg = background ?: theme.surface
        context.fill(Rect(0, 0, bounds.width, bounds.height), emptyFg, bg)
        if (indeterminateValue) {
            val fillWidth = (bounds.width / 4).coerceAtLeast(1)
            val start = if (bounds.width == 0) 0 else (phase % bounds.width + bounds.width) % bounds.width
            var index = 0
            while (index < fillWidth) {
                val x = (start + index) % bounds.width
                context.shape(Shape.PROGRESS_FULL, x, 0, filledFg, bg)
                index++
            }
        } else {
            val fraction = valueValue.coerceIn(0.0, 1.0) * bounds.width
            val full = floor(fraction).toInt().coerceIn(0, bounds.width)
            var x = 0
            while (x < full) {
                context.shape(Shape.PROGRESS_FULL, x, 0, filledFg, bg)
                x++
            }
            if (full < bounds.width) {
                val remainder = fraction - full
                if (remainder > 0.0) {
                    context.shape(partialShape(remainder), full, 0, filledFg, bg)
                    x = full + 1
                }
                while (x < bounds.width) {
                    context.shape(Shape.PROGRESS_EMPTY, x, 0, emptyFg, bg)
                    x++
                }
            }
        }
        if (showPercent && !indeterminateValue) {
            val label = "${(valueValue * 100).roundToInt()}%"
            TextPainter.draw(
                context = context,
                line = label,
                x = 0,
                y = 0,
                width = bounds.width,
                alignment = TextAlignment.CENTER,
                fg = theme.text,
                bg = bg,
                ellipsis = false,
            )
        }
    }

    private fun schedule(scheduler: com.psiwray.organization.terminal_window_gui.input.Scheduler) {
        timer =
            scheduler.postDelayed(FRAME_MILLIS) {
                timer = null
                if (!running) {
                    return@postDelayed
                }
                phase++
                invalidatePaint()
                val context = guiContext
                if (context != null) {
                    schedule(context.scheduler)
                }
            }
    }

    private fun partialShape(remainder: Double): Shape {
        val eighth = ceil(remainder * 8.0).toInt().coerceIn(1, 7)
        return when (eighth) {
            1 -> Shape.PROGRESS_1_8
            2 -> Shape.PROGRESS_1_4
            3 -> Shape.PROGRESS_3_8
            4 -> Shape.PROGRESS_1_2
            5 -> Shape.PROGRESS_5_8
            6 -> Shape.PROGRESS_3_4
            else -> Shape.PROGRESS_7_8
        }
    }

    private fun requireValue(value: Double): Double {
        require(value in 0.0..1.0) { "value must be in 0.0..1.0, got $value" }
        return value
    }

    private companion object {
        private const val MIN_WIDTH: Int = 3
        private const val FRAME_MILLIS: Long = 100
    }
}
