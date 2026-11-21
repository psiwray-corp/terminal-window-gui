package com.psiwray.organization.terminal_window_gui.widget

/**
 * Line-number gutter configuration.
 *
 * The gutter is right-aligned and reserves [minDigits] number columns plus one
 * separating space. Continuation rows of a wrapped logical line leave the
 * number cell blank.
 *
 * @property enabled Whether the gutter is painted.
 * @property minDigits Minimum number of digits; must be positive.
 * @since 1.0.0
 */
public data class LineNumbers(
    public val enabled: Boolean = true,
    public val minDigits: Int = 1,
) {
    init {
        require(minDigits > 0) { "minDigits must be positive, got $minDigits" }
    }

    /** Disabled gutter. @since 1.0.0 */
    public companion object {
        /** Gutter configuration with line numbers off. @since 1.0.0 */
        public val NONE: LineNumbers = LineNumbers(false, 1)
    }
}
