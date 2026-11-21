package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.GridSize
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.command.Keymap

/**
 * One navigable screen: a stable key, title, content, optional keymap, and
 * lifecycle callbacks.
 *
 * A screen does not own a native window. [ScreenHost] keeps screens in a stack
 * and calls [onEnter] when the screen becomes active, [onExit] when it stops
 * being active, [onResize] before its content is laid out at a new grid size,
 * and [onBack] for Esc when no overlay consumed it.
 *
 * @property key Stable navigation key.
 * @property title Advisory title for the caller's window.
 * @property content Screen content widget.
 * @property keymap Optional screen-scoped commands installed while active.
 * @property onEnter Called when the screen becomes active.
 * @property onExit Called when the screen stops being active.
 * @property onResize Called before layout with the new grid size.
 * @property onBack Handles Esc; return `true` to consume and pop.
 * @since 1.0.0
 */
public class Screen(
    public val key: Any,
    title: String,
    content: Widget,
    public var keymap: Keymap? = null,
    public var onEnter: () -> Unit = {},
    public var onExit: () -> Unit = {},
    public var onResize: (GridSize) -> Unit = {},
    public var onBack: () -> Boolean = { false },
) {
    private var titleValue: String = title
    private var contentValue: Widget = content

    /** Advisory window title. @since 1.0.0 */
    public var title: String
        get() = titleValue
        set(value) {
            titleValue = value
        }

    /** Screen content. @since 1.0.0 */
    public var content: Widget
        get() = contentValue
        set(value) {
            if (contentValue === value) {
                return
            }
            contentValue = value
        }

    /** Creates a screen with the content as its key. @since 1.0.0 */
    public constructor(
        content: Widget,
        title: String = "",
        keymap: Keymap? = null,
    ) : this(title, title, content, keymap)

    /** Creates a screen from a chord-to-command map. @since 1.0.0 */
    public constructor(
        key: Any,
        title: String,
        content: Widget,
        keymap: Map<KeyChord, Command>,
        onEnter: () -> Unit = {},
        onExit: () -> Unit = {},
        onResize: (GridSize) -> Unit = {},
        onBack: () -> Boolean = { false },
    ) : this(
        key = key,
        title = title,
        content = content,
        keymap = keymap.toKeymap(),
        onEnter = onEnter,
        onExit = onExit,
        onResize = onResize,
        onBack = onBack,
    )

    private companion object {
        private fun Map<KeyChord, Command>.toKeymap(): Keymap {
            val result = Keymap()
            for ((chord, command) in this) {
                result.bind(chord, command)
            }
            return result
        }
    }
}
