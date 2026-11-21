package com.psiwray.organization.terminal_window_gui.shape

/**
 * Primary code point and optional per-shape fallback code point.
 *
 * Both values must be Unicode scalar values that are not control characters.
 *
 * @property codePoint Primary Unicode code point for the shape.
 * @property fallbackCodePoint Fallback code point used when the primary is
 *   absent from the font, or `null` when no per-shape fallback is declared.
 * @since 1.0.0
 */
public data class ShapeSpec(
    public val codePoint: Int,
    public val fallbackCodePoint: Int? = null,
) {
    init {
        requireGraphicCodePoint(codePoint, "codePoint")
        fallbackCodePoint?.let { requireGraphicCodePoint(it, "fallbackCodePoint") }
    }
}

internal fun requireGraphicCodePoint(
    codePoint: Int,
    label: String,
) {
    require(codePoint in 0x20..0x10FFFF) {
        "$label must be a Unicode scalar value at or above U+0020, got U+${codePoint.toString(16).uppercase()}"
    }
    require(codePoint !in 0xD800..0xDFFF) {
        "$label must not be a surrogate, got U+${codePoint.toString(16).uppercase()}"
    }
    require(Character.getType(codePoint) != Character.CONTROL.toInt()) {
        "$label must not be a control character, got U+${codePoint.toString(16).uppercase()}"
    }
}
