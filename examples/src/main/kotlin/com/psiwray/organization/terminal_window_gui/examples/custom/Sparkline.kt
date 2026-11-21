package com.psiwray.organization.terminal_window_gui.examples.custom

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.widget.Widget
import kotlin.math.ceil

/**
 * One-row sparkline built only from public extension points.
 *
 * Values are normalized against [maximum] and quantized to eighth-block
 * progress shapes. The widget owns no glyph code point; it asks the surface's
 * [PaintContext] for semantic [Shape] roles.
 */
internal class Sparkline(
    values: List<Double> = emptyList(),
    var maximum: Double = 1.0,
    var foreground: Int? = null,
    var background: Int? = null,
) : Widget() {
    private var valuesValue: List<Double> = values.map(::requireFinite)

    /** Values in display order. */
    var values: List<Double>
        get() = valuesValue
        set(value) {
            valuesValue = value.map(::requireFinite)
            invalidateLayout()
            invalidatePaint()
        }

    init {
        require(maximum > 0.0 && maximum.isFinite()) { "maximum must be finite and positive: $maximum" }
    }

    override fun measure(constraints: Constraints): IntSize =
        constraints.constrain(IntSize(valuesValue.size.coerceAtLeast(1), 1))

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val bg = background ?: theme.surface
        val fg = foreground ?: theme.accent
        context.fill(Rect(0, 0, bounds.width, 1), fg, bg)
        var x = 0
        while (x < bounds.width) {
            val value = valuesValue.getOrNull(x)
            if (value != null) {
                val fraction = (value / maximum).coerceIn(0.0, 1.0)
                context.shape(shapeFor(fraction), x, 0, fg, bg)
            }
            x++
        }
    }

    private fun shapeFor(fraction: Double): Shape {
        val eighth = ceil(fraction * 8.0).toInt().coerceIn(0, 8)
        return when (eighth) {
            0 -> Shape.PROGRESS_EMPTY
            1 -> Shape.PROGRESS_1_8
            2 -> Shape.PROGRESS_1_4
            3 -> Shape.PROGRESS_3_8
            4 -> Shape.PROGRESS_1_2
            5 -> Shape.PROGRESS_5_8
            6 -> Shape.PROGRESS_3_4
            7 -> Shape.PROGRESS_7_8
            else -> Shape.PROGRESS_FULL
        }
    }

    private companion object {
        private fun requireFinite(value: Double): Double {
            require(value.isFinite()) { "sparkline values must be finite: $value" }
            return value
        }
    }
}
