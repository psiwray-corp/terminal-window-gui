package com.psiwray.organization.terminal_window_gui.text

import com.psiwray.organization.terminal_window_gui.input.Clipboard
import kotlin.math.min

/**
 * Mutable text, selection, and undo state shared by text widgets.
 *
 * All public indices are UTF-16 character indices. Edits, caret movement, and
 * length limits snap to Unicode code-point boundaries, so a surrogate pair is
 * never split. Word movement uses the ASCII v1 boundary rules in
 * [WordBoundary].
 *
 * Typing coalesces into one undo entry while edits occur at the same caret
 * within [COALESCE_NANOS]. Cursor movement, paste, cut, deletion, and
 * [setText] break coalescing. The history is bounded to 200 snapshots.
 *
 * @property text Current text.
 * @property selection Anchor and focus.
 * @property caret Current focus index.
 * @property maxLength Maximum text length in Unicode code points.
 * @property revision Increments whenever text or selection changes.
 * @property clock Monotonic clock used for typing coalescing.
 * @since 1.0.0
 */
public class TextEditingController(
    text: String = "",
    public var maxLength: Int = Int.MAX_VALUE,
    public var clock: () -> Long = { System.nanoTime() },
) {
    private var textValue: String = text
    private var selectionValue: TextSelection = TextSelection(text.length, text.length)
    private var revisionValue: Long = 0
    private val history: EditHistory = EditHistory()
    private val textListeners: MutableList<() -> Unit> = ArrayList()
    private val selectionListeners: MutableList<() -> Unit> = ArrayList()
    private var lastKind: EditKind = EditKind.OTHER
    private var lastEditNanos: Long = Long.MIN_VALUE
    private var lastAfterCaret: Int = -1

    init {
        require(maxLength >= 0) { "maxLength must be non-negative, got $maxLength" }
        val trimmed = Caret.take(textValue, maxLength)
        if (trimmed != textValue) {
            textValue = trimmed
        }
        if (selectionValue.focus > textValue.length) {
            selectionValue = TextSelection(textValue.length, textValue.length)
        }
    }

    /** Current text. @since 1.0.0 */
    public val text: String get() = textValue

    /** Current selection. @since 1.0.0 */
    public var selection: TextSelection
        get() = selectionValue
        set(value) {
            val next = TextSelection(Caret.snap(textValue, value.anchor), Caret.snap(textValue, value.focus))
            if (next == selectionValue) {
                return
            }
            selectionValue = next
            breakCoalescing()
            bump()
            notifySelectionChanged()
        }

    /** Current caret position, equal to [TextSelection.focus]. @since 1.0.0 */
    public val caret: Int get() = selectionValue.focus

    /** Increments whenever text or selection changes. @since 1.0.0 */
    public val revision: Long get() = revisionValue

    /** Whether an undo snapshot is available. @since 1.0.0 */
    public val canUndo: Boolean get() = history.canUndo()

    /** Whether a redo snapshot is available. @since 1.0.0 */
    public val canRedo: Boolean get() = history.canRedo()

    /** Currently selected text. @since 1.0.0 */
    public val selectedText: String get() = textValue.substring(selectionValue.start, selectionValue.end)

    /** Replaces the text, records undo state, and places the caret at the old offset. @since 1.0.0 */
    public fun setText(value: String) {
        val nextText = Caret.take(value, maxLength)
        if (nextText == textValue) {
            return
        }
        applyEdit(nextText, TextSelection(nextText.length, nextText.length), EditKind.OTHER)
    }

    /**
     * Inserts [value] at the selection or replaces the selection.
     *
     * Insertion is clipped to [maxLength] without splitting a code point.
     *
     * @return whether the text or selection changed.
     * @since 1.0.0
     */
    public fun insert(value: String): Boolean {
        val start = selectionValue.start
        val end = selectionValue.end
        val available = maxLength - (Caret.count(textValue) - Caret.count(textValue, start, end))
        val inserted = Caret.take(value, available)
        if (inserted.isEmpty() && start == end) {
            return false
        }
        if (inserted.isEmpty() && value.isNotEmpty() && available <= 0) {
            return false
        }
        val typing =
            start == end &&
                Caret.count(inserted) == 1 &&
                inserted != "\n" &&
                inserted != "\r"
        val nextText = textValue.substring(0, start) + inserted + textValue.substring(end)
        val nextCaret = start + inserted.length
        return edit(nextText, TextSelection(nextCaret, nextCaret), if (typing) EditKind.TYPING else EditKind.OTHER)
    }

    /**
     * Replaces the selection with [value] and records one undo entry.
     *
     * @return whether the text changed.
     * @since 1.0.0
     */
    public fun replaceSelection(value: String): Boolean {
        val start = selectionValue.start
        val end = selectionValue.end
        val available = maxLength - (Caret.count(textValue) - Caret.count(textValue, start, end))
        val inserted = Caret.take(value, available)
        if (inserted.isEmpty() && start == end) {
            return false
        }
        if (inserted.isEmpty() && value.isNotEmpty() && available <= 0) {
            return false
        }
        val nextText = textValue.substring(0, start) + inserted + textValue.substring(end)
        val nextCaret = start + inserted.length
        return edit(nextText, TextSelection(nextCaret, nextCaret), EditKind.OTHER)
    }

    /** Deletes [count] code points before the caret or the selection. @since 1.0.0 */
    public fun deleteBefore(count: Int = 1): Boolean {
        if (count <= 0) {
            return false
        }
        if (!selectionValue.isCollapsed) {
            return deleteRange(selectionValue.start, selectionValue.end)
        }
        val start = Caret.advance(textValue, caret, -count)
        return start < caret && deleteRange(start, caret)
    }

    /** Deletes [count] code points after the caret or the selection. @since 1.0.0 */
    public fun deleteAfter(count: Int = 1): Boolean {
        if (count <= 0) {
            return false
        }
        if (!selectionValue.isCollapsed) {
            return deleteRange(selectionValue.start, selectionValue.end)
        }
        val end = Caret.advance(textValue, caret, count)
        return end > caret && deleteRange(caret, end)
    }

    /** Deletes from the caret to the previous word boundary. @since 1.0.0 */
    public fun deleteWordBefore(): Boolean {
        if (!selectionValue.isCollapsed) {
            return deleteRange(selectionValue.start, selectionValue.end)
        }
        val start = WordBoundary.previous(textValue, caret)
        return start < caret && deleteRange(start, caret)
    }

    /** Deletes from the caret to the next word boundary. @since 1.0.0 */
    public fun deleteWordAfter(): Boolean {
        if (!selectionValue.isCollapsed) {
            return deleteRange(selectionValue.start, selectionValue.end)
        }
        val end = WordBoundary.next(textValue, caret)
        return end > caret && deleteRange(caret, end)
    }

    /** Selects the whole text. @since 1.0.0 */
    public fun selectAll() {
        selection = TextSelection(0, textValue.length)
    }

    /** Copies the selection to [clipboard]. @return whether anything was copied. @since 1.0.0 */
    public fun copy(clipboard: Clipboard): Boolean {
        if (selectionValue.isCollapsed) {
            return false
        }
        clipboard.setText(selectedText)
        return true
    }

    /** Cuts the selection to [clipboard]. @return whether anything was cut. @since 1.0.0 */
    public fun cut(clipboard: Clipboard): Boolean {
        if (selectionValue.isCollapsed) {
            return false
        }
        clipboard.setText(selectedText)
        return deleteRange(selectionValue.start, selectionValue.end)
    }

    /** Pastes [clipboard] text over the selection. @return whether the text changed. @since 1.0.0 */
    public fun paste(clipboard: Clipboard): Boolean {
        val value = clipboard.getText() ?: return false
        if (value.isEmpty()) {
            return false
        }
        return replaceSelection(value)
    }

    /**
     * Moves the caret by [deltaCodePoints].
     *
     * @return whether the selection changed.
     * @since 1.0.0
     */
    public fun moveCaret(
        deltaCodePoints: Int,
        extendSelection: Boolean = false,
    ): Boolean {
        if (deltaCodePoints == 0) {
            return false
        }
        val target = Caret.advance(textValue, caret, deltaCodePoints)
        return moveTo(target, extendSelection)
    }

    /**
     * Moves by one ASCII word boundary in [direction].
     *
     * @return whether the selection changed.
     * @since 1.0.0
     */
    public fun moveWord(
        direction: Int,
        extendSelection: Boolean = false,
    ): Boolean {
        if (direction == 0) {
            return false
        }
        val target =
            if (direction < 0) {
                WordBoundary.previous(textValue, caret)
            } else {
                WordBoundary.next(textValue, caret)
            }
        return moveTo(target, extendSelection)
    }

    /** Moves to the line start. @since 1.0.0 */
    public fun home(extendSelection: Boolean = false): Boolean = moveTo(lineStart(caret), extendSelection)

    /** Moves to the line end. @since 1.0.0 */
    public fun end(extendSelection: Boolean = false): Boolean = moveTo(lineEnd(caret), extendSelection)

    /** Moves to the text start. @since 1.0.0 */
    public fun textHome(extendSelection: Boolean = false): Boolean = moveTo(0, extendSelection)

    /** Moves to the text end. @since 1.0.0 */
    public fun textEnd(extendSelection: Boolean = false): Boolean = moveTo(textValue.length, extendSelection)

    /** Moves up one logical line. @since 1.0.0 */
    public fun lineUp(extendSelection: Boolean = false): Boolean = moveTo(verticalTarget(-1), extendSelection)

    /** Moves down one logical line. @since 1.0.0 */
    public fun lineDown(extendSelection: Boolean = false): Boolean = moveTo(verticalTarget(1), extendSelection)

    /** Undoes the most recent edit. @return whether an edit was restored. @since 1.0.0 */
    public fun undo(): Boolean {
        val current = TextEdit(textValue, selectionValue)
        val previous = history.undo(current) ?: return false
        breakCoalescing()
        applySnapshot(previous)
        return true
    }

    /** Redoes the most recently undone edit. @return whether an edit was restored. @since 1.0.0 */
    public fun redo(): Boolean {
        val current = TextEdit(textValue, selectionValue)
        val next = history.redo(current) ?: return false
        breakCoalescing()
        applySnapshot(next)
        return true
    }

    /** Registers a text-change listener. */
    internal fun addTextListener(listener: () -> Unit) {
        textListeners.add(listener)
    }

    /** Removes a text-change listener. */
    internal fun removeTextListener(listener: () -> Unit) {
        textListeners.remove(listener)
    }

    /** Registers a selection-change listener. */
    internal fun addSelectionListener(listener: () -> Unit) {
        selectionListeners.add(listener)
    }

    /** Removes a selection-change listener. */
    internal fun removeSelectionListener(listener: () -> Unit) {
        selectionListeners.remove(listener)
    }

    private fun deleteRange(
        start: Int,
        end: Int,
    ): Boolean {
        if (start >= end) {
            return false
        }
        val nextText = textValue.substring(0, start) + textValue.substring(end)
        val nextCaret = Caret.snap(nextText, start)
        return edit(nextText, TextSelection(nextCaret, nextCaret), EditKind.OTHER)
    }

    private fun edit(
        nextText: String,
        nextSelection: TextSelection,
        kind: EditKind,
    ): Boolean {
        if (nextText == textValue && nextSelection == selectionValue) {
            return false
        }
        val before = TextEdit(textValue, selectionValue)
        val now = clock()
        val coalesce =
            kind == EditKind.TYPING &&
                lastKind == EditKind.TYPING &&
                now >= lastEditNanos &&
                now - lastEditNanos <= COALESCE_NANOS &&
                lastAfterCaret == selectionValue.focus &&
                selectionValue.isCollapsed &&
                before.selection.isCollapsed
        if (!coalesce) {
            history.record(before)
        }
        textValue = nextText
        selectionValue = nextSelection
        lastKind = kind
        lastEditNanos = now
        lastAfterCaret = nextSelection.focus
        bump()
        notifyTextChanged()
        notifySelectionChanged()
        return true
    }

    private fun applyEdit(
        nextText: String,
        nextSelection: TextSelection,
        kind: EditKind,
    ) {
        if (nextText == textValue && nextSelection == selectionValue) {
            return
        }
        val before = TextEdit(textValue, selectionValue)
        history.record(before)
        textValue = nextText
        selectionValue = nextSelection
        lastKind = kind
        lastEditNanos = clock()
        lastAfterCaret = nextSelection.focus
        bump()
        notifyTextChanged()
        notifySelectionChanged()
    }

    private fun applySnapshot(snapshot: TextEdit) {
        val textChanged = snapshot.text != textValue
        textValue = snapshot.text
        selectionValue =
            TextSelection(
                Caret.snap(textValue, snapshot.selection.anchor),
                Caret.snap(textValue, snapshot.selection.focus),
            )
        bump()
        if (textChanged) {
            notifyTextChanged()
        }
        notifySelectionChanged()
    }

    private fun moveTo(
        target: Int,
        extendSelection: Boolean,
    ): Boolean {
        val nextFocus = Caret.snap(textValue, target)
        val next =
            if (extendSelection) {
                TextSelection(selectionValue.anchor, nextFocus)
            } else {
                TextSelection(nextFocus, nextFocus)
            }
        if (next == selectionValue) {
            return false
        }
        selectionValue = next
        breakCoalescing()
        bump()
        notifySelectionChanged()
        return true
    }

    private fun verticalTarget(direction: Int): Int {
        val start = lineStart(caret)
        val end = lineEnd(caret)
        val column = Caret.count(textValue, start, caret)
        return when {
            direction < 0 -> {
                if (start == 0) {
                    caret
                } else {
                    val previousEnd = start - 1
                    val previousStart = lineStart(previousEnd)
                    val targetColumn = min(column, Caret.count(textValue, previousStart, previousEnd))
                    Caret.advance(textValue, previousStart, targetColumn)
                }
            }

            direction > 0 -> {
                if (end >= textValue.length) {
                    caret
                } else {
                    val nextStart = end + 1
                    val nextEnd = lineEnd(nextStart)
                    val targetColumn = min(column, Caret.count(textValue, nextStart, nextEnd))
                    Caret.advance(textValue, nextStart, targetColumn)
                }
            }

            else -> {
                caret
            }
        }
    }

    private fun lineStart(index: Int): Int {
        val position = index.coerceIn(0, textValue.length)
        if (position == 0) {
            return 0
        }
        val newline = textValue.lastIndexOf('\n', position - 1)
        return if (newline < 0) 0 else newline + 1
    }

    private fun lineEnd(index: Int): Int {
        val position = index.coerceIn(0, textValue.length)
        val newline = textValue.indexOf('\n', position)
        return if (newline < 0) textValue.length else newline
    }

    private fun breakCoalescing() {
        lastKind = EditKind.OTHER
        lastEditNanos = Long.MIN_VALUE
        lastAfterCaret = -1
    }

    private fun bump() {
        revisionValue++
    }

    private fun notifyTextChanged() {
        var index = 0
        while (index < textListeners.size) {
            textListeners[index].invoke()
            index++
        }
    }

    private fun notifySelectionChanged() {
        var index = 0
        while (index < selectionListeners.size) {
            selectionListeners[index].invoke()
            index++
        }
    }

    private companion object {
        private const val COALESCE_NANOS: Long = 500_000_000L
    }
}
