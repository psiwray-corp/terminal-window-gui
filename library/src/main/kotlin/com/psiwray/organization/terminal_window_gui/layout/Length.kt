package com.psiwray.organization.terminal_window_gui.layout

/**
 * A position or extent used by `Positioned`.
 *
 * `Cells` may be negative only for edges. `Fraction` is relative to the parent
 * content extent and is in `0.0..1.0`. `Content` is the measured intrinsic
 * child extent. `Fill` is the parent content extent.
 *
 * @since 1.0.0
 */
public sealed interface Length {
    /**
     * Absolute cells.
     *
     * @property value Cell count.
     * @since 1.0.0
     */
    public data class Cells(
        public val value: Int,
    ) : Length

    /**
     * Fraction of the parent content extent.
     *
     * @property value Fraction in `0.0..1.0`.
     * @since 1.0.0
     */
    public data class Fraction(
        public val value: Float,
    ) : Length {
        /** Double-precision convenience constructor. @since 1.0.0 */
        public constructor(value: Double) : this(value.toFloat())

        init {
            require(value.isFinite() && value in 0f..1f) {
                "fraction must be in 0.0..1.0, got $value"
            }
        }
    }

    /** Measured intrinsic size. @since 1.0.0 */
    public data object Content : Length

    /** Fill the available parent content extent. @since 1.0.0 */
    public data object Fill : Length
}
