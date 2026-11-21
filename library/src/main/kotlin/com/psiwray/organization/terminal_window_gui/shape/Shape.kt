package com.psiwray.organization.terminal_window_gui.shape

/**
 * Semantic shape roles used by widgets.
 *
 * Widgets request roles rather than Unicode code points. A [ShapeSet] maps each
 * role to a primary code point and an optional fallback; [ShapeSet.resolve]
 * binds the roles to a host [com.psiwray.organization.terminal_window.font.BitmapFont].
 *
 * Box-drawing roles use the `BOX_<STYLE>_<ROLE>` form. Rounded edges, tees, and
 * crosses reuse the single-line code points; only the four corners differ.
 * Heavy roles are reserved for v1.1 widgets but are part of the enum and the
 * bundled font.
 *
 * @since 1.0.0
 */
public enum class Shape {
    /** Single-line horizontal box edge. @since 1.0.0 */
    BOX_SINGLE_H,

    /** Single-line vertical box edge. @since 1.0.0 */
    BOX_SINGLE_V,

    /** Single-line top-left corner. @since 1.0.0 */
    BOX_SINGLE_TOP_LEFT,

    /** Single-line top-right corner. @since 1.0.0 */
    BOX_SINGLE_TOP_RIGHT,

    /** Single-line bottom-left corner. @since 1.0.0 */
    BOX_SINGLE_BOTTOM_LEFT,

    /** Single-line bottom-right corner. @since 1.0.0 */
    BOX_SINGLE_BOTTOM_RIGHT,

    /** Single-line tee with a downward stem. @since 1.0.0 */
    BOX_SINGLE_TEE_DOWN,

    /** Single-line tee with an upward stem. @since 1.0.0 */
    BOX_SINGLE_TEE_UP,

    /** Single-line tee with a rightward stem. @since 1.0.0 */
    BOX_SINGLE_TEE_RIGHT,

    /** Single-line tee with a leftward stem. @since 1.0.0 */
    BOX_SINGLE_TEE_LEFT,

    /** Single-line cross. @since 1.0.0 */
    BOX_SINGLE_CROSS,

    /** Double-line horizontal box edge. @since 1.0.0 */
    BOX_DOUBLE_H,

    /** Double-line vertical box edge. @since 1.0.0 */
    BOX_DOUBLE_V,

    /** Double-line top-left corner. @since 1.0.0 */
    BOX_DOUBLE_TOP_LEFT,

    /** Double-line top-right corner. @since 1.0.0 */
    BOX_DOUBLE_TOP_RIGHT,

    /** Double-line bottom-left corner. @since 1.0.0 */
    BOX_DOUBLE_BOTTOM_LEFT,

    /** Double-line bottom-right corner. @since 1.0.0 */
    BOX_DOUBLE_BOTTOM_RIGHT,

    /** Double-line tee with a downward stem. @since 1.0.0 */
    BOX_DOUBLE_TEE_DOWN,

    /** Double-line tee with an upward stem. @since 1.0.0 */
    BOX_DOUBLE_TEE_UP,

    /** Double-line tee with a rightward stem. @since 1.0.0 */
    BOX_DOUBLE_TEE_RIGHT,

    /** Double-line tee with a leftward stem. @since 1.0.0 */
    BOX_DOUBLE_TEE_LEFT,

    /** Double-line cross. @since 1.0.0 */
    BOX_DOUBLE_CROSS,

    /** Rounded horizontal box edge, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_H,

    /** Rounded vertical box edge, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_V,

    /** Rounded top-left corner. @since 1.0.0 */
    BOX_ROUNDED_TOP_LEFT,

    /** Rounded top-right corner. @since 1.0.0 */
    BOX_ROUNDED_TOP_RIGHT,

    /** Rounded bottom-left corner. @since 1.0.0 */
    BOX_ROUNDED_BOTTOM_LEFT,

    /** Rounded bottom-right corner. @since 1.0.0 */
    BOX_ROUNDED_BOTTOM_RIGHT,

    /** Rounded downward tee, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_TEE_DOWN,

    /** Rounded upward tee, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_TEE_UP,

    /** Rounded rightward tee, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_TEE_RIGHT,

    /** Rounded leftward tee, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_TEE_LEFT,

    /** Rounded cross, reusing the single-line glyph. @since 1.0.0 */
    BOX_ROUNDED_CROSS,

    /** Heavy horizontal box edge, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_H,

    /** Heavy vertical box edge, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_V,

    /** Heavy top-left corner, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TOP_LEFT,

    /** Heavy top-right corner, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TOP_RIGHT,

    /** Heavy bottom-left corner, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_BOTTOM_LEFT,

    /** Heavy bottom-right corner, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_BOTTOM_RIGHT,

    /** Heavy tee with a downward stem, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TEE_DOWN,

    /** Heavy tee with an upward stem, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TEE_UP,

    /** Heavy tee with a rightward stem, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TEE_RIGHT,

    /** Heavy tee with a leftward stem, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_TEE_LEFT,

    /** Heavy cross, reserved for v1.1. @since 1.0.0 */
    BOX_HEAVY_CROSS,

    /** Empty progress cell. @since 1.0.0 */
    PROGRESS_EMPTY,

    /** One-eighth progress cell. @since 1.0.0 */
    PROGRESS_1_8,

    /** One-quarter progress cell. @since 1.0.0 */
    PROGRESS_1_4,

    /** Three-eighths progress cell. @since 1.0.0 */
    PROGRESS_3_8,

    /** Half progress cell. @since 1.0.0 */
    PROGRESS_1_2,

    /** Five-eighths progress cell. @since 1.0.0 */
    PROGRESS_5_8,

    /** Three-quarter progress cell. @since 1.0.0 */
    PROGRESS_3_4,

    /** Seven-eighths progress cell. @since 1.0.0 */
    PROGRESS_7_8,

    /** Full progress cell. @since 1.0.0 */
    PROGRESS_FULL,

    /** Vertical scrollbar track. @since 1.0.0 */
    SCROLL_V_TRACK,

    /** Vertical scrollbar thumb. @since 1.0.0 */
    SCROLL_V_THUMB,

    /** Horizontal scrollbar track. @since 1.0.0 */
    SCROLL_H_TRACK,

    /** Horizontal scrollbar thumb. @since 1.0.0 */
    SCROLL_H_THUMB,

    /** Up scrollbar control. @since 1.0.0 */
    SCROLL_UP,

    /** Down scrollbar control. @since 1.0.0 */
    SCROLL_DOWN,

    /** Left scrollbar control. @since 1.0.0 */
    SCROLL_LEFT,

    /** Right scrollbar control. @since 1.0.0 */
    SCROLL_RIGHT,

    /** Check indicator. @since 1.0.0 */
    CHECK,

    /** Cross indicator. @since 1.0.0 */
    CROSS,

    /** Close indicator. @since 1.0.0 */
    CLOSE,

    /** Bullet or radio indicator. @since 1.0.0 */
    DOT,

    /** Ellipsis. @since 1.0.0 */
    ELLIPSIS,

    /** Tree branch marker. @since 1.0.0 */
    TREE_BRANCH,

    /** Last tree item marker. @since 1.0.0 */
    TREE_LAST,

    /** Vertical tree guide. @since 1.0.0 */
    TREE_VERTICAL,

    /** Expanded tree marker. @since 1.0.0 */
    TREE_EXPANDED,

    /** Collapsed tree marker. @since 1.0.0 */
    TREE_COLLAPSED,

    /** Up arrow. @since 1.0.0 */
    ARROW_UP,

    /** Down arrow. @since 1.0.0 */
    ARROW_DOWN,

    /** Left arrow. @since 1.0.0 */
    ARROW_LEFT,

    /** Right arrow. @since 1.0.0 */
    ARROW_RIGHT,

    /** Ascending sort indicator. @since 1.0.0 */
    SORT_ASCENDING,

    /** Descending sort indicator. @since 1.0.0 */
    SORT_DESCENDING,

    /** Left bracket. @since 1.0.0 */
    BRACKET_LEFT,

    /** Right bracket. @since 1.0.0 */
    BRACKET_RIGHT,

    /** Left parenthesis. @since 1.0.0 */
    PAREN_LEFT,

    /** Right parenthesis. @since 1.0.0 */
    PAREN_RIGHT,

    /** First spinner frame. @since 1.0.0 */
    SPINNER_0,

    /** Second spinner frame. @since 1.0.0 */
    SPINNER_1,

    /** Third spinner frame. @since 1.0.0 */
    SPINNER_2,

    /** Fourth spinner frame. @since 1.0.0 */
    SPINNER_3,
}
