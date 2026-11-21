package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.GridSize

/**
 * Base type for input and lifecycle events delivered to a widget tree.
 *
 * @since 1.0.0
 */
public sealed interface GuiEvent

/**
 * Keyboard transition.
 *
 * @property key Physical key.
 * @property action Press, release, or repeat.
 * @property modifiers Active modifier bits.
 * @property repeat Whether this is an auto-repeat transition.
 * @since 1.0.0
 */
public data class GuiKeyEvent(
    public val key: Key,
    public val action: KeyAction,
    public val modifiers: Modifiers,
    public val repeat: Boolean,
) : GuiEvent

/**
 * One Unicode scalar value entered as text.
 *
 * @property codePoint Unicode scalar value.
 * @since 1.0.0
 */
public data class GuiCharEvent(
    public val codePoint: Int,
) : GuiEvent {
    init {
        require(codePoint in 0..0x10FFFF && codePoint !in 0xD800..0xDFFF) {
            "codePoint must be a Unicode scalar value, got $codePoint"
        }
    }
}

/**
 * Pointer movement. Local coordinates are relative to the receiving widget.
 *
 * @property localX Local column.
 * @property localY Local row.
 * @property screenX Screen column retained for overlays and drag ghosts.
 * @property screenY Screen row retained for overlays and drag ghosts.
 * @property modifiers Active modifier bits.
 * @since 1.0.0
 */
public data class GuiPointerMoveEvent(
    public val localX: Int,
    public val localY: Int,
    public val screenX: Int,
    public val screenY: Int,
    public val modifiers: Modifiers,
) : GuiEvent

/**
 * Pointer button transition. Local coordinates are relative to the receiving
 * widget and may be negative while captured outside its bounds.
 *
 * @property button Button.
 * @property action Press or release.
 * @property modifiers Active modifier bits.
 * @property localX Local column.
 * @property localY Local row.
 * @property screenX Screen column.
 * @property screenY Screen row.
 * @property clickCount Consecutive click count: 1, 2, or 3.
 * @since 1.0.0
 */
public data class GuiPointerButtonEvent(
    public val button: MouseButton,
    public val action: KeyAction,
    public val modifiers: Modifiers,
    public val localX: Int,
    public val localY: Int,
    public val screenX: Int,
    public val screenY: Int,
    public val clickCount: Int,
) : GuiEvent {
    init {
        require(clickCount in 1..3) { "clickCount must be in 1..3, got $clickCount" }
    }
}

/**
 * Wheel or trackpad scroll. Local coordinates are relative to the receiving
 * widget.
 *
 * @property dx Horizontal scroll delta.
 * @property dy Vertical scroll delta.
 * @property localX Local column.
 * @property localY Local row.
 * @since 1.0.0
 */
public data class GuiScrollEvent(
    public val dx: Double,
    public val dy: Double,
    public val localX: Int,
    public val localY: Int,
) : GuiEvent

/**
 * Window focus transition.
 *
 * @property focused Whether the host window gained focus.
 * @since 1.0.0
 */
public data class GuiFocusEvent(
    public val focused: Boolean,
) : GuiEvent

/**
 * Surface resize.
 *
 * @property old Previous grid size.
 * @property new New grid size.
 * @since 1.0.0
 */
public data class GuiResizeEvent(
    public val old: GridSize,
    public val new: GridSize,
) : GuiEvent
