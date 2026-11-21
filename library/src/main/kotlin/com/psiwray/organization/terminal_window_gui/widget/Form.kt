package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import java.util.Collections

/**
 * Vertical form of [FormField] rows with a shared label-width policy.
 *
 * Fields are laid out top to bottom with [gap] blank rows between them. Each
 * field measures its control and optional message row; the form itself does
 * not validate or submit.
 *
 * @property labelWidth Label width applied to every field.
 * @property gap Blank rows between fields.
 * @since 1.0.0
 */
public class Form(
    children: List<FormField> = emptyList(),
    labelWidth: Any = FormLabelWidth.Content,
    gap: Int = 1,
) : ContainerWidget() {
    private var labelWidthValue: Any = labelWidth
    private var gapValue: Int = gap

    init {
        require(gap >= 0) { "gap must be non-negative, got $gap" }
        for (field in children) {
            field.labelWidth = labelWidthValue
            adopt(field)
        }
    }

    /** Blank rows between fields. @since 1.0.0 */
    public var gap: Int
        get() = gapValue
        set(value) {
            require(value >= 0) { "gap must be non-negative, got $value" }
            if (gapValue == value) {
                return
            }
            gapValue = value
            invalidateLayout()
        }

    /** Appends a field with the form label width. @since 1.0.0 */
    public fun add(field: FormField): Form {
        field.labelWidth = labelWidthValue
        adopt(field)
        return this
    }

    /** Removes [field] and detaches it. @since 1.0.0 */
    public fun remove(field: FormField): Boolean = drop(field)

    /** Fields in display order. @since 1.0.0 */
    public val children: List<FormField> get() = Collections.unmodifiableList(fieldList())

    /** Alias for [children]. @since 1.0.0 */
    public val fields: List<FormField> get() = children

    /** Shared label width. Assigning updates every field. @since 1.0.0 */
    public var labelWidth: Any
        get() = labelWidthValue
        set(value) {
            if (labelWidthValue == value) {
                return
            }
            labelWidthValue = value
            for (field in fieldList()) {
                field.labelWidth = value
            }
            invalidateLayout()
        }

    /** Alias for [gap]. @since 1.0.0 */
    public var spacing: Int
        get() = gap
        set(value) {
            require(value >= 0) { "spacing must be non-negative, got $value" }
            if (gap == value) {
                return
            }
            gap = value
            invalidateLayout()
        }

    override fun measure(constraints: Constraints): IntSize {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else preferredWidth(constraints)
        val fieldConstraints = Constraints(0, width, 0, Int.MAX_VALUE)
        var height = 0
        val fields = fieldList()
        var index = 0
        while (index < fields.size) {
            if (index > 0) {
                height += gap
            }
            height += fields[index].measured(fieldConstraints).height
            index++
        }
        val measuredHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else height
        return constraints.constrain(IntSize(width.coerceAtLeast(1), measuredHeight.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val width = contentRect.width
        val height = contentRect.height
        var y = 0
        for (field in fieldList()) {
            if (y >= height) {
                field.arrange(Rect.EMPTY)
                continue
            }
            val preferred = field.measured(Constraints(0, width, 0, Int.MAX_VALUE)).height
            val fieldHeight = preferred.coerceAtMost(height - y)
            field.arrange(Rect(0, y, width, fieldHeight))
            y += fieldHeight + gap
        }
    }

    override fun paint(context: PaintContext) {}

    private fun preferredWidth(constraints: Constraints): Int {
        var width = 1
        for (field in fieldList()) {
            val measured = field.measured(constraints.loosen())
            width = maxOf(width, measured.width)
        }
        return width
    }

    private fun fieldList(): MutableList<FormField> {
        val result = ArrayList<FormField>(childList.size)
        for (child in childList) {
            if (child is FormField) {
                result.add(child)
            }
        }
        return result
    }
}
