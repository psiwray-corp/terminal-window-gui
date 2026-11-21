package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window.font.BitmapFont
import java.util.Collections

/**
 * A shape catalog bound to one font.
 *
 * Instances are created by [ShapeSet.resolve] and contain a code point for
 * every [Shape]. The sets expose which roles used a fallback or the global last
 * resort, which callers can use for diagnostics and tests.
 *
 * @property font Font against which the catalog was resolved.
 * @property codePoints Resolved code point for every shape role.
 * @property missing Shapes whose primary was absent from [font].
 * @property fallbackUsed Shapes resolved through their per-shape fallback.
 * @property lastResortUsed Shapes resolved through the global last resort.
 * @since 1.0.0
 */
public class ResolvedShapeSet internal constructor(
    public val font: BitmapFont,
    codePoints: Map<Shape, Int>,
    missing: Set<Shape>,
    fallbackUsed: Set<Shape>,
    lastResortUsed: Set<Shape>,
) {
    /** Read-only resolved code points. @since 1.0.0 */
    public val codePoints: Map<Shape, Int> =
        Collections.unmodifiableMap(LinkedHashMap(codePoints))

    /** Shapes whose primary code point is absent. @since 1.0.0 */
    public val missing: Set<Shape> = Collections.unmodifiableSet(LinkedHashSet(missing))

    /** Shapes resolved through their per-shape fallback. @since 1.0.0 */
    public val fallbackUsed: Set<Shape> =
        Collections.unmodifiableSet(LinkedHashSet(fallbackUsed))

    /** Shapes resolved through the global last resort. @since 1.0.0 */
    public val lastResortUsed: Set<Shape> =
        Collections.unmodifiableSet(LinkedHashSet(lastResortUsed))

    /** Returns the resolved code point for [shape]. @since 1.0.0 */
    public operator fun get(shape: Shape): Int =
        codePoints[shape] ?: throw IllegalArgumentException("shape is not resolved: $shape")
}
