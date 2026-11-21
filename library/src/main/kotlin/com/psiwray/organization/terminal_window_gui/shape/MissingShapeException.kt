package com.psiwray.organization.terminal_window_gui.shape

/**
 * Thrown when a shape set cannot be resolved against a font.
 *
 * @property missing Shapes whose primary code point is absent from the font.
 * @property codePoints Code points involved in the failure, including missing
 *   primaries and an absent last resort.
 * @since 1.0.0
 */
public class MissingShapeException(
    missing: Set<Shape>,
    codePoints: Set<Int>,
    message: String,
) : IllegalArgumentException(message) {
    /** Shapes whose primary code point is absent. @since 1.0.0 */
    public val missing: Set<Shape> = missing.toSet()

    /** Code points involved in the failure. @since 1.0.0 */
    public val codePoints: Set<Int> = codePoints.toSet()
}
