package com.psiwray.organization.terminal_window_gui.shape

import com.psiwray.organization.terminal_window_gui.fontWith
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShapeResolutionTest {
    @Test
    fun missingPrimaryUsesPerShapeFallback() {
        val set =
            ShapeSet(
                mapOf(Shape.CHECK to ShapeSpec(0x2713, 0x0078)),
                lastResortCodePoint = 0x003F,
            )
        val resolved = set.resolve(fontWith(0x0078, 0x003F))

        assertEquals(0x0078, resolved[Shape.CHECK])
        assertTrue(resolved.missing.contains(Shape.CHECK))
        assertTrue(resolved.fallbackUsed.contains(Shape.CHECK))
        assertTrue(!resolved.lastResortUsed.contains(Shape.CHECK))
    }

    @Test
    fun missingFallbackUsesLastResort() {
        val set =
            ShapeSet(
                mapOf(Shape.CHECK to ShapeSpec(0x2713, 0x0078)),
                lastResortCodePoint = 0x003F,
            )
        val resolved = set.resolve(fontWith(0x003F))

        assertEquals(0x003F, resolved[Shape.CHECK])
        assertTrue(resolved.missing.contains(Shape.CHECK))
        assertTrue(resolved.fallbackUsed.isEmpty())
        assertTrue(resolved.lastResortUsed.contains(Shape.CHECK))
    }

    @Test
    fun missingLastResortThrows() {
        val set =
            ShapeSet(
                mapOf(Shape.CHECK to ShapeSpec(0x2713, 0x0078)),
                lastResortCodePoint = 0x003F,
            )
        val error = assertFailsWith<MissingShapeException> { set.resolve(fontWith(0x0078)) }

        assertTrue(error.message!!.contains("last resort"))
        assertTrue(error.codePoints.contains(0x003F))
    }

    @Test
    fun strictPolicyRejectsEveryMissingPrimary() {
        val set =
            ShapeSet
                .Builder()
                .set(Shape.CHECK, 0x2713, 0x0078)
                .set(Shape.CROSS, 0x2717, 0x0058)
                .lastResort(0x003F)
                .build()
        val error =
            assertFailsWith<MissingShapeException> {
                set.resolve(fontWith(0x0078, 0x0058, 0x003F), MissingShapePolicy.FAIL)
            }

        assertTrue(error.missing.contains(Shape.CHECK))
        assertTrue(error.missing.contains(Shape.CROSS))
        assertContains(error.message!!, "CHECK")
        assertContains(error.message!!, "CROSS")
    }

    @Test
    fun omittedRoleFallsBackToLastResort() {
        val resolved = ShapeSet(emptyMap()).resolve(fontWith(0x003F))

        assertEquals(Shape.entries.size, resolved.lastResortUsed.size)
        for (shape in Shape.entries) {
            assertEquals(0x003F, resolved[shape])
        }
    }

    @Test
    fun withReplacesOneRole() {
        val original = ShapeSet(mapOf(Shape.CHECK to ShapeSpec(0x2713)))
        val updated = original.with(Shape.CROSS, 0x2717, 0x0058)

        assertEquals(0x2713, original[Shape.CHECK]!!.codePoint)
        assertEquals(0x2717, updated[Shape.CROSS]!!.codePoint)
        assertEquals(0x0058, updated[Shape.CROSS]!!.fallbackCodePoint)
    }

    @Test
    fun invalidCodePointsAreRejected() {
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0x0000) }
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0x000A) }
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0x007F) }
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0xD800) }
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0x110000) }
        assertFailsWith<IllegalArgumentException> { ShapeSpec(0x2713, 0x0009) }
        assertFailsWith<IllegalArgumentException> {
            ShapeSet(emptyMap(), lastResortCodePoint = 0x0001)
        }
        assertFailsWith<IllegalArgumentException> {
            ShapeSet.Builder().lastResort(0xDFFF)
        }
    }
}
