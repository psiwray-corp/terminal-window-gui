package com.psiwray.organization.terminal_window_gui.paint

/**
 * Deterministic result of measuring text.
 *
 * [lines] are already tab-expanded, contain no newline characters, and fit
 * within the requested width. [width] is the widest returned line in cells;
 * [height] is the number of returned lines. [truncated] is true when content
 * was omitted by the width or line limit.
 *
 * @property lines Rendered lines, one cell per Unicode code point.
 * @property width Widest line width in cells.
 * @property height Number of lines.
 * @property truncated Whether content was omitted.
 * @since 1.0.0
 */
public data class TextLayout(
    public val lines: List<String>,
    public val width: Int,
    public val height: Int,
    public val truncated: Boolean,
)
