package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.internal.BoxPainter
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import kotlin.math.max

/**
 * Bordered container with an optional top-edge title and padding.
 *
 * The border consumes one cell per side. The title is painted with one cell of
 * padding on each side and truncates with an ellipsis. Focus uses the focused
 * border color and an inverted title; when those colors are indistinguishable,
 * a `>` marker is added on the top edge.
 *
 * @property title Optional title.
 * @property style Border style.
 * @property padding Inner padding between border and child.
 * @property background Content background, or `null` for the theme surface.
 * @since 1.0.0
 */
public class Panel(
    title: String? = null,
    public val style: BoxStyle = BoxStyle.SINGLE,
    public val padding: Insets = Insets(0, 0, 0, 0),
    public var background: Int? = null,
    child: Widget? = null,
) : SingleChildWidget(child) {
    private var titleValue: String? = title

    /** Optional title. @since 1.0.0 */
    public var title: String?
        get() = titleValue
        set(value) {
            if (titleValue == value) {
                return
            }
            titleValue = value
            invalidateLayout()
        }

    init {
        focusable = true
    }

    override fun computeContentRect(bounds: Rect): Rect =
        bounds.inset(
            1 + padding.left,
            1 + padding.top,
            1 + padding.right,
            1 + padding.bottom,
        )

    override fun measure(constraints: Constraints): IntSize {
        val innerMaxWidth =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth - 2 - padding.horizontal).coerceAtLeast(0)
            } else {
                Int.MAX_VALUE
            }
        val innerMaxHeight =
            if (constraints.hasBoundedHeight) {
                (constraints.maxHeight - 2 - padding.vertical).coerceAtLeast(0)
            } else {
                Int.MAX_VALUE
            }
        val childSize = child?.measured(Constraints(0, innerMaxWidth, 0, innerMaxHeight)) ?: IntSize.ZERO
        val titleWidth = titleValue?.let(TextPainter::codePointCount) ?: 0
        val width =
            max(
                2,
                childSize.width + 2 + padding.horizontal,
            ).let { if (titleWidth > 0) max(it, titleWidth + 4) else it }
        val height = max(2, childSize.height + 2 + padding.vertical)
        return constraints.constrain(IntSize(width, height))
    }

    override fun arrangeChildren() {
        val current = child ?: return
        val content = contentRect
        val size = current.measured(Constraints.loose(IntSize(content.width, content.height)))
        current.arrange(Rect(0, 0, content.width.coerceAtMost(size.width), content.height.coerceAtMost(size.height)))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        val focused = isFocused
        val lineColor = if (focused) theme.borderFocused else theme.border
        val titleStyle =
            if (focused) {
                theme.style(com.psiwray.organization.terminal_window_gui.style.WidgetState.FOCUSED)
            } else {
                theme.style(
                    com.psiwray.organization.terminal_window_gui.style.WidgetState.NORMAL,
                )
            }
        BoxPainter.paint(context, style, bounds.width, bounds.height, lineColor, theme.surface)
        val content = contentRect
        if (!content.isEmpty) {
            context.fill(
                Rect(content.x - bounds.x, content.y - bounds.y, content.width, content.height),
                theme.text,
                background ?: theme.surface,
            )
        }
        val text = titleValue
        if (!text.isNullOrEmpty() && bounds.width > 4) {
            val available = bounds.width - 4
            val width = TextPainter.codePointCount(text)
            val truncated = width > available
            context.text(1, 0, " ", titleStyle.fg, titleStyle.bg)
            TextPainter.draw(
                context = context,
                line = text,
                x = 2,
                y = 0,
                width = available,
                alignment = TextAlignment.LEFT,
                fg = titleStyle.fg,
                bg = titleStyle.bg,
                ellipsis = truncated,
            )
            if (!truncated) {
                val trailing = 2 + width
                if (trailing < bounds.width - 1) {
                    context.text(trailing, 0, " ", titleStyle.fg, titleStyle.bg)
                }
            }
        } else if (focused && bounds.width > 1 && theme.borderFocused == theme.border) {
            context.text(1, 0, ">", titleStyle.fg, titleStyle.bg)
        }
    }
}
