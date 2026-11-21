package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window_gui.style.Themes
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BannerTest {
    @Test
    fun bannerMatchesGolden() {
        val harness = GuiTestHarness(Banner(message = "Saved", severity = Severity.SUCCESS), columns = 20, rows = 1)
        harness.paint()
        GoldenAssert.assertRows("banner-success.txt", harness.rows())
    }

    @Test
    fun actionAndCloseRespondToMouse() {
        var actions = 0
        var closes = 0
        val banner =
            Banner(
                message = "Saved",
                action = "Retry",
                onAction = { actions++ },
                onClose = { closes++ },
            )
        val harness = GuiTestHarness(banner, columns = 20, rows = 1)
        harness.paint()
        harness.click(15, 0)
        assertEquals(1, actions)
        harness.click(19, 0)
        assertEquals(1, closes)
    }

    @Test
    fun actionAndCloseRespondToKeyboard() {
        var actions = 0
        var closes = 0
        val banner =
            Banner(
                message = "Saved",
                action = "Retry",
                onAction = { actions++ },
                onClose = { closes++ },
            )
        val harness = GuiTestHarness(banner, columns = 20, rows = 1)
        harness.paint()
        harness.click(0, 0)
        assertTrue(banner.isFocused)
        assertTrue(harness.key(Key.ENTER))
        assertEquals(1, actions)
        assertTrue(harness.key(Key.ESCAPE))
        assertEquals(1, closes)
    }

    @Test
    fun focusedBannerPaintsANonColorMarker() {
        val banner =
            Banner(
                message = "Saved",
                action = "Retry",
                onAction = {},
                onClose = {},
            )
        val harness = GuiTestHarness(banner, columns = 20, rows = 1)
        harness.paint()

        assertTrue(harness.key(Key.TAB))
        assertTrue(harness.surface.needsPaint)
        harness.paint()
        assertEquals('>'.code, harness.matrix.codePointAt(1, 0))
    }

    @Test
    fun severitySelectsThemeBackground() {
        val harness = GuiTestHarness(Banner(message = "Bad", severity = Severity.ERROR), columns = 20, rows = 1)
        harness.paint()
        assertEquals(harness.surface.theme.error, harness.matrix.backgroundAt(0, 0))
    }

    @Test
    fun messageTruncatesInNarrowBanner() {
        val harness = GuiTestHarness(Banner(message = "a long message"), columns = 10, rows = 1)
        harness.paint()
        assertTrue(harness.rows()[0].isNotEmpty())
    }

    @Test
    fun infoBannerRemainsReadableInHighContrast() {
        val harness =
            GuiTestHarness(
                Banner(message = "Gallery of v1 widgets"),
                theme = Themes.highContrast(),
                columns = 30,
                rows = 1,
            )
        harness.paint()
        assertNotEquals(harness.matrix.foregroundAt(2, 0), harness.matrix.backgroundAt(2, 0))
    }
}
