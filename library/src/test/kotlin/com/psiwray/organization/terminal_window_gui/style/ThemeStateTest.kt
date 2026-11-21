package com.psiwray.organization.terminal_window_gui.style

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeStateTest {
    @Test
    fun statesResolveToTokenPairs() {
        val theme = Themes.dark()
        assertEquals(Style(theme.text, theme.surface), theme.style(WidgetState.NORMAL))
        assertEquals(Style(theme.text, theme.surfaceAlt), theme.style(WidgetState.FOCUSED))
        assertEquals(Style(theme.text, theme.surfaceAlt), theme.styleFor(WidgetState.HOVERED))
        assertEquals(Style(theme.selectionForeground, theme.accent), theme.style(WidgetState.PRESSED))
        assertEquals(Style(theme.textDisabled, theme.surface), theme.style(WidgetState.DISABLED))
        assertEquals(Style(theme.selectionForeground, theme.selectionBackground), theme.style(WidgetState.SELECTED))
        assertEquals(Style(theme.error, theme.surface), theme.style(WidgetState.ERROR))
    }

    @Test
    fun styleResolveUsesTheme() {
        val theme = Themes.light()
        assertEquals(theme.style(WidgetState.SELECTED), Style(0, 0).resolve(theme, WidgetState.SELECTED))
    }

    @Test
    fun builtInThemesAreDistinctAndHighContrastFocusIsVisible() {
        val dark = Themes.dark()
        val light = Themes.light()
        val contrast = Themes.highContrast()
        assertNotEquals(dark, light)
        assertNotEquals(dark, contrast)
        assertNotEquals(contrast.style(WidgetState.NORMAL).bg, contrast.style(WidgetState.FOCUSED).bg)
        assertEquals(contrast.text, contrast.borderFocused)
        assertTrue(contrast.background != contrast.surfaceAlt)
    }
}
