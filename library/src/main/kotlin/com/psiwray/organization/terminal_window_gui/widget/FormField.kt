package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import kotlin.math.floor

/**
 * Label width choices shared by forms and fields.
 *
 * A fixed cell count, a fraction of the available width, or the label's
 * intrinsic content width. Content width is always capped so the control keeps
 * at least one cell.
 *
 * @since 1.0.0
 */
public sealed class FormLabelWidth {
    /**
     * Fixed label cells.
     *
     * @property cells Label cells.
     * @since 1.0.0
     */
    public data class Fixed(
        public val cells: Int,
    ) : FormLabelWidth() {
        init {
            require(cells >= 0) { "cells must be non-negative, got $cells" }
        }
    }

    /**
     * Fraction of the available width.
     *
     * @property fraction Fraction in `(0, 1]`.
     * @since 1.0.0
     */
    public data class Fraction(
        public val fraction: Double,
    ) : FormLabelWidth() {
        init {
            require(fraction > 0.0 && fraction <= 1.0) {
                "fraction must be in (0, 1], got $fraction"
            }
        }
    }

    /** Intrinsic label width, capped to preserve the control. @since 1.0.0 */
    public data object Content : FormLabelWidth()
}

/**
 * One form row: label, control, and an optional single-line help/error message.
 *
 * The label is painted in its own column; the control keeps the remaining
 * width. The message row is below the control and is truncated with an
 * ellipsis. An error uses the error theme color and a leading `!` marker.
 * Required is application state; the widget only displays it and never
 * validates.
 *
 * @property label Label text.
 * @property control Interactive control.
 * @property helpText Muted help line.
 * @property errorText Error line; takes precedence over [helpText].
 * @property required Application-owned required flag.
 * @property labelWidth Fixed, fraction, or content label width.
 * @since 1.0.0
 */
public class FormField(
    label: String = "",
    control: Widget,
    helpText: String = "",
    errorText: String = "",
    required: Boolean = false,
    labelWidth: Any = FormLabelWidth.Content,
) : SingleChildWidget(control) {
    private var labelValue: String = label
    private var helpTextValue: String = helpText
    private var errorTextValue: String = errorText
    private var requiredValue: Boolean = required
    private var labelWidthValue: Any = labelWidth

    /**
     * Options for the nested label-width API.
     *
     * @since 1.0.0
     */
    public sealed class LabelWidth {
        /**
         * Fixed label cells.
         *
         * @property cells Label cells.
         * @since 1.0.0
         */
        public data class Fixed(
            public val cells: Int,
        ) : LabelWidth()

        /**
         * Fraction of the available width.
         *
         * @property fraction Fraction in `(0, 1]`.
         * @since 1.0.0
         */
        public data class Fraction(
            public val fraction: Double,
        ) : LabelWidth()

        /** Intrinsic content width. @since 1.0.0 */
        public data object Content : LabelWidth()
    }

    /** Label text. @since 1.0.0 */
    public var label: String
        get() = labelValue
        set(value) {
            if (labelValue == value) {
                return
            }
            labelValue = value
            invalidateLayout()
        }

    /** Interactive control. @since 1.0.0 */
    public var control: Widget
        get() = child ?: error("form field has no control")
        set(value) {
            if (child !== value) {
                setChild(value)
                invalidateLayout()
            }
        }

    /** Muted help text; ignored while [errorText] is non-empty. @since 1.0.0 */
    public var helpText: String
        get() = helpTextValue
        set(value) {
            if (helpTextValue == value) {
                return
            }
            helpTextValue = value
            invalidateLayout()
        }

    /** Error text. @since 1.0.0 */
    public var errorText: String
        get() = errorTextValue
        set(value) {
            if (errorTextValue == value) {
                return
            }
            errorTextValue = value
            invalidateLayout()
        }

    /** Application-owned required flag. @since 1.0.0 */
    public var required: Boolean
        get() = requiredValue
        set(value) {
            requiredValue = value
        }

    /**
     * Label width request. Accepts [FormLabelWidth], [FormField.LabelWidth],
     * [ColumnWidth.Fixed], `.Fraction`, `.Content`, or an integer cell count.
     *
     * @since 1.0.0
     */
    public var labelWidth: Any
        get() = labelWidthValue
        set(value) {
            labelWidthValue = value
            invalidateLayout()
        }

    override fun measure(constraints: Constraints): IntSize {
        val current = child ?: return constraints.constrain(IntSize(1, 1))
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val labelCells = labelWidthFor(maxWidth)
        val gap = if (labelCells > 0) 1 else 0
        val controlWidth = (maxWidth - labelCells - gap).coerceAtLeast(1)
        val maxHeight =
            if (constraints.hasBoundedHeight) {
                (constraints.maxHeight - messageHeight()).coerceAtLeast(0)
            } else {
                Int.MAX_VALUE
            }
        val controlSize =
            current.measured(
                Constraints(0, controlWidth, 0, maxHeight),
            )
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                (labelCells + gap + controlSize.width).coerceAtLeast(1)
            }
        val height = controlSize.height.coerceAtLeast(1) + messageHeight()
        return constraints.constrain(IntSize(width.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val width = contentRect.width
        val height = contentRect.height
        if (width <= 0 || height <= 0) {
            current.arrange(Rect.EMPTY)
            return
        }
        val messageRows = messageHeight().coerceAtMost(height)
        val controlHeight = (height - messageRows).coerceAtLeast(1)
        val labelCells = labelWidthFor(width)
        val minControl = 1
        val gap = if (labelCells > 0 && width > labelCells + minControl) 1 else 0
        val controlWidth = (width - labelCells - gap).coerceAtLeast(minControl)
        val controlX = (labelCells + gap).coerceAtMost((width - minControl).coerceAtLeast(0))
        current.arrange(Rect(controlX, 0, controlWidth, controlHeight))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val width = bounds.width
        val messageRows = messageHeight()
        val controlHeight = (bounds.height - messageRows).coerceAtLeast(1)
        val labelCells = labelWidthFor(width)
        val gap = if (labelCells > 0 && width > labelCells + 1) 1 else 0
        val controlX = (labelCells + gap).coerceAtMost((width - 1).coerceAtLeast(0))
        if (labelCells > 0 && labelValue.isNotEmpty()) {
            val label = labelValue.replace('\n', ' ')
            TextPainter.draw(
                context = context,
                line = label,
                x = 0,
                y = 0,
                width = labelCells,
                alignment = TextAlignment.LEFT,
                fg = theme.text,
                bg = theme.surface,
                ellipsis = TextPainter.codePointCount(label) > labelCells,
            )
        }
        if (messageRows == 0) {
            return
        }
        val error = errorTextValue.isNotEmpty()
        if (error) {
            context.text(controlX, controlHeight, "!", theme.error, theme.surface)
            val available = (width - controlX - 2).coerceAtLeast(0)
            if (available > 0) {
                val text = errorTextValue.replace('\n', ' ')
                TextPainter.draw(
                    context = context,
                    line = text,
                    x = controlX + 2,
                    y = controlHeight,
                    width = available,
                    alignment = TextAlignment.LEFT,
                    fg = theme.error,
                    bg = theme.surface,
                    ellipsis = TextPainter.codePointCount(text) > available,
                )
            }
        } else {
            val available = (width - controlX).coerceAtLeast(0)
            if (available > 0) {
                val text = helpTextValue.replace('\n', ' ')
                TextPainter.draw(
                    context = context,
                    line = text,
                    x = controlX,
                    y = controlHeight,
                    width = available,
                    alignment = TextAlignment.LEFT,
                    fg = theme.textMuted,
                    bg = theme.surface,
                    ellipsis = TextPainter.codePointCount(text) > available,
                )
            }
        }
    }

    private fun messageHeight(): Int = if (errorTextValue.isNotEmpty() || helpTextValue.isNotEmpty()) 1 else 0

    private fun labelWidthFor(available: Int): Int {
        if (available <= 0) {
            return 0
        }
        val requested =
            when (val request = labelWidthValue) {
                is Int -> request
                is FormLabelWidth.Fixed -> request.cells
                is FormLabelWidth.Fraction -> floor(available * request.fraction).toInt()
                FormLabelWidth.Content -> TextPainter.codePointCount(labelValue)
                is FormField.LabelWidth.Fixed -> request.cells
                is FormField.LabelWidth.Fraction -> floor(available * request.fraction).toInt()
                FormField.LabelWidth.Content -> TextPainter.codePointCount(labelValue)
                is ColumnWidth.Fixed -> request.cells
                is ColumnWidth.Fraction -> floor(available * request.fraction).toInt()
                ColumnWidth.Content -> TextPainter.codePointCount(labelValue)
                is ColumnWidth.Flex -> 1
                else -> TextPainter.codePointCount(labelValue)
            }
        return requested.coerceIn(0, (available - 1).coerceAtLeast(0))
    }
}

/** Alias for [FormField] used by the v1 component catalog. @since 1.0.0 */
public typealias Field = FormField
