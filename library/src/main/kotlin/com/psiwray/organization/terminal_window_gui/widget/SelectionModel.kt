package com.psiwray.organization.terminal_window_gui.widget

import java.util.Collections

/**
 * Selection and cursor state shared by list, table, and tree widgets.
 *
 * Selected keys are stable application keys, never row indices. The ordered
 * [keys] list supplies the current visual order for range selection, cursor
 * movement, and `selectAll`; collections update it whenever their data or
 * expansion changes. When no order is supplied, range selection falls back to
 * selecting the target key only.
 *
 * @property mode Selection policy.
 * @since 1.0.0
 */
public class SelectionModel(
    mode: SelectionMode = SelectionMode.SINGLE,
) {
    private var modeValue: SelectionMode = mode
    private var keysValue: List<Any> = emptyList()
    private val selectedValue: LinkedHashSet<Any> = LinkedHashSet()
    private var cursorKey: Any? = null
    private var anchorKey: Any? = null
    private val listeners: MutableList<() -> Unit> = ArrayList()

    /** Creates a model with an initial visual order. @since 1.0.0 */
    public constructor(keys: List<Any>) : this() {
        this.keys = keys
    }

    /** Creates a model with a policy and initial visual order. @since 1.0.0 */
    public constructor(
        mode: SelectionMode,
        keys: List<Any>,
    ) : this(mode) {
        this.keys = keys
    }

    /** Selection policy. Changing it normalizes the current selection. @since 1.0.0 */
    public var mode: SelectionMode
        get() = modeValue
        set(value) {
            if (modeValue == value) {
                return
            }
            modeValue = value
            normalize()
            changed()
        }

    /** Visual order of keys used for range and cursor movement. @since 1.0.0 */
    public var keys: List<Any>
        get() = keysValue
        set(value) {
            keysValue = value.toList()
            changed()
        }

    /** Selected stable keys in selection order. @since 1.0.0 */
    public val selectedKeys: Set<Any> get() = Collections.unmodifiableSet(selectedValue)

    /** Alias for [selectedKeys]. @since 1.0.0 */
    public val selected: Set<Any> get() = selectedKeys

    /** Cursor key, or `null` when no cursor is set. @since 1.0.0 */
    public val cursor: Any? get() = cursorKey

    /** Range anchor key, or `null` when no anchor is set. @since 1.0.0 */
    public val anchor: Any? get() = anchorKey

    /** Increments whenever selection, cursor, anchor, mode, or order changes. @since 1.0.0 */
    public var revision: Long = 0
        private set

    /** Function alias for the [cursor] property. @since 1.0.0 */
    public fun cursor(): Any? = cursorKey

    /** Function alias for the [anchor] property. @since 1.0.0 */
    public fun anchor(): Any? = anchorKey

    /** Returns whether [key] is selected. @since 1.0.0 */
    public fun isSelected(key: Any): Boolean = selectedValue.contains(key)

    /**
     * Selects [key].
     *
     * [range] replaces the selection with the span between [anchor] and [key]
     * when both are in [keys]. [additive] adds to the current selection instead
     * of replacing it. In [SelectionMode.SINGLE] the request always becomes the
     * sole selection.
     *
     * @since 1.0.0
     */
    public fun select(
        key: Any,
        additive: Boolean = false,
        range: Boolean = false,
    ) {
        if (modeValue == SelectionMode.NONE) {
            cursorKey = key
            anchorKey = key
            selectedValue.clear()
            changed()
            return
        }
        if (modeValue == SelectionMode.SINGLE || !range) {
            if (modeValue == SelectionMode.MULTIPLE && additive) {
                selectedValue.add(key)
            } else {
                selectedValue.clear()
                selectedValue.add(key)
            }
            cursorKey = key
            anchorKey = key
        } else {
            val anchorIndex = keysValue.indexOf(anchorKey)
            val targetIndex = keysValue.indexOf(key)
            if (anchorIndex >= 0 && targetIndex >= 0) {
                val first = minOf(anchorIndex, targetIndex)
                val last = maxOf(anchorIndex, targetIndex)
                if (!additive) {
                    selectedValue.clear()
                }
                var index = first
                while (index <= last) {
                    selectedValue.add(keysValue[index])
                    index++
                }
            } else if (!additive) {
                selectedValue.clear()
                selectedValue.add(key)
                anchorKey = key
            } else {
                selectedValue.add(key)
            }
            cursorKey = key
        }
        changed()
    }

    /**
     * Toggles [key] in [SelectionMode.MULTIPLE], selects it in
     * [SelectionMode.SINGLE], and changes only the cursor in
     * [SelectionMode.NONE].
     *
     * @since 1.0.0
     */
    public fun toggle(key: Any) {
        when (modeValue) {
            SelectionMode.NONE -> {
                cursorKey = key
                anchorKey = key
                selectedValue.clear()
            }

            SelectionMode.SINGLE -> {
                selectedValue.clear()
                selectedValue.add(key)
                cursorKey = key
                anchorKey = key
            }

            SelectionMode.MULTIPLE -> {
                if (!selectedValue.remove(key)) {
                    selectedValue.add(key)
                }
                cursorKey = key
                anchorKey = key
            }
        }
        changed()
    }

    /** Clears every selected key and the anchor; the cursor is retained. @since 1.0.0 */
    public fun clear() {
        selectedValue.clear()
        anchorKey = null
        changed()
    }

    /**
     * Selects every key in [ordered] when non-empty, otherwise [keys].
     *
     * @since 1.0.0
     */
    public fun selectAll(ordered: List<Any> = keysValue) {
        if (modeValue == SelectionMode.NONE) {
            return
        }
        selectedValue.clear()
        if (modeValue == SelectionMode.SINGLE) {
            ordered.firstOrNull()?.let(selectedValue::add)
        } else {
            selectedValue.addAll(ordered)
        }
        if (cursorKey == null || cursorKey !in ordered) {
            cursorKey = ordered.firstOrNull()
        }
        if (anchorKey == null || anchorKey !in ordered) {
            anchorKey = cursorKey
        }
        changed()
    }

    /**
     * Moves the cursor by [delta] positions in [keys] and clamps at the ends.
     *
     * With [extend], the selection becomes the span between the anchor and the
     * new cursor. Without it, the target is selected through [select].
     *
     * @return whether the cursor moved.
     * @since 1.0.0
     */
    public fun moveCursor(
        delta: Int,
        extend: Boolean = false,
    ): Boolean {
        if (keysValue.isEmpty()) {
            return false
        }
        val current = keysValue.indexOf(cursorKey)
        val target =
            if (current < 0) {
                if (delta < 0) keysValue.lastIndex else 0
            } else {
                (current + delta).coerceIn(0, keysValue.lastIndex)
            }
        val key = keysValue[target]
        val moved = key != cursorKey
        if (!moved && !extend) {
            cursorKey = key
        }
        if (extend && modeValue == SelectionMode.MULTIPLE) {
            select(key, range = true)
        } else {
            select(key)
        }
        return moved
    }

    /**
     * Sets the cursor. In [SelectionMode.SINGLE] the key is also selected.
     * In [SelectionMode.MULTIPLE] the existing selection is retained.
     *
     * @since 1.0.0
     */
    public fun setCursor(key: Any?) {
        cursorKey = key
        anchorKey = key
        if (key == null) {
            if (modeValue == SelectionMode.SINGLE) {
                selectedValue.clear()
            }
        } else if (modeValue == SelectionMode.SINGLE) {
            selectedValue.clear()
            selectedValue.add(key)
        }
        changed()
    }

    /** Removes selected keys, the cursor, and the anchor that are absent from [available]. @since 1.0.0 */
    public fun retain(available: Collection<Any>) {
        val before = selectedValue.size
        selectedValue.retainAll(available.toSet())
        if (modeValue == SelectionMode.SINGLE && selectedValue.size > 1) {
            val keep = cursorKey?.takeIf(selectedValue::contains) ?: selectedValue.first()
            selectedValue.clear()
            selectedValue.add(keep)
        }
        if (cursorKey != null && cursorKey !in available) {
            cursorKey = selectedValue.firstOrNull() ?: available.firstOrNull()
        }
        if (anchorKey != null && anchorKey !in available) {
            anchorKey = cursorKey
        }
        if (before != selectedValue.size || cursorKey == null) {
            changed()
        }
    }

    internal fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    internal fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun normalize() {
        when (modeValue) {
            SelectionMode.NONE -> {
                selectedValue.clear()
            }

            SelectionMode.SINGLE -> {
                if (selectedValue.isNotEmpty()) {
                    val keep = cursorKey?.takeIf(selectedValue::contains) ?: selectedValue.first()
                    selectedValue.clear()
                    selectedValue.add(keep)
                    cursorKey = keep
                    anchorKey = keep
                }
            }

            SelectionMode.MULTIPLE -> {}
        }
    }

    private fun changed() {
        revision++
        for (listener in listeners.toList()) {
            listener()
        }
    }
}
