package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment

/**
 * One muted row with left, centered, and right text sections.
 *
 * Sections never overlap. The right section is reserved first, the left
 * section gets the remaining leading cells, and the center section is centered
 * in the space between them. Every section is truncated with an ellipsis when
 * it does not fit. [hints] are formatted with [KeyHint] and joined by
 * [hintSeparator] in the left section when no left text is supplied.
 *
 * The bar is display-only; it never takes focus or input.
 *
 * @property left Leading text.
 * @property center Center text.
 * @property right Trailing text.
 * @property hints Key hints shown in the leading section when [left] is empty.
 * @property hintSeparator Separator between [hints].
 * @since 1.0.0
 */
public class StatusBar(
    left: String = "",
    center: String = "",
    right: String = "",
    hints: List<KeyHint> = emptyList(),
    public var hintSeparator: String = " | ",
) : Widget() {
    private var leftValue: String = left
    private var centerValue: String = center
    private var rightValue: String = right
    private var hintsValue: List<KeyHint> = hints.toList()

    /** Leading text. @since 1.0.0 */
    public var left: String
        get() = leftValue
        set(value) {
            if (leftValue == value) {
                return
            }
            leftValue = value
            invalidateLayout()
            invalidatePaint()
        }

    /** Center text. @since 1.0.0 */
    public var center: String
        get() = centerValue
        set(value) {
            if (centerValue == value) {
                return
            }
            centerValue = value
            invalidateLayout()
            invalidatePaint()
        }

    /** Trailing text. @since 1.0.0 */
    public var right: String
        get() = rightValue
        set(value) {
            if (rightValue == value) {
                return
            }
            rightValue = value
            invalidateLayout()
            invalidatePaint()
        }

    /** Key hints. @since 1.0.0 */
    public var hints: List<KeyHint>
        get() = hintsValue
        set(value) {
            hintsValue = value.toList()
            invalidateLayout()
            invalidatePaint()
        }

    /** Alias for [hints]. @since 1.0.0 */
    public val keyHints: List<KeyHint> get() = hintsValue

    /** Creates a hint-only bar. @since 1.0.0 */
    public constructor(
        hints: List<KeyHint>,
        separator: String = " | ",
    ) : this("", "", "", hints, separator)

    /** Creates a bar from three hint values. @since 1.0.0 */
    public constructor(
        left: KeyHint,
        center: KeyHint? = null,
        right: KeyHint? = null,
    ) : this(left.text, center?.text ?: "", right?.text ?: "")

    /** Creates a bar from hint lists. @since 1.0.0 */
    public constructor(
        left: List<KeyHint>,
        center: List<KeyHint>,
        right: List<KeyHint>,
    ) : this(
        left.joinToString(" | ") { it.text },
        center.joinToString(" | ") { it.text },
        right.joinToString(" | ") { it.text },
    )

    override fun measure(constraints: Constraints): IntSize {
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                preferredWidth()
            }
        return constraints.constrain(IntSize(width.coerceAtLeast(1), 1))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, 1), theme.textMuted, theme.surface)
        val width = bounds.width
        val (leftText, centerText, rightText) = sections()
        val rightWidth = TextPainter.codePointCount(rightText).coerceAtMost(width)
        val rightX = width - rightWidth
        val leftCap = rightX.coerceAtLeast(0)
        val leftWidth = TextPainter.codePointCount(leftText).coerceAtMost(leftCap)
        drawSection(context, leftText, 0, leftWidth)
        val centerStart = leftWidth
        val centerAvailable = (rightX - centerStart).coerceAtLeast(0)
        val centerWidth = TextPainter.codePointCount(centerText).coerceAtMost(centerAvailable)
        val centerX = centerStart + (centerAvailable - centerWidth) / 2
        drawSection(context, centerText, centerX, centerWidth)
        drawSection(context, rightText, rightX, rightWidth)
    }

    private fun drawSection(
        context: PaintContext,
        text: String,
        x: Int,
        width: Int,
    ) {
        if (width <= 0 || text.isEmpty()) {
            return
        }
        if (x <= -width || x >= bounds.width) {
            return
        }
        context.pushClip(Rect(x, 0, width, 1))
        try {
            TextPainter.draw(
                context = context,
                line = text.replace('\n', ' '),
                x = x,
                y = 0,
                width = width,
                alignment = TextAlignment.LEFT,
                fg = context.theme.textMuted,
                bg = context.theme.surface,
                ellipsis = TextPainter.codePointCount(text) > width,
            )
        } finally {
            context.popClip()
        }
    }

    private fun sections(): Triple<String, String, String> {
        val hintText = hintsValue.joinToString(hintSeparator) { it.text }
        val leading =
            when {
                hintText.isEmpty() -> leftValue
                leftValue.isEmpty() -> hintText
                else -> leftValue + hintSeparator + hintText
            }
        return Triple(leading, centerValue, rightValue)
    }

    private fun preferredWidth(): Int {
        val (leftText, centerText, rightText) = sections()
        return maxOf(
            1,
            TextPainter.codePointCount(leftText) +
                TextPainter.codePointCount(centerText) +
                TextPainter.codePointCount(rightText),
        )
    }
}
