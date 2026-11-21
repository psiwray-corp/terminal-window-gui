package com.psiwray.organization.terminal_window_gui.widget

/**
 * One row or column sizing rule in a [Grid].
 *
 * @since 1.0.0
 */
public sealed interface GridTrack {
    /**
     * Fixed number of cells.
     *
     * @property cells Non-negative cell count.
     * @since 1.0.0
     */
    public data class Fixed(
        public val cells: Int,
    ) : GridTrack {
        init {
            require(cells >= 0) { "cells must be non-negative, got $cells" }
        }
    }

    /**
     * Fraction of available track space.
     *
     * @property fraction Fraction in `0.0..1.0`.
     * @since 1.0.0
     */
    public data class Fraction(
        public val fraction: Float,
    ) : GridTrack {
        /** Double-precision convenience constructor. @since 1.0.0 */
        public constructor(fraction: Double) : this(fraction.toFloat())

        init {
            require(fraction.isFinite() && fraction in 0f..1f) {
                "fraction must be in 0.0..1.0, got $fraction"
            }
        }
    }

    /**
     * Share of the remaining track space.
     *
     * @property weight Positive flex weight.
     * @since 1.0.0
     */
    public data class Flex(
        public val weight: Int,
    ) : GridTrack {
        init {
            require(weight > 0) { "weight must be positive, got $weight" }
        }
    }

    /** Shared track constructors. @since 1.0.0 */
    public companion object {
        /** A fixed track. @since 1.0.0 */
        public fun fixed(cells: Int): GridTrack = Fixed(cells)

        /** A fractional track. @since 1.0.0 */
        public fun fraction(fraction: Float): GridTrack = Fraction(fraction)

        /** A flex track. @since 1.0.0 */
        public fun flex(weight: Int = 1): GridTrack = Flex(weight)

        /** [count] equal fixed tracks. @since 1.0.0 */
        public fun uniformFixed(
            count: Int,
            cells: Int,
        ): List<GridTrack> {
            require(count >= 0) { "count must be non-negative, got $count" }
            return List(count) { Fixed(cells) }
        }

        /** [count] equal fractional tracks. @since 1.0.0 */
        public fun uniformFraction(
            count: Int,
            fraction: Float,
        ): List<GridTrack> {
            require(count >= 0) { "count must be non-negative, got $count" }
            return List(count) { Fraction(fraction) }
        }

        /** [count] equal flex tracks. @since 1.0.0 */
        public fun uniformFlex(
            count: Int,
            weight: Int = 1,
        ): List<GridTrack> {
            require(count >= 0) { "count must be non-negative, got $count" }
            return List(count) { Flex(weight) }
        }
    }
}
