package com.psiwray.organization.terminal_window_gui.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrollModelTest {
    @Test
    fun offsetsClampForZeroOneAndManyContent() {
        val model = ScrollModel()
        model.scrollTo(10, 10)
        assertEquals(0, model.offsetX)
        assertEquals(0, model.offsetY)
        model.viewportWidth = 5
        model.viewportHeight = 5
        model.contentWidth = 1
        model.contentHeight = 1
        assertEquals(0, model.maxOffsetX)
        assertEquals(0, model.maxOffsetY)
        model.contentWidth = 20
        model.contentHeight = 30
        assertTrue(model.scrollTo(100, 100))
        assertEquals(15, model.offsetX)
        assertEquals(25, model.offsetY)
        assertTrue(model.scrollBy(-100, -100))
        assertEquals(0, model.offsetX)
        assertEquals(0, model.offsetY)
    }

    @Test
    fun contentAndViewportChangesReclampOffsets() {
        val model = ScrollModel(contentWidth = 20, contentHeight = 20, viewportWidth = 5, viewportHeight = 5)
        model.scrollTo(15, 15)
        model.contentWidth = 2
        model.contentHeight = 2
        assertEquals(0, model.offsetX)
        assertEquals(0, model.offsetY)
        model.viewportWidth = 20
        model.viewportHeight = 20
        assertEquals(0, model.maxOffsetX)
        assertEquals(0, model.maxOffsetY)
    }

    @Test
    fun ensureVisibleUsesSmallestOffsetChange() {
        val model = ScrollModel(contentWidth = 20, contentHeight = 20, viewportWidth = 5, viewportHeight = 5)
        assertTrue(model.ensureVisible(7, 7, 2, 1))
        assertEquals(4, model.offsetX)
        assertEquals(3, model.offsetY)
        assertFalse(model.ensureVisible(4, 3, 2, 1))
        assertTrue(model.ensureVisible(0, 0, 1, 1))
        assertEquals(0, model.offsetX)
        assertEquals(0, model.offsetY)
        assertTrue(model.ensureVisible(19, 19, 1, 1))
        assertEquals(15, model.offsetX)
        assertEquals(15, model.offsetY)
    }

    @Test
    fun followEndTracksGrowthAndDisablesOnScrollUp() {
        val model = ScrollModel(contentHeight = 10, viewportHeight = 5)
        model.followEnd = true
        assertEquals(5, model.offsetY)
        model.contentHeight = 20
        assertEquals(15, model.offsetY)
        assertTrue(model.scrollBy(0, -1))
        assertFalse(model.followEnd)
        model.contentHeight = 30
        assertEquals(14, model.offsetY)
        assertTrue(model.scrollBy(0, 100))
        assertEquals(25, model.offsetY)
        assertTrue(model.followEnd)
    }

    @Test
    fun pageAndHomeEndMoveViewport() {
        val model = ScrollModel(contentHeight = 30, viewportHeight = 10)
        assertTrue(model.pageDown())
        assertEquals(10, model.offsetY)
        assertTrue(model.pageDown())
        assertEquals(20, model.offsetY)
        assertTrue(model.pageUp())
        assertEquals(10, model.offsetY)
        assertTrue(model.home())
        assertEquals(0, model.offsetY)
        assertTrue(model.end())
        assertEquals(20, model.offsetY)
    }

    @Test
    fun revisionTracksChanges() {
        val model = ScrollModel(contentHeight = 20, viewportHeight = 5)
        val revision = model.revision
        assertTrue(model.scrollTo(0, 1))
        assertTrue(model.revision > revision)
        val stable = model.revision
        assertFalse(model.scrollTo(0, 1))
        assertEquals(stable, model.revision)
    }
}
