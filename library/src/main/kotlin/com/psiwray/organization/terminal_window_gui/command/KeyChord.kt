package com.psiwray.organization.terminal_window_gui.command

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers

/**
 * A key plus modifier bits used as a command binding.
 *
 * @property key Physical key.
 * @property modifiers Active modifiers.
 * @since 1.0.0
 */
public data class KeyChord(
    public val key: Key,
    public val modifiers: Modifiers = Modifiers.NONE,
) {
    /**
     * Returns a platform-aware display label such as `Ctrl+Shift+S`.
     *
     * @since 1.0.0
     */
    public fun label(): String {
        val mac = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
        val parts = ArrayList<String>(4)
        if (modifiers.has(Modifiers.CONTROL)) {
            parts.add("Ctrl")
        }
        if (modifiers.has(Modifiers.ALT)) {
            parts.add(if (mac) "Option" else "Alt")
        }
        if (modifiers.has(Modifiers.SUPER)) {
            parts.add(if (mac) "Cmd" else "Super")
        }
        if (modifiers.has(Modifiers.SHIFT)) {
            parts.add("Shift")
        }
        parts.add(keyLabel(key))
        return parts.joinToString("+")
    }

    /** Returns [label]. @since 1.0.0 */
    override fun toString(): String = label()

    internal fun normalized(): KeyChord {
        val mask = Modifiers.SHIFT.bits or Modifiers.CONTROL.bits or Modifiers.ALT.bits or Modifiers.SUPER.bits
        return KeyChord(key, Modifiers(modifiers.bits and mask))
    }

    private fun keyLabel(key: Key): String =
        when (key) {
            Key.SPACE -> "Space"

            Key.ENTER, Key.KP_ENTER -> "Enter"

            Key.ESCAPE -> "Esc"

            Key.TAB -> "Tab"

            Key.BACKSPACE -> "Backspace"

            Key.DELETE -> "Delete"

            Key.INSERT -> "Insert"

            Key.LEFT -> "Left"

            Key.RIGHT -> "Right"

            Key.UP -> "Up"

            Key.DOWN -> "Down"

            Key.HOME -> "Home"

            Key.END -> "End"

            Key.PAGE_UP -> "PageUp"

            Key.PAGE_DOWN -> "PageDown"

            Key.NUM_0,
            Key.NUM_1,
            Key.NUM_2,
            Key.NUM_3,
            Key.NUM_4,
            Key.NUM_5,
            Key.NUM_6,
            Key.NUM_7,
            Key.NUM_8,
            Key.NUM_9,
            -> key.name.substringAfter('_')

            Key.KP_0,
            Key.KP_1,
            Key.KP_2,
            Key.KP_3,
            Key.KP_4,
            Key.KP_5,
            Key.KP_6,
            Key.KP_7,
            Key.KP_8,
            Key.KP_9,
            -> "Num" + key.name.substringAfter('_')

            else -> key.name.replace('_', ' ')
        }
}
