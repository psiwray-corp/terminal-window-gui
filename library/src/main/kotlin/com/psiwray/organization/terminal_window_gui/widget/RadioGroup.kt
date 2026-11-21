package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import kotlin.math.max

/**
 * Vertical group of mutually exclusive [RadioButton] options.
 *
 * Selection is exactly one key unless the group is empty. Arrow keys move to
 * the next or previous enabled option and select it; Home and End jump to the
 * ends. Only the selected option, or the last focused option when selection is
 * unavailable, participates in Tab traversal.
 *
 * @property selectedKey Selected stable key, or `null` when empty.
 * @property onChanged Callback invoked when user interaction changes selection.
 * @since 1.0.0
 */
public class RadioGroup(
    selectedKey: String? = null,
    enabled: Boolean = true,
    public var onChanged: (String) -> Unit = {},
    children: List<RadioButton> = emptyList(),
) : MultiChildWidget() {
    private var selectedKeyValue: String? = selectedKey
    private var lastFocused: RadioButton? = null

    /** Selected stable key, or `null` when the group is empty. @since 1.0.0 */
    public var selectedKey: String?
        get() = selectedKeyValue
        set(value) {
            require(value == null || childList.any { it is RadioButton && it.key == value }) {
                "selectedKey $value does not belong to this group"
            }
            if (selectedKeyValue == value) {
                return
            }
            selectedKeyValue = value
            invalidatePaint()
        }

    init {
        this.enabled = enabled
        focusScope = com.psiwray.organization.terminal_window_gui.input.FocusScope.ENTER
        val keys = HashSet<String>()
        for (child in children) {
            require(keys.add(child.key)) { "duplicate radio key ${child.key}" }
        }
        require(selectedKey == null || keys.contains(selectedKey)) {
            "selectedKey $selectedKey does not belong to this group"
        }
        if (selectedKeyValue == null && children.isNotEmpty()) {
            selectedKeyValue = children.first().key
        }
        for (child in children) {
            child.owner = this
            add(child)
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        if (childList.isEmpty()) {
            return constraints.constrain(IntSize.ZERO)
        }
        val widthBound = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        var width = 0
        for (child in childList) {
            val size = child.measured(Constraints(0, widthBound, 1, 1))
            if (size.width > width) {
                width = size.width
            }
        }
        return constraints.constrain(IntSize(max(1, width), childList.size))
    }

    override fun arrangeChildren() {
        val width = contentRect.width
        var y = 0
        for (child in childList) {
            child.arrange(Rect(0, y, width, 1))
            y++
        }
    }

    override fun paint(context: PaintContext) {}

    override fun traversalChildren(): List<Widget> {
        val active = activeButton() ?: return emptyList()
        return listOf(active)
    }

    override fun onEvent(event: GuiEvent): Boolean {
        if (!enabled || event !is GuiKeyEvent || event.action == KeyAction.RELEASE) {
            return false
        }
        val key =
            when (event.key) {
                Key.UP, Key.LEFT -> -1
                Key.DOWN, Key.RIGHT -> 1
                Key.HOME -> 0
                Key.END -> 0
                else -> return false
            }
        val order = enabledButtons()
        if (order.isEmpty()) {
            return false
        }
        val focused = guiContext?.focus?.focused as? RadioButton
        val current =
            when {
                focused != null && focused.owner === this && order.contains(focused) -> focused
                else -> activeButton()
            }
        val index = if (current == null) -1 else order.indexOf(current)
        val target =
            when (event.key) {
                Key.HOME -> order.first()
                Key.END -> order.last()
                else -> order[if (index < 0) 0 else (index + key + order.size) % order.size]
            }
        select(target.key, focus = true)
        return true
    }

    internal fun select(
        key: String,
        focus: Boolean,
    ): Boolean {
        if (!enabled) {
            return false
        }
        val target = childList.firstOrNull { it is RadioButton && it.key == key } as? RadioButton ?: return false
        if (!target.enabled) {
            return false
        }
        val changed = selectedKeyValue != key
        selectedKeyValue = key
        if (focus) {
            guiContext?.focus?.focus(target)
        }
        invalidatePaint()
        if (changed) {
            onChanged(key)
        }
        return true
    }

    internal fun onRadioFocused(
        button: RadioButton,
        focused: Boolean,
    ) {
        if (focused) {
            lastFocused = button
            invalidatePaint()
        }
    }

    private fun activeButton(): RadioButton? {
        val selected =
            selectedKeyValue?.let { key ->
                childList.firstOrNull { it is RadioButton && it.key == key } as? RadioButton
            }
        if (selected != null && selected.enabled) {
            return selected
        }
        val last = lastFocused
        if (last != null && last.enabled && childList.contains(last)) {
            return last
        }
        return enabledButtons().firstOrNull()
    }

    private fun enabledButtons(): List<RadioButton> {
        val result = ArrayList<RadioButton>(childList.size)
        for (child in childList) {
            if (child is RadioButton && child.enabled) {
                result.add(child)
            }
        }
        return result
    }
}
