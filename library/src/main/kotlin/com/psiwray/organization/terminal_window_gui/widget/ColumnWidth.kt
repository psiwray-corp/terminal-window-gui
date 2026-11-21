package com.psiwray.organization.terminal_window_gui.widget

/**
 * Width request for one table column.
 *
 * Every column occupies at least one cell, regardless of the request.
 *
 * @since 1.0.0
 */
public sealed class ColumnWidth {
    /**
     * Fixed cell count.
     *
     * @property cells Requested cells; values below one render as one.
     * @since 1.0.0
     */
    public data class Fixed(
        public val cells: Int,
    ) : ColumnWidth() {
        init {
            require(cells >= 0) { "cells must be non-negative, got $cells" }
        }
    }

    /**
     * Share of the width left after fixed and fractional columns.
     *
     * @property weight Positive weight.
     * @since 1.0.0
     */
    public data class Flex(
        public val weight: Double,
    ) : ColumnWidth() {
        init {
            require(weight > 0.0 && weight.isFinite()) { "weight must be positive and finite, got $weight" }
        }

        /** Integer-weight convenience constructor. @since 1.0.0 */
        public constructor(weight: Int) : this(weight.toDouble())
    }

    /**
     * Fraction of the viewport width.
     *
     * @property fraction Fraction in `(0, 1]`.
     * @since 1.0.0
     */
    public data class Fraction(
        public val fraction: Double,
    ) : ColumnWidth() {
        init {
            require(fraction > 0.0 && fraction <= 1.0) {
                "fraction must be in (0, 1], got $fraction"
            }
        }
    }

    /** Intrinsic content width, where a layout supports it. @since 1.0.0 */
    public data object Content : ColumnWidth()
}
