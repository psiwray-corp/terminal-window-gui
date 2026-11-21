package com.psiwray.organization.terminal_window_gui.command

/**
 * A named action with on-demand enablement and checked state.
 *
 * [enabled] and [checked] are evaluated only when a command menu or shortcut
 * asks; no value is cached per paint or per cell.
 *
 * @property id Stable identifier.
 * @property label Short menu label.
 * @property description Longer description.
 * @property shortcut Optional default chord.
 * @property enabled Enablement predicate.
 * @property checked Checked-state predicate.
 * @property action Action body.
 * @since 1.0.0
 */
public data class Command(
    public val id: String,
    public val label: String = id,
    public val description: String = "",
    public val shortcut: KeyChord? = null,
    public val enabled: () -> Boolean = { true },
    public val checked: () -> Boolean = { false },
    public val action: () -> Unit,
) {
    /** Returns the current enablement without caching it. @since 1.0.0 */
    public fun isEnabled(): Boolean = enabled()

    /** Returns the current checked state without caching it. @since 1.0.0 */
    public fun isChecked(): Boolean = checked()

    /**
     * Runs [action] once when enabled.
     *
     * @return whether the action ran.
     * @since 1.0.0
     */
    public fun execute(): Boolean {
        if (!enabled()) {
            return false
        }
        action()
        return true
    }
}
