package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window_gui.ascii95Font
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShapeCoverageTest {
    @Test
    fun bundledFontCoversEveryPrimary() {
        val font = GuiFonts.bundled()
        assertEquals(8, font.cellWidth)
        assertEquals(16, font.cellHeight)
        assertEquals(0x003F, font.fallbackCodePoint)

        val set = ShapeSets.unicode()
        for (shape in Shape.entries) {
            val spec = assertNotNull(set[shape], "missing spec for $shape")
            assertTrue(
                font.hasGlyph(spec.codePoint),
                "bundled font lacks ${shape.name} U+${spec.codePoint.toString(16)}",
            )
            spec.fallbackCodePoint?.let { fallback ->
                assertTrue(
                    font.hasGlyph(fallback),
                    "bundled font lacks fallback for ${shape.name} U+${fallback.toString(16)}",
                )
            }
        }

        val resolved = set.resolve(font)
        assertTrue(resolved.fallbackUsed.isEmpty())
        assertTrue(resolved.lastResortUsed.isEmpty())
    }

    @Test
    fun unicodeCatalogUsesExactCodePoints() {
        val set = ShapeSets.unicode()
        assertEquals(Shape.entries.size, set.shapes.size)
        for ((shape, expected) in expectedUnicode) {
            val spec = assertNotNull(set[shape], "missing spec for $shape")
            assertEquals(expected, spec.codePoint, "primary for $shape")
        }
    }

    @Test
    fun noPrimaryUsesPrivateUseArea() {
        for (shape in Shape.entries) {
            val codePoint = assertNotNull(ShapeSets.unicode()[shape]).codePoint
            assertTrue(
                codePoint !in 0xE000..0xF8FF &&
                    codePoint !in 0xF0000..0xFFFFD &&
                    codePoint !in 0x100000..0x10FFFD,
                "${shape.name} uses private-use code point U+${codePoint.toString(16)}",
            )
        }
    }

    @Test
    fun asciiCatalogResolvesAgainstMinimalAsciiFont() {
        val resolved = ShapeSets.ascii().resolve(ascii95Font())
        assertEquals(Shape.entries.size, resolved.codePoints.size)
        assertTrue(resolved.missing.isEmpty())
        assertTrue(resolved.fallbackUsed.isEmpty())
        assertTrue(resolved.lastResortUsed.isEmpty())
        for (shape in Shape.entries) {
            val codePoint = resolved[shape]
            assertTrue(
                codePoint in 0x0020..0x007E,
                "${shape.name} resolved to non-ASCII U+${codePoint.toString(16)}",
            )
        }
        assertEquals(0x0020, resolved[Shape.PROGRESS_EMPTY])
        assertEquals(0x002D, resolved[Shape.BOX_SINGLE_H])
        assertEquals(0x007C, resolved[Shape.BOX_SINGLE_V])
        assertEquals(0x002B, resolved[Shape.BOX_SINGLE_CROSS])
        assertEquals(0x0023, resolved[Shape.PROGRESS_FULL])
        assertEquals(0x005E, resolved[Shape.SCROLL_UP])
        assertEquals(0x0076, resolved[Shape.ARROW_DOWN])
        assertEquals(0x0078, resolved[Shape.CHECK])
        assertEquals(0x0058, resolved[Shape.CROSS])
        assertEquals(0x002A, resolved[Shape.DOT])
        assertEquals(0x002E, resolved[Shape.ELLIPSIS])
        assertEquals(0x005B, resolved[Shape.BRACKET_LEFT])
        assertEquals(0x0028, resolved[Shape.PAREN_LEFT])
        assertEquals(0x007C, resolved[Shape.SPINNER_0])
        assertEquals(0x002F, resolved[Shape.SPINNER_1])
        assertEquals(0x002D, resolved[Shape.SPINNER_2])
        assertEquals(0x005C, resolved[Shape.SPINNER_3])
    }

    private companion object {
        private val expectedUnicode: Map<Shape, Int> =
            mapOf(
                Shape.BOX_SINGLE_H to 0x2500,
                Shape.BOX_SINGLE_V to 0x2502,
                Shape.BOX_SINGLE_TOP_LEFT to 0x250C,
                Shape.BOX_SINGLE_TOP_RIGHT to 0x2510,
                Shape.BOX_SINGLE_BOTTOM_LEFT to 0x2514,
                Shape.BOX_SINGLE_BOTTOM_RIGHT to 0x2518,
                Shape.BOX_SINGLE_TEE_DOWN to 0x252C,
                Shape.BOX_SINGLE_TEE_UP to 0x2534,
                Shape.BOX_SINGLE_TEE_RIGHT to 0x251C,
                Shape.BOX_SINGLE_TEE_LEFT to 0x2524,
                Shape.BOX_SINGLE_CROSS to 0x253C,
                Shape.BOX_DOUBLE_H to 0x2550,
                Shape.BOX_DOUBLE_V to 0x2551,
                Shape.BOX_DOUBLE_TOP_LEFT to 0x2554,
                Shape.BOX_DOUBLE_TOP_RIGHT to 0x2557,
                Shape.BOX_DOUBLE_BOTTOM_LEFT to 0x255A,
                Shape.BOX_DOUBLE_BOTTOM_RIGHT to 0x255D,
                Shape.BOX_DOUBLE_TEE_DOWN to 0x2566,
                Shape.BOX_DOUBLE_TEE_UP to 0x2569,
                Shape.BOX_DOUBLE_TEE_RIGHT to 0x2560,
                Shape.BOX_DOUBLE_TEE_LEFT to 0x2563,
                Shape.BOX_DOUBLE_CROSS to 0x256C,
                Shape.BOX_ROUNDED_H to 0x2500,
                Shape.BOX_ROUNDED_V to 0x2502,
                Shape.BOX_ROUNDED_TOP_LEFT to 0x256D,
                Shape.BOX_ROUNDED_TOP_RIGHT to 0x256E,
                Shape.BOX_ROUNDED_BOTTOM_LEFT to 0x256F,
                Shape.BOX_ROUNDED_BOTTOM_RIGHT to 0x2570,
                Shape.BOX_ROUNDED_TEE_DOWN to 0x252C,
                Shape.BOX_ROUNDED_TEE_UP to 0x2534,
                Shape.BOX_ROUNDED_TEE_RIGHT to 0x251C,
                Shape.BOX_ROUNDED_TEE_LEFT to 0x2524,
                Shape.BOX_ROUNDED_CROSS to 0x253C,
                Shape.BOX_HEAVY_H to 0x2501,
                Shape.BOX_HEAVY_V to 0x2503,
                Shape.BOX_HEAVY_TOP_LEFT to 0x250F,
                Shape.BOX_HEAVY_TOP_RIGHT to 0x2513,
                Shape.BOX_HEAVY_BOTTOM_LEFT to 0x2517,
                Shape.BOX_HEAVY_BOTTOM_RIGHT to 0x251B,
                Shape.BOX_HEAVY_TEE_DOWN to 0x2533,
                Shape.BOX_HEAVY_TEE_UP to 0x253B,
                Shape.BOX_HEAVY_TEE_RIGHT to 0x2523,
                Shape.BOX_HEAVY_TEE_LEFT to 0x252B,
                Shape.BOX_HEAVY_CROSS to 0x254B,
                Shape.PROGRESS_EMPTY to 0x0020,
                Shape.PROGRESS_1_8 to 0x258F,
                Shape.PROGRESS_1_4 to 0x258E,
                Shape.PROGRESS_3_8 to 0x258D,
                Shape.PROGRESS_1_2 to 0x258C,
                Shape.PROGRESS_5_8 to 0x258B,
                Shape.PROGRESS_3_4 to 0x258A,
                Shape.PROGRESS_7_8 to 0x2589,
                Shape.PROGRESS_FULL to 0x2588,
                Shape.SCROLL_V_TRACK to 0x2502,
                Shape.SCROLL_V_THUMB to 0x2588,
                Shape.SCROLL_H_TRACK to 0x2500,
                Shape.SCROLL_H_THUMB to 0x2588,
                Shape.SCROLL_UP to 0x25B2,
                Shape.SCROLL_DOWN to 0x25BC,
                Shape.SCROLL_LEFT to 0x25C0,
                Shape.SCROLL_RIGHT to 0x25B6,
                Shape.CHECK to 0x2713,
                Shape.CROSS to 0x2717,
                Shape.CLOSE to 0x2715,
                Shape.DOT to 0x2022,
                Shape.ELLIPSIS to 0x2026,
                Shape.TREE_BRANCH to 0x251C,
                Shape.TREE_LAST to 0x2514,
                Shape.TREE_VERTICAL to 0x2502,
                Shape.TREE_EXPANDED to 0x25BE,
                Shape.TREE_COLLAPSED to 0x25B8,
                Shape.ARROW_UP to 0x2191,
                Shape.ARROW_DOWN to 0x2193,
                Shape.ARROW_LEFT to 0x2190,
                Shape.ARROW_RIGHT to 0x2192,
                Shape.SORT_ASCENDING to 0x2191,
                Shape.SORT_DESCENDING to 0x2193,
                Shape.BRACKET_LEFT to 0x005B,
                Shape.BRACKET_RIGHT to 0x005D,
                Shape.PAREN_LEFT to 0x0028,
                Shape.PAREN_RIGHT to 0x0029,
                Shape.SPINNER_0 to 0x007C,
                Shape.SPINNER_1 to 0x002F,
                Shape.SPINNER_2 to 0x002D,
                Shape.SPINNER_3 to 0x005C,
            )
    }
}
