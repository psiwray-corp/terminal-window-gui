package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.widget.FixedSizeWidget
import com.psiwray.organization.terminal_window_gui.widget.MultiChildWidget
import com.psiwray.organization.terminal_window_gui.widget.Widget

internal open class InteractionWidget(
    val name: String = "widget",
    width: Int = 1,
    height: Int = 1,
    focusable: Boolean = true,
    private val handler: ((GuiEvent) -> Boolean)? = null,
    private val captureHandler: ((GuiEvent) -> Boolean)? = null,
) : FixedSizeWidget(width, height) {
    val events: MutableList<GuiEvent> = ArrayList()
    val captureEvents: MutableList<GuiEvent> = ArrayList()
    val focusChanges: MutableList<Boolean> = ArrayList()
    val hoverChanges: MutableList<Boolean> = ArrayList()

    init {
        this.focusable = focusable
    }

    override fun onEvent(event: GuiEvent): Boolean {
        events.add(event)
        return handler?.invoke(event) == true
    }

    override fun onCaptureEvent(event: GuiEvent): Boolean {
        captureEvents.add(event)
        return captureHandler?.invoke(event) == true
    }

    override fun onFocusChanged(focused: Boolean) {
        focusChanges.add(focused)
    }

    override fun onHoverChanged(hovered: Boolean) {
        hoverChanges.add(hovered)
    }
}

internal class BubbleOnlyContainer(
    children: List<Widget> = emptyList(),
) : MultiChildWidget() {
    val events: MutableList<GuiEvent> = ArrayList()

    init {
        for (child in children) {
            add(child)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        events.add(event)
        return false
    }

    override fun measure(constraints: Constraints): IntSize = constraints.constrain(IntSize(10, 5))

    override fun arrangeChildren() {
        var index = 0
        for (child in childList) {
            child.arrange(Rect(index, 0, 1, 1))
            index++
        }
    }

    override fun paint(context: PaintContext) {}
}

internal open class TestContainer(
    children: List<Widget> = emptyList(),
    private val offsetX: Int = 0,
    private val offsetY: Int = 0,
    private val fillChildren: Boolean = false,
    private val preferred: IntSize = IntSize(10, 5),
) : MultiChildWidget() {
    val events: MutableList<GuiEvent> = ArrayList()
    val captureEvents: MutableList<GuiEvent> = ArrayList()

    init {
        for (child in children) {
            add(child)
        }
    }

    override fun onEvent(event: GuiEvent): Boolean {
        events.add(event)
        return false
    }

    override fun onCaptureEvent(event: GuiEvent): Boolean {
        captureEvents.add(event)
        return false
    }

    override fun measure(constraints: Constraints): IntSize = constraints.constrain(preferred)

    override fun arrangeChildren() {
        var index = 0
        for (child in childList) {
            if (fillChildren) {
                child.arrange(Rect(0, 0, bounds.width, bounds.height))
            } else {
                child.arrange(Rect(offsetX + index, offsetY, 1, 1))
            }
            index++
        }
    }

    override fun paint(context: PaintContext) {}
}
