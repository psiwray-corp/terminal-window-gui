package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.command.Command

/**
 * One entry in a [Menu] or [ContextMenu].
 *
 * Menu item height is one row. Commands expose their current label, shortcut,
 * enabled state, and checked state at paint and activation time.
 *
 * @since 1.0.0
 */
public sealed interface MenuItem {
    /**
     * A command entry.
     *
     * @property command Command to display and run.
     * @property radio Whether a checked command uses the radio marker.
     * @since 1.0.0
     */
    public data class CommandItem(
        public val command: Command,
        public val radio: Boolean = false,
    ) : MenuItem

    /** A non-focusable horizontal separator. @since 1.0.0 */
    public data object Separator : MenuItem

    /**
     * A submenu entry.
     *
     * @property title Display title.
     * @property items Nested entries.
     * @since 1.0.0
     */
    public data class Submenu(
        public val title: String,
        public val items: List<MenuItem>,
    ) : MenuItem
}
