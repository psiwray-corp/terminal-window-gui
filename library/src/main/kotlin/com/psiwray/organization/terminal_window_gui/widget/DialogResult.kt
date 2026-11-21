package com.psiwray.organization.terminal_window_gui.widget

/**
 * Outcome delivered once by a [Dialog].
 *
 * [CONFIRMED] and [CANCELLED] cover the ordinary actions. [Key] carries a
 * caller-defined key and optional payload for custom actions.
 *
 * @since 1.0.0
 */
public sealed interface DialogResult {
    /** The default or confirm action completed. @since 1.0.0 */
    public data object CONFIRMED : DialogResult

    /** The dialog was cancelled or dismissed with Escape. @since 1.0.0 */
    public data object CANCELLED : DialogResult

    /**
     * A custom action result.
     *
     * @property key Caller-defined result key.
     * @property payload Optional value.
     * @since 1.0.0
     */
    public data class Key(
        public val key: String,
        public val payload: Any? = null,
    ) : DialogResult

    /** Conventional spelling alias for [CONFIRMED]. @since 1.0.0 */
    public companion object {
        /** Alias value for [CONFIRMED]. @since 1.0.0 */
        public val Confirmed: DialogResult get() = CONFIRMED

        /** Alias value for [CANCELLED]. @since 1.0.0 */
        public val Cancelled: DialogResult get() = CANCELLED
    }
}
