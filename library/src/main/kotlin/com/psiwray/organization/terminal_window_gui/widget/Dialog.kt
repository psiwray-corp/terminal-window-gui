package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.input.DismissPolicy
import com.psiwray.organization.terminal_window_gui.input.FocusScope
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.input.OverlayAnchor
import com.psiwray.organization.terminal_window_gui.input.OverlayEntry
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.internal.BoxPainter
import com.psiwray.organization.terminal_window_gui.internal.TextPainter
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.paint.TextAlignment
import kotlin.math.max
import kotlin.math.min

/**
 * Modal bordered dialog with a title, scrollable body, and bottom-right actions.
 *
 * The dialog is an overlay root. It traps Tab focus while open, maps Escape to
 * the cancel action, and maps Enter to the default action when a focused child
 * does not consume it. Clicking outside does not dismiss it. A result is
 * delivered at most once.
 *
 * @property title Dialog title painted on the top border.
 * @property body Scrollable content.
 * @property actions Action descriptors.
 * @property result Result callback.
 * @since 1.0.0
 */
public open class Dialog(
    title: String,
    public val body: Widget,
    public val actions: List<DialogAction> = emptyList(),
    result: (DialogResult) -> Unit = {},
) : ContainerWidget() {
    private var titleValue: String = title

    /** Scroll viewport around [body]. @since 1.0.0 */
    public val bodyScroll: ScrollView = ScrollView(body)

    /** Action buttons in [actions] order. @since 1.0.0 */
    public val actionButtons: List<Button>

    private val mutableActionButtons: MutableList<Button> = ArrayList()
    private var resultValue: (DialogResult) -> Unit = result
    private var delivered: Boolean = false
    private var entryValue: OverlayEntry? = null
    private var hostValue: OverlayHost? = null

    /** Dialog title. @since 1.0.0 */
    public var title: String
        get() = titleValue
        set(value) {
            if (titleValue == value) {
                return
            }
            titleValue = value
            invalidateLayout()
        }

    /** Result callback. @since 1.0.0 */
    public var result: (DialogResult) -> Unit
        get() = resultValue
        set(value) {
            resultValue = value
        }

    /** Alias for [result]. @since 1.0.0 */
    public var onResult: (DialogResult) -> Unit
        get() = resultValue
        set(value) {
            resultValue = value
        }

    /** Overlay entry while open, or `null`. @since 1.0.0 */
    public val entry: OverlayEntry? get() = entryValue

    /** Whether a result has been delivered. @since 1.0.0 */
    public val isFinished: Boolean get() = delivered

    /** Child-first constructor using an `onResult` callback name. @since 1.0.0 */
    public constructor(
        title: String,
        body: Widget,
        onResult: (DialogResult) -> Unit,
        actions: List<DialogAction> = emptyList(),
    ) : this(title, body, actions, onResult)

    init {
        focusScope = FocusScope.TRAP
        bodyScroll.tabStop = true
        actions.forEachIndexed { index, action ->
            mutableActionButtons.add(
                Button(
                    label = action.label,
                    default = action.default,
                    onActivate = { activateAction(index) },
                ),
            )
        }
        actionButtons = java.util.Collections.unmodifiableList(mutableActionButtons)
        adopt(bodyScroll)
        for (button in mutableActionButtons) {
            adopt(button)
        }
    }

    override fun onAttach() {
        hostValue = guiContext?.overlays
        if (entryValue == null) {
            entryValue = hostValue?.entries?.firstOrNull { it.widget === this }
        }
    }

    override fun onDetach() {
        entryValue = null
        hostValue = null
    }

    override fun computeContentRect(bounds: Rect): Rect = bounds.inset(1)

    override fun measure(constraints: Constraints): IntSize {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val contentMaxWidth = (maxWidth - 2).coerceAtLeast(1)
        val contentMaxHeight = (maxHeight - 2).coerceAtLeast(1)
        val bodySize =
            body.measured(
                Constraints(0, contentMaxWidth, 0, Int.MAX_VALUE),
            )
        val actionWidth =
            if (mutableActionButtons.isEmpty()) {
                0
            } else {
                mutableActionButtons.sumOf { it.measured(Constraints(0, contentMaxWidth, 1, 1)).width } +
                    (mutableActionButtons.size - 1)
            }
        val titleWidth = TextPainter.codePointCount(titleValue) + 4
        val preferredWidth =
            max(
                titleWidth,
                max(bodySize.width + 2, actionWidth + 2),
            )
        val actionHeight = if (mutableActionButtons.isEmpty()) 0 else 1
        val availableBody = (contentMaxHeight - actionHeight).coerceAtLeast(0)
        val bodyHeight = min(max(1, bodySize.height), max(1, availableBody))
        val preferredHeight = 2 + bodyHeight + actionHeight
        return constraints.constrain(IntSize(max(1, preferredWidth), max(1, preferredHeight)))
    }

    override fun arrangeChildren() {
        val content = contentRect
        val actionHeight = if (mutableActionButtons.isEmpty() || content.height < 1) 0 else 1
        val bodyHeight = (content.height - actionHeight).coerceAtLeast(0)
        bodyScroll.arrange(Rect(0, 0, content.width, bodyHeight))
        if (actionHeight > 0) {
            arrangeActions(
                width = content.width,
                y = bodyHeight,
            )
        }
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        val theme = context.theme
        BoxPainter.paint(
            context,
            BoxStyle.SINGLE,
            bounds.width,
            bounds.height,
            if (isFocused) theme.borderFocused else theme.border,
            theme.surface,
        )
        if (bounds.width > 2) {
            TextPainter.draw(
                context = context,
                line = titleValue,
                x = 2,
                y = 0,
                width = (bounds.width - 4).coerceAtLeast(0),
                alignment = TextAlignment.LEFT,
                fg = theme.text,
                bg = theme.surface,
                ellipsis = TextPainter.codePointCount(titleValue) > bounds.width - 4,
            )
            context.text(1, 0, " ", theme.text, theme.surface)
        }
        val content = contentRect
        if (!content.isEmpty) {
            context.fill(Rect(content.x, content.y, content.width, content.height), theme.text, theme.surface)
        }
    }

    override fun onCaptureEvent(event: GuiEvent): Boolean = false

    override fun onEvent(event: GuiEvent): Boolean {
        if (event !is GuiKeyEvent || event.action == KeyAction.RELEASE) {
            return false
        }
        return when (event.key) {
            Key.ESCAPE -> {
                cancel()
                true
            }

            Key.ENTER, Key.KP_ENTER -> {
                confirm()
                true
            }

            Key.F10 -> {
                false
            }

            else -> {
                false
            }
        }
    }

    /** Runs the default action, or confirms when none is declared. @since 1.0.0 */
    public fun confirm() {
        val index = actions.indexOfFirst { it.default && !it.cancel }
        if (index >= 0) {
            activateAction(index)
        } else {
            finish(DialogResult.CONFIRMED)
        }
    }

    /** Runs the cancel action, or cancels when none is declared. @since 1.0.0 */
    public fun cancel() {
        val index = actions.indexOfFirst { it.cancel }
        if (index >= 0) {
            activateAction(index)
        } else {
            finish(DialogResult.CANCELLED)
        }
    }

    /** Delivers [result] once and closes the dialog. @since 1.0.0 */
    public fun finish(result: DialogResult) {
        if (delivered) {
            return
        }
        delivered = true
        val host = hostValue
        val entry = entryValue
        entryValue = null
        if (host != null && entry != null) {
            host.remove(entry)
        }
        resultValue(result)
    }

    /** Opens at a screen cell as a modal overlay. @since 1.0.0 */
    public fun show(
        overlays: OverlayHost,
        column: Int,
        row: Int,
    ): OverlayEntry = show(overlays, OverlayAnchor.Rect(Rect(column, row, 0, 0)))

    /** Opens this dialog as a modal overlay. @since 1.0.0 */
    public open fun show(
        overlays: OverlayHost,
        anchor: OverlayAnchor = OverlayAnchor.Screen,
    ): OverlayEntry {
        if (entryValue != null) {
            return entryValue!!
        }
        hostValue = overlays
        val added =
            overlays.add(
                widget = this,
                anchor = anchor,
                dismissPolicy = DismissPolicy.NONE,
                modal = true,
                shadow = true,
            )
        entryValue = added
        return added
    }

    /** Cancels and closes the dialog. @since 1.0.0 */
    public fun dismiss() {
        cancel()
    }

    /** Closes the dialog without delivering another result. @since 1.0.0 */
    public fun close() {
        val host = hostValue
        val entry = entryValue
        entryValue = null
        if (host != null && entry != null) {
            host.remove(entry)
        }
    }

    /** Runs action [index], then delivers its configured result once. @since 1.0.0 */
    protected open fun activateAction(index: Int) {
        val action = actions.getOrNull(index) ?: return
        action.onActivate()
        val outcome =
            action.result ?: if (action.cancel) DialogResult.CANCELLED else DialogResult.CONFIRMED
        finish(outcome)
    }

    private fun arrangeActions(
        width: Int,
        y: Int,
    ) {
        val count = mutableActionButtons.size
        if (count == 0) {
            return
        }
        val gaps = count - 1
        val available = (width - gaps).coerceAtLeast(0)
        val preferred =
            mutableActionButtons.map {
                it.measured(Constraints(0, available, 1, 1)).width
            }
        val preferredTotal = preferred.sum().coerceAtLeast(1)
        var x = 0
        for (index in 0 until count) {
            val allocation =
                if (preferredTotal <= available) {
                    preferred[index]
                } else {
                    preferred[index] * available / preferredTotal
                }
            mutableActionButtons[index].arrange(Rect(x, y, allocation, 1))
            x += allocation + 1
        }
        if (preferredTotal <= available) {
            val shift = (width - (preferredTotal + gaps)).coerceAtLeast(0)
            x = shift
            for (index in 0 until count) {
                val current = mutableActionButtons[index].bounds
                mutableActionButtons[index].arrange(Rect(x, y, current.width, 1))
                x += current.width + 1
            }
        }
    }
}
