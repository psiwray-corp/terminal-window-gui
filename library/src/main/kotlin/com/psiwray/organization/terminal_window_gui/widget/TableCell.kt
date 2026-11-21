package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.Shape

/**
 * One painted table cell.
 *
 * The icon, when present, occupies one cell before the text. Text is
 * normalized to one line and truncated with an ellipsis.
 *
 * @property text Cell text.
 * @property icon Optional one-cell icon.
 * @property fg Optional foreground color override.
 * @property bg Optional background color override.
 * @since 1.0.0
 */
public data class TableCell(
    public val text: String = "",
    public val icon: Shape? = null,
    public val fg: Int? = null,
    public val bg: Int? = null,
)
