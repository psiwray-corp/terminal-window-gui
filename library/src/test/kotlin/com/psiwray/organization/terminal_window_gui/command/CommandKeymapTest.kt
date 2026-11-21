package com.psiwray.organization.terminal_window_gui.command

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandKeymapTest {
    @Test
    fun commandEvaluatesEnablementAndCheckedOnDemand() {
        var enabledChecks = 0
        var checkedChecks = 0
        var runs = 0
        val command =
            Command(
                id = "save",
                label = "Save",
                enabled = {
                    enabledChecks++
                    true
                },
                checked = {
                    checkedChecks++
                    true
                },
                action = { runs++ },
            )

        assertTrue(command.isEnabled())
        assertTrue(command.isChecked())
        assertEquals(1, enabledChecks)
        assertEquals(1, checkedChecks)
        assertTrue(command.execute())
        assertEquals(1, runs)
    }

    @Test
    fun disabledCommandDoesNotRun() {
        var runs = 0
        val command =
            Command(
                id = "disabled",
                enabled = { false },
                action = { runs++ },
            )
        assertFalse(command.execute())
        assertEquals(0, runs)
    }

    @Test
    fun globalBindingRunsOnceAndCannotConflict() {
        var runs = 0
        val command = Command("save", action = { runs++ })
        val chord = KeyChord(Key.S, Modifiers.CONTROL)
        val keymap = Keymap().bind(chord, command)
        assertEquals(command, keymap.resolve(chord))
        assertTrue(keymap.execute(chord))
        assertEquals(1, runs)
        assertFailsWith<IllegalArgumentException> { keymap.bind(chord, command) }
    }

    @Test
    fun scopedBindingShadowsGlobalAndConflictIsRejected() {
        var globalRuns = 0
        var scopedRuns = 0
        val chord = KeyChord(Key.F, Modifiers.CONTROL)
        val global = Command("global", action = { globalRuns++ })
        val scoped = Command("scoped", action = { scopedRuns++ })
        val keymap = Keymap().bind(chord, global).bind("editor", chord, scoped)

        assertEquals(global, keymap.resolve(chord))
        assertEquals(scoped, keymap.resolve(chord, "editor"))
        assertTrue(keymap.execute(chord, "editor"))
        assertEquals(1, scopedRuns)
        assertEquals(0, globalRuns)
        assertFailsWith<IllegalArgumentException> { keymap.bind("editor", chord, global) }
        assertEquals(scoped, keymap.unbind("editor", chord))
        assertEquals(global, keymap.resolve(chord, "editor"))
    }

    @Test
    fun labelIsPlatformAware() {
        val label = KeyChord(Key.S, Modifiers.CONTROL).label()
        assertTrue(label.endsWith("+S"))
        assertTrue(label.contains("Ctrl"))
        val shift = KeyChord(Key.TAB, Modifiers.SHIFT).label()
        assertTrue(shift.contains("Shift"))
    }

    @Test
    fun disabledScopedBindingDoesNotExecute() {
        var runs = 0
        val keymap =
            Keymap().bind(
                "editor",
                KeyChord(Key.X),
                Command("cut", enabled = { false }, action = { runs++ }),
            )
        assertFalse(keymap.execute(KeyChord(Key.X), "editor"))
        assertEquals(0, runs)
        assertNull(keymap.resolve(KeyChord(Key.Y), "editor"))
    }
}
