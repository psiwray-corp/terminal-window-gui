package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.paint.TextAlignment

/**
 * Header and cell definition for one table column.
 *
 * @property title Header title.
 * @property width Width request.
 * @property alignment Header and cell text alignment.
 * @property cell Cell builder called only for visible rows.
 * @since 1.0.0
 */
public class TableColumn<T>(
    public val title: String = "",
    public val width: ColumnWidth = ColumnWidth.Flex(1),
    public val alignment: TextAlignment = TextAlignment.LEFT,
    public val cell: (T) -> TableCell = { TableCell(it.toString()) },
) {
    /** Fixed-width convenience constructor. @since 1.0.0 */
    public constructor(
        title: String,
        cells: Int,
        alignment: TextAlignment = TextAlignment.LEFT,
        cell: (T) -> TableCell = { TableCell(it.toString()) },
    ) : this(title, ColumnWidth.Fixed(cells), alignment, cell)
}
