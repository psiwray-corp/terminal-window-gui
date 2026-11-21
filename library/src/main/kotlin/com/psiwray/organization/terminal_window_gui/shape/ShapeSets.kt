package com.psiwray.organization.terminal_window_gui.shape

/**
 * Built-in shape catalogs.
 *
 * [unicode] carries the standard Unicode shape primaries and ASCII fallbacks.
 * [ascii] maps every role directly to its ASCII fallback and is intended for
 * callers that only have an ASCII font.
 *
 * @since 1.0.0
 */
public object ShapeSets {
    private const val SPACE = 0x0020
    private const val HASH = 0x0023
    private const val STAR = 0x002A
    private const val PLUS = 0x002B
    private const val DASH = 0x002D
    private const val PERIOD = 0x002E
    private const val LEFT_PAREN = 0x0028
    private const val RIGHT_PAREN = 0x0029
    private const val LEFT_BRACKET = 0x005B
    private const val RIGHT_BRACKET = 0x005D
    private const val LESS = 0x003C
    private const val GREATER = 0x003E
    private const val CARET = 0x005E
    private const val BACKSLASH = 0x005C
    private const val SLASH = 0x002F
    private const val VERTICAL = 0x007C
    private const val LETTER_V = 0x0076
    private const val LETTER_X = 0x0078
    private const val LETTER_UPPER_X = 0x0058

    private val unicode: ShapeSet =
        ShapeSet
            .Builder()
            .set(Shape.BOX_SINGLE_H, 0x2500, DASH)
            .set(Shape.BOX_SINGLE_V, 0x2502, VERTICAL)
            .set(Shape.BOX_SINGLE_TOP_LEFT, 0x250C, PLUS)
            .set(Shape.BOX_SINGLE_TOP_RIGHT, 0x2510, PLUS)
            .set(Shape.BOX_SINGLE_BOTTOM_LEFT, 0x2514, PLUS)
            .set(Shape.BOX_SINGLE_BOTTOM_RIGHT, 0x2518, PLUS)
            .set(Shape.BOX_SINGLE_TEE_DOWN, 0x252C, PLUS)
            .set(Shape.BOX_SINGLE_TEE_UP, 0x2534, PLUS)
            .set(Shape.BOX_SINGLE_TEE_RIGHT, 0x251C, PLUS)
            .set(Shape.BOX_SINGLE_TEE_LEFT, 0x2524, PLUS)
            .set(Shape.BOX_SINGLE_CROSS, 0x253C, PLUS)
            .set(Shape.BOX_DOUBLE_H, 0x2550, DASH)
            .set(Shape.BOX_DOUBLE_V, 0x2551, VERTICAL)
            .set(Shape.BOX_DOUBLE_TOP_LEFT, 0x2554, PLUS)
            .set(Shape.BOX_DOUBLE_TOP_RIGHT, 0x2557, PLUS)
            .set(Shape.BOX_DOUBLE_BOTTOM_LEFT, 0x255A, PLUS)
            .set(Shape.BOX_DOUBLE_BOTTOM_RIGHT, 0x255D, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_DOWN, 0x2566, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_UP, 0x2569, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_RIGHT, 0x2560, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_LEFT, 0x2563, PLUS)
            .set(Shape.BOX_DOUBLE_CROSS, 0x256C, PLUS)
            .set(Shape.BOX_ROUNDED_H, 0x2500, DASH)
            .set(Shape.BOX_ROUNDED_V, 0x2502, VERTICAL)
            .set(Shape.BOX_ROUNDED_TOP_LEFT, 0x256D, PLUS)
            .set(Shape.BOX_ROUNDED_TOP_RIGHT, 0x256E, PLUS)
            .set(Shape.BOX_ROUNDED_BOTTOM_LEFT, 0x256F, PLUS)
            .set(Shape.BOX_ROUNDED_BOTTOM_RIGHT, 0x2570, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_DOWN, 0x252C, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_UP, 0x2534, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_RIGHT, 0x251C, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_LEFT, 0x2524, PLUS)
            .set(Shape.BOX_ROUNDED_CROSS, 0x253C, PLUS)
            .set(Shape.BOX_HEAVY_H, 0x2501, DASH)
            .set(Shape.BOX_HEAVY_V, 0x2503, VERTICAL)
            .set(Shape.BOX_HEAVY_TOP_LEFT, 0x250F, PLUS)
            .set(Shape.BOX_HEAVY_TOP_RIGHT, 0x2513, PLUS)
            .set(Shape.BOX_HEAVY_BOTTOM_LEFT, 0x2517, PLUS)
            .set(Shape.BOX_HEAVY_BOTTOM_RIGHT, 0x251B, PLUS)
            .set(Shape.BOX_HEAVY_TEE_DOWN, 0x2533, PLUS)
            .set(Shape.BOX_HEAVY_TEE_UP, 0x253B, PLUS)
            .set(Shape.BOX_HEAVY_TEE_RIGHT, 0x2523, PLUS)
            .set(Shape.BOX_HEAVY_TEE_LEFT, 0x252B, PLUS)
            .set(Shape.BOX_HEAVY_CROSS, 0x254B, PLUS)
            .set(Shape.PROGRESS_EMPTY, SPACE, SPACE)
            .set(Shape.PROGRESS_1_8, 0x258F, HASH)
            .set(Shape.PROGRESS_1_4, 0x258E, HASH)
            .set(Shape.PROGRESS_3_8, 0x258D, HASH)
            .set(Shape.PROGRESS_1_2, 0x258C, HASH)
            .set(Shape.PROGRESS_5_8, 0x258B, HASH)
            .set(Shape.PROGRESS_3_4, 0x258A, HASH)
            .set(Shape.PROGRESS_7_8, 0x2589, HASH)
            .set(Shape.PROGRESS_FULL, 0x2588, HASH)
            .set(Shape.SCROLL_V_TRACK, 0x2502, VERTICAL)
            .set(Shape.SCROLL_V_THUMB, 0x2588, HASH)
            .set(Shape.SCROLL_H_TRACK, 0x2500, DASH)
            .set(Shape.SCROLL_H_THUMB, 0x2588, HASH)
            .set(Shape.SCROLL_UP, 0x25B2, CARET)
            .set(Shape.SCROLL_DOWN, 0x25BC, LETTER_V)
            .set(Shape.SCROLL_LEFT, 0x25C0, LESS)
            .set(Shape.SCROLL_RIGHT, 0x25B6, GREATER)
            .set(Shape.CHECK, 0x2713, LETTER_X)
            .set(Shape.CROSS, 0x2717, LETTER_UPPER_X)
            .set(Shape.CLOSE, 0x2715, LETTER_X)
            .set(Shape.DOT, 0x2022, STAR)
            .set(Shape.ELLIPSIS, 0x2026, PERIOD)
            .set(Shape.TREE_BRANCH, 0x251C, PLUS)
            .set(Shape.TREE_LAST, 0x2514, PLUS)
            .set(Shape.TREE_VERTICAL, 0x2502, VERTICAL)
            .set(Shape.TREE_EXPANDED, 0x25BE, LETTER_V)
            .set(Shape.TREE_COLLAPSED, 0x25B8, GREATER)
            .set(Shape.ARROW_UP, 0x2191, CARET)
            .set(Shape.ARROW_DOWN, 0x2193, LETTER_V)
            .set(Shape.ARROW_LEFT, 0x2190, LESS)
            .set(Shape.ARROW_RIGHT, 0x2192, GREATER)
            .set(Shape.SORT_ASCENDING, 0x2191, CARET)
            .set(Shape.SORT_DESCENDING, 0x2193, LETTER_V)
            .set(Shape.BRACKET_LEFT, LEFT_BRACKET, LEFT_BRACKET)
            .set(Shape.BRACKET_RIGHT, RIGHT_BRACKET, RIGHT_BRACKET)
            .set(Shape.PAREN_LEFT, LEFT_PAREN, LEFT_PAREN)
            .set(Shape.PAREN_RIGHT, RIGHT_PAREN, RIGHT_PAREN)
            .set(Shape.SPINNER_0, VERTICAL)
            .set(Shape.SPINNER_1, SLASH)
            .set(Shape.SPINNER_2, DASH)
            .set(Shape.SPINNER_3, BACKSLASH)
            .build()

    private val ascii: ShapeSet =
        ShapeSet
            .Builder()
            .set(Shape.BOX_SINGLE_H, DASH)
            .set(Shape.BOX_SINGLE_V, VERTICAL)
            .set(Shape.BOX_SINGLE_TOP_LEFT, PLUS)
            .set(Shape.BOX_SINGLE_TOP_RIGHT, PLUS)
            .set(Shape.BOX_SINGLE_BOTTOM_LEFT, PLUS)
            .set(Shape.BOX_SINGLE_BOTTOM_RIGHT, PLUS)
            .set(Shape.BOX_SINGLE_TEE_DOWN, PLUS)
            .set(Shape.BOX_SINGLE_TEE_UP, PLUS)
            .set(Shape.BOX_SINGLE_TEE_RIGHT, PLUS)
            .set(Shape.BOX_SINGLE_TEE_LEFT, PLUS)
            .set(Shape.BOX_SINGLE_CROSS, PLUS)
            .set(Shape.BOX_DOUBLE_H, DASH)
            .set(Shape.BOX_DOUBLE_V, VERTICAL)
            .set(Shape.BOX_DOUBLE_TOP_LEFT, PLUS)
            .set(Shape.BOX_DOUBLE_TOP_RIGHT, PLUS)
            .set(Shape.BOX_DOUBLE_BOTTOM_LEFT, PLUS)
            .set(Shape.BOX_DOUBLE_BOTTOM_RIGHT, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_DOWN, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_UP, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_RIGHT, PLUS)
            .set(Shape.BOX_DOUBLE_TEE_LEFT, PLUS)
            .set(Shape.BOX_DOUBLE_CROSS, PLUS)
            .set(Shape.BOX_ROUNDED_H, DASH)
            .set(Shape.BOX_ROUNDED_V, VERTICAL)
            .set(Shape.BOX_ROUNDED_TOP_LEFT, PLUS)
            .set(Shape.BOX_ROUNDED_TOP_RIGHT, PLUS)
            .set(Shape.BOX_ROUNDED_BOTTOM_LEFT, PLUS)
            .set(Shape.BOX_ROUNDED_BOTTOM_RIGHT, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_DOWN, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_UP, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_RIGHT, PLUS)
            .set(Shape.BOX_ROUNDED_TEE_LEFT, PLUS)
            .set(Shape.BOX_ROUNDED_CROSS, PLUS)
            .set(Shape.BOX_HEAVY_H, DASH)
            .set(Shape.BOX_HEAVY_V, VERTICAL)
            .set(Shape.BOX_HEAVY_TOP_LEFT, PLUS)
            .set(Shape.BOX_HEAVY_TOP_RIGHT, PLUS)
            .set(Shape.BOX_HEAVY_BOTTOM_LEFT, PLUS)
            .set(Shape.BOX_HEAVY_BOTTOM_RIGHT, PLUS)
            .set(Shape.BOX_HEAVY_TEE_DOWN, PLUS)
            .set(Shape.BOX_HEAVY_TEE_UP, PLUS)
            .set(Shape.BOX_HEAVY_TEE_RIGHT, PLUS)
            .set(Shape.BOX_HEAVY_TEE_LEFT, PLUS)
            .set(Shape.BOX_HEAVY_CROSS, PLUS)
            .set(Shape.PROGRESS_EMPTY, SPACE)
            .set(Shape.PROGRESS_1_8, HASH)
            .set(Shape.PROGRESS_1_4, HASH)
            .set(Shape.PROGRESS_3_8, HASH)
            .set(Shape.PROGRESS_1_2, HASH)
            .set(Shape.PROGRESS_5_8, HASH)
            .set(Shape.PROGRESS_3_4, HASH)
            .set(Shape.PROGRESS_7_8, HASH)
            .set(Shape.PROGRESS_FULL, HASH)
            .set(Shape.SCROLL_V_TRACK, VERTICAL)
            .set(Shape.SCROLL_V_THUMB, HASH)
            .set(Shape.SCROLL_H_TRACK, DASH)
            .set(Shape.SCROLL_H_THUMB, HASH)
            .set(Shape.SCROLL_UP, CARET)
            .set(Shape.SCROLL_DOWN, LETTER_V)
            .set(Shape.SCROLL_LEFT, LESS)
            .set(Shape.SCROLL_RIGHT, GREATER)
            .set(Shape.CHECK, LETTER_X)
            .set(Shape.CROSS, LETTER_UPPER_X)
            .set(Shape.CLOSE, LETTER_X)
            .set(Shape.DOT, STAR)
            .set(Shape.ELLIPSIS, PERIOD)
            .set(Shape.TREE_BRANCH, PLUS)
            .set(Shape.TREE_LAST, PLUS)
            .set(Shape.TREE_VERTICAL, VERTICAL)
            .set(Shape.TREE_EXPANDED, LETTER_V)
            .set(Shape.TREE_COLLAPSED, GREATER)
            .set(Shape.ARROW_UP, CARET)
            .set(Shape.ARROW_DOWN, LETTER_V)
            .set(Shape.ARROW_LEFT, LESS)
            .set(Shape.ARROW_RIGHT, GREATER)
            .set(Shape.SORT_ASCENDING, CARET)
            .set(Shape.SORT_DESCENDING, LETTER_V)
            .set(Shape.BRACKET_LEFT, LEFT_BRACKET)
            .set(Shape.BRACKET_RIGHT, RIGHT_BRACKET)
            .set(Shape.PAREN_LEFT, LEFT_PAREN)
            .set(Shape.PAREN_RIGHT, RIGHT_PAREN)
            .set(Shape.SPINNER_0, VERTICAL)
            .set(Shape.SPINNER_1, SLASH)
            .set(Shape.SPINNER_2, DASH)
            .set(Shape.SPINNER_3, BACKSLASH)
            .build()

    /**
     * Returns the standard Unicode catalog with ASCII fallbacks.
     *
     * @since 1.0.0
     */
    public fun unicode(): ShapeSet = unicode

    /**
     * Returns the ASCII-only catalog.
     *
     * @since 1.0.0
     */
    public fun ascii(): ShapeSet = ascii
}
