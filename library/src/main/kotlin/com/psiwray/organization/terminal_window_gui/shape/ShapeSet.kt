package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window.font.BitmapFont
import java.util.Collections

/**
 * An immutable shape catalog and its resolution policy.
 *
 * A set may omit roles; an omitted role is treated as a missing primary with no
 * per-shape fallback. Use [resolve] to bind every role to a host font.
 *
 * @property shapes Shape specifications keyed by role.
 * @property lastResortCodePoint Code point used after a missing primary and
 *   fallback; defaults to U+003F QUESTION MARK.
 * @since 1.0.0
 */
public class ShapeSet(
    shapes: Map<Shape, ShapeSpec>,
    public val lastResortCodePoint: Int = DEFAULT_LAST_RESORT,
) {
    /** Read-only shape specifications. @since 1.0.0 */
    public val shapes: Map<Shape, ShapeSpec> =
        Collections.unmodifiableMap(LinkedHashMap(shapes))

    init {
        requireGraphicCodePoint(lastResortCodePoint, "lastResortCodePoint")
    }

    /** Returns the specification for [shape], or `null` when omitted. @since 1.0.0 */
    public operator fun get(shape: Shape): ShapeSpec? = shapes[shape]

    /**
     * Returns a copy with [shape] mapped to a new primary and fallback.
     *
     * @since 1.0.0
     */
    public fun with(
        shape: Shape,
        codePoint: Int,
        fallbackCodePoint: Int? = null,
    ): ShapeSet {
        val copy = LinkedHashMap(shapes)
        copy[shape] = ShapeSpec(codePoint, fallbackCodePoint)
        return ShapeSet(copy, lastResortCodePoint)
    }

    /**
     * Resolves every [Shape] against [font].
     *
     * Resolution order: primary, per-shape fallback, then
     * [lastResortCodePoint]. [MissingShapePolicy.FAIL] rejects any missing
     * primary before fallbacks are considered.
     *
     * @throws MissingShapeException when strict resolution fails or a shape
     *   cannot be resolved to a present glyph.
     * @since 1.0.0
     */
    public fun resolve(
        font: BitmapFont,
        policy: MissingShapePolicy = MissingShapePolicy.FALLBACK,
    ): ResolvedShapeSet {
        val resolved = LinkedHashMap<Shape, Int>(Shape.entries.size)
        val missingPrimary = LinkedHashSet<Shape>()
        val fallbackUsed = LinkedHashSet<Shape>()
        val lastResortUsed = LinkedHashSet<Shape>()
        val unresolved = LinkedHashSet<Shape>()
        val failureCodePoints = LinkedHashSet<Int>()

        for (shape in Shape.entries) {
            val spec = shapes[shape]
            val primary = spec?.codePoint
            val primaryPresent = primary != null && font.hasGlyph(primary)
            if (!primaryPresent) {
                missingPrimary.add(shape)
                if (primary != null) {
                    failureCodePoints.add(primary)
                }
            }

            if (primaryPresent) {
                resolved[shape] = primary
                continue
            }

            val fallback = spec?.fallbackCodePoint
            if (fallback != null && font.hasGlyph(fallback)) {
                resolved[shape] = fallback
                fallbackUsed.add(shape)
                continue
            }

            if (font.hasGlyph(lastResortCodePoint)) {
                resolved[shape] = lastResortCodePoint
                lastResortUsed.add(shape)
                continue
            }

            failureCodePoints.add(lastResortCodePoint)
            fallback?.let { failureCodePoints.add(it) }
            unresolved.add(shape)
        }

        if (policy == MissingShapePolicy.FAIL && missingPrimary.isNotEmpty()) {
            throw MissingShapeException(
                missingPrimary,
                failureCodePoints,
                "missing primary shape glyphs: ${describe(missingPrimary)}",
            )
        }
        if (unresolved.isNotEmpty()) {
            throw MissingShapeException(
                missingPrimary,
                failureCodePoints,
                "unresolved shapes: ${describe(unresolved)}; " +
                    "last resort U+${lastResortCodePoint.toString(16).uppercase()} is absent",
            )
        }

        return ResolvedShapeSet(
            font = font,
            codePoints = resolved,
            missing = missingPrimary,
            fallbackUsed = fallbackUsed,
            lastResortUsed = lastResortUsed,
        )
    }

    /** Mutable builder for shape catalogs. @since 1.0.0 */
    public class Builder {
        private val shapes = LinkedHashMap<Shape, ShapeSpec>()
        private var lastResort = DEFAULT_LAST_RESORT

        /**
         * Adds or replaces [shape].
         *
         * @since 1.0.0
         */
        public fun set(
            shape: Shape,
            codePoint: Int,
            fallbackCodePoint: Int? = null,
        ): Builder {
            shapes[shape] = ShapeSpec(codePoint, fallbackCodePoint)
            return this
        }

        /** Sets the global last-resort code point. @since 1.0.0 */
        public fun lastResort(codePoint: Int): Builder {
            requireGraphicCodePoint(codePoint, "lastResortCodePoint")
            lastResort = codePoint
            return this
        }

        /** Builds an immutable set. @since 1.0.0 */
        public fun build(): ShapeSet = ShapeSet(shapes, lastResort)
    }

    private fun describe(shapes: Set<Shape>): String =
        shapes.joinToString { shape ->
            val spec = this.shapes[shape]
            if (spec == null) {
                shape.name
            } else {
                "${shape.name}=U+${spec.codePoint.toString(16).uppercase()}"
            }
        }

    /** Default and fixed constants for shape catalogs. @since 1.0.0 */
    public companion object {
        /** Default last-resort code point, U+003F. @since 1.0.0 */
        public const val DEFAULT_LAST_RESORT: Int = 0x003F
    }
}
