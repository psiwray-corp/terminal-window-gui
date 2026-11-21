package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.Key

/**
 * Maps physical keys to the ASCII character they represent for mnemonics.
 *
 * @since 1.0.0
 */
internal object MnemonicKeys {
    /** Returns the lowercase ASCII character for [key], or `0`. */
    fun codePoint(key: Key): Int =
        when (key) {
            Key.A -> 'a'.code
            Key.B -> 'b'.code
            Key.C -> 'c'.code
            Key.D -> 'd'.code
            Key.E -> 'e'.code
            Key.F -> 'f'.code
            Key.G -> 'g'.code
            Key.H -> 'h'.code
            Key.I -> 'i'.code
            Key.J -> 'j'.code
            Key.K -> 'k'.code
            Key.L -> 'l'.code
            Key.M -> 'm'.code
            Key.N -> 'n'.code
            Key.O -> 'o'.code
            Key.P -> 'p'.code
            Key.Q -> 'q'.code
            Key.R -> 'r'.code
            Key.S -> 's'.code
            Key.T -> 't'.code
            Key.U -> 'u'.code
            Key.V -> 'v'.code
            Key.W -> 'w'.code
            Key.X -> 'x'.code
            Key.Y -> 'y'.code
            Key.Z -> 'z'.code
            Key.NUM_0 -> '0'.code
            Key.NUM_1 -> '1'.code
            Key.NUM_2 -> '2'.code
            Key.NUM_3 -> '3'.code
            Key.NUM_4 -> '4'.code
            Key.NUM_5 -> '5'.code
            Key.NUM_6 -> '6'.code
            Key.NUM_7 -> '7'.code
            Key.NUM_8 -> '8'.code
            Key.NUM_9 -> '9'.code
            else -> 0
        }
}
