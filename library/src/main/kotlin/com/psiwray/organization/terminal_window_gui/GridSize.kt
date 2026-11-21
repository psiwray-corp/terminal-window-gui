package com.psiwray.organization.terminal_window_gui

/**
 * Immutable terminal grid dimensions in cells.
 *
 * @property columns Number of columns.
 * @property rows Number of rows.
 * @since 1.0.0
 */
public data class GridSize(
    public val columns: Int,
    public val rows: Int,
) {
    init {
        require(columns >= 0) { "columns must be non-negative, got $columns" }
        require(rows >= 0) { "rows must be non-negative, got $rows" }
    }

    /** Zero columns and rows. @since 1.0.0 */
    public companion object {
        /** Zero columns and rows. @since 1.0.0 */
        public val ZERO: GridSize = GridSize(0, 0)
    }
}
