package com.psiwray.organization.terminal_window_gui.widget

/**
 * A top-level menu title and its entries.
 *
 * @property title Visible title.
 * @property mnemonic Optional `Alt` activation character.
 * @property items Entries in display order.
 * @since 1.0.0
 */
public data class Menu(
    public val title: String,
    public val mnemonic: Char? = null,
    public val items: List<MenuItem> = emptyList(),
) {
    /** Child-first constructor for a menu without a mnemonic. @since 1.0.0 */
    public constructor(
        title: String,
        items: List<MenuItem>,
    ) : this(title, null, items)
}
