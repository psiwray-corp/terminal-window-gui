package com.psiwray.organization.terminal_window_gui.command

/**
 * Global and screen-scoped key bindings with conflict rejection.
 *
 * A scoped binding shadows a global binding for the same chord while that
 * screen is active. Two bindings for the same normalized chord and scope are
 * rejected at bind time.
 *
 * @since 1.0.0
 */
public class Keymap {
    private val globalBindings: MutableMap<KeyChord, Command> = LinkedHashMap()
    private val scopedBindings: MutableMap<String, MutableMap<KeyChord, Command>> = LinkedHashMap()

    /** Immutable snapshot of global bindings. @since 1.0.0 */
    public val global: Map<KeyChord, Command> get() = globalBindings.toMap()

    /**
     * Binds [chord] globally to [command].
     *
     * @throws IllegalArgumentException when the chord is already bound.
     * @since 1.0.0
     */
    public fun bind(
        chord: KeyChord,
        command: Command,
    ): Keymap {
        val normalized = chord.normalized()
        require(!globalBindings.containsKey(normalized)) { "keymap conflict: $normalized" }
        globalBindings[normalized] = command
        return this
    }

    /**
     * Binds [chord] to [command] for [scope].
     *
     * @throws IllegalArgumentException when the chord is already bound in that
     * scope.
     * @since 1.0.0
     */
    public fun bind(
        scope: String,
        chord: KeyChord,
        command: Command,
    ): Keymap {
        require(scope.isNotEmpty()) { "scope must not be empty" }
        val normalized = chord.normalized()
        val bindings = scopedBindings.getOrPut(scope) { LinkedHashMap() }
        require(!bindings.containsKey(normalized)) { "keymap conflict in $scope: $normalized" }
        bindings[normalized] = command
        return this
    }

    /**
     * Binds [chord] to [command], optionally for [scope].
     *
     * @since 1.0.0
     */
    public fun bind(
        chord: KeyChord,
        command: Command,
        scope: String?,
    ): Keymap = if (scope == null) bind(chord, command) else bind(scope, chord, command)

    /** Alias for [resolve]. @since 1.0.0 */
    public fun commandFor(
        chord: KeyChord,
        scope: String? = null,
    ): Command? = resolve(chord, scope)

    /** Removes a global binding. @since 1.0.0 */
    public fun unbind(chord: KeyChord): Command? = globalBindings.remove(chord.normalized())

    /** Removes a scoped binding. @since 1.0.0 */
    public fun unbind(
        scope: String,
        chord: KeyChord,
    ): Command? = scopedBindings[scope]?.remove(chord.normalized())

    /**
     * Returns the binding for [chord], preferring [scope] over global.
     *
     * @since 1.0.0
     */
    public fun resolve(
        chord: KeyChord,
        scope: String? = null,
    ): Command? {
        val normalized = chord.normalized()
        if (scope != null) {
            scopedBindings[scope]?.get(normalized)?.let { return it }
        }
        return globalBindings[normalized]
    }

    /**
     * Returns the command already bound to [chord] in [scope], if any.
     *
     * @since 1.0.0
     */
    public fun conflict(
        chord: KeyChord,
        scope: String? = null,
    ): Command? {
        val normalized = chord.normalized()
        if (scope != null) {
            scopedBindings[scope]?.get(normalized)?.let { return it }
        }
        return globalBindings[normalized]
    }

    /**
     * Executes the binding for [chord] at most once.
     *
     * @return whether an enabled command ran.
     * @since 1.0.0
     */
    public fun execute(
        chord: KeyChord,
        scope: String? = null,
    ): Boolean = resolve(chord, scope)?.execute() ?: false

    /** Removes all global and scoped bindings. @since 1.0.0 */
    public fun clear() {
        globalBindings.clear()
        scopedBindings.clear()
    }
}
