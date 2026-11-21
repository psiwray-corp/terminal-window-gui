package com.psiwray.organization.terminal_window_gui.text

/**
 * Selection anchor and focus in UTF-16 character indices.
 *
 * [start] and [end] are normalized; [anchor] is the fixed end of a directional
 * selection and [focus] is the moving end. A collapsed selection has equal
 * anchor and focus.
 *
 * @property anchor Fixed selection end.
 * @property focus Moving selection end; also the caret position.
 * @since 1.0.0
 */
public data class TextSelection(
    public val anchor: Int = 0,
    public val focus: Int = 0,
) {
    init {
        require(anchor >= 0) { "anchor must be non-negative, got $anchor" }
        require(focus >= 0) { "focus must be non-negative, got $focus" }
    }

    /** Lower UTF-16 index. @since 1.0.0 */
    public val start: Int get() = if (anchor <= focus) anchor else focus

    /** Higher UTF-16 index. @since 1.0.0 */
    public val end: Int get() = if (anchor <= focus) focus else anchor

    /** Whether both ends are the same. @since 1.0.0 */
    public val isCollapsed: Boolean get() = anchor == focus

    /** Returns the selection with [anchor] at [start] and [focus] at [end]. @since 1.0.0 */
    public fun normalized(): TextSelection = TextSelection(start, end)

    /** Empty selection at the start of the text. @since 1.0.0 */
    public companion object {
        /** Empty selection at index zero. @since 1.0.0 */
        public val EMPTY: TextSelection = TextSelection(0, 0)
    }
}
