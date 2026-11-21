package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment

/**
 * Muted `keys label` hint such as `Ctrl+S Save`.
 *
 * Keys are supplied as a display string or formatted from a shared
 * [KeyChord]. The widget is one row high and never takes focus or input.
 *
 * @property keys Display form of the key combination.
 * @property label Command description.
 * @since 1.0.0
 */
public class KeyHint(
    keys: String,
    label: String,
) : Widget() {
    private var keysValue: String = keys
    private var labelValue: String = label

    /** Display form of the key combination. @since 1.0.0 */
    public var keys: String
        get() = keysValue
        set(value) {
            if (keysValue == value) {
                return
            }
            keysValue = value
            invalidateLayout()
        }

    /** Command description. @since 1.0.0 */
    public var label: String
        get() = labelValue
        set(value) {
            if (labelValue == value) {
                return
            }
            labelValue = value
            invalidateLayout()
        }

    /** Full `keys label` text. @since 1.0.0 */
    public val text: String
        get() = if (labelValue.isEmpty()) keysValue else "$keysValue $labelValue"

    /** Formats [chord] for display. @since 1.0.0 */
    public constructor(
        chord: KeyChord,
        label: String,
    ) : this(chord.label(), label)

    override fun measure(constraints: Constraints): IntSize {
        val width = TextPainter.codePointCount(text).coerceAtLeast(1)
        return constraints.constrain(IntSize(width, 1))
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        context.fill(Rect(0, 0, bounds.width, 1), theme.textMuted, theme.surface)
        val value = text
        TextPainter.draw(
            context = context,
            line = value,
            x = 0,
            y = 0,
            width = bounds.width,
            alignment = TextAlignment.LEFT,
            fg = theme.textMuted,
            bg = theme.surface,
            ellipsis = TextPainter.codePointCount(value) > bounds.width,
        )
    }
}
