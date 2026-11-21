package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window_gui.GridSize
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.command.Keymap
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.GuiKeyEvent
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import java.util.Collections

/**
 * Stack host for [Screen] navigation.
 *
 * Only the current screen content is attached, measured, painted, hit-tested,
 * and focusable. Push saves focus and exits the previous screen; pop exits the
 * top screen and restores the previous screen and its saved focus. The host
 * reports [currentTitle] for a caller-owned native title and executes the
 * current screen's [Screen.keymap] while that screen is active.
 *
 * Esc is offered to [Screen.onBack] during capture only after the event router
 * has given overlays their chance to dismiss. A rejected back does not pop.
 * Resize callbacks run before the active content is arranged at the new size.
 *
 * @property screens Initial screens available to [initialKey].
 * @property initialKey Screen selected on construction, or the first screen.
 * @since 1.0.0
 */
public class ScreenHost(
    screens: List<Screen> = emptyList(),
    public val initialKey: Any? = null,
) : ContainerWidget() {
    private val screenList: List<Screen> = screens.toList()
    private val stackValue: MutableList<Screen> = ArrayList()
    private val savedFocus: MutableMap<Screen, Widget> = LinkedHashMap()
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0
    private var activeContent: Widget? = null
    private var attachedAndEntered: Boolean = false

    /** Current screen, or `null` when there are no screens. @since 1.0.0 */
    public val current: Screen? get() = stackValue.lastOrNull()

    /** Screen stack from root to current. @since 1.0.0 */
    public val stack: List<Screen> get() = Collections.unmodifiableList(stackValue)

    /** Advisory title of [current], or `null`. @since 1.0.0 */
    public val currentTitle: String? get() = current?.title

    /** Keymap installed by [current], or `null`. @since 1.0.0 */
    public val currentKeymap: Keymap? get() = current?.keymap

    /** Initial screens supplied to the constructor. @since 1.0.0 */
    public val screens: List<Screen> get() = screenList

    /** Content of [current], or `null`. @since 1.0.0 */
    public val content: Widget? get() = activeContent

    init {
        val initial =
            initialKey?.let { key -> screenList.firstOrNull { it.key == key } }
                ?: screenList.firstOrNull()
        if (initial != null) {
            stackValue.add(initial)
            adopt(initial.content)
            activeContent = initial.content
        }
    }

    /**
     * Pushes [screen] on top of the current screen.
     *
     * @since 1.0.0
     */
    public fun push(screen: Screen) {
        if (screen === current) {
            return
        }
        leaveCurrent(exit = true)
        stackValue.add(screen)
        attachScreen(screen)
        screen.onEnter()
        notifyResize(screen)
        arrangeCurrent()
        restoreFocus(screen)
    }

    /**
     * Pops the top screen when another screen remains.
     *
     * @return whether a screen was popped.
     * @since 1.0.0
     */
    public fun pop(): Boolean {
        if (stackValue.size <= 1) {
            return false
        }
        val top = stackValue.removeAt(stackValue.lastIndex)
        top.onExit()
        detachContent()
        val next = stackValue.last()
        attachScreen(next)
        next.onEnter()
        notifyResize(next)
        arrangeCurrent()
        restoreFocus(next)
        return true
    }

    /**
     * Replaces the current screen, or pushes [screen] when the stack is empty.
     *
     * @since 1.0.0
     */
    public fun replace(screen: Screen) {
        if (stackValue.isEmpty()) {
            stackValue.add(screen)
            attachScreen(screen)
            screen.onEnter()
            notifyResize(screen)
            arrangeCurrent()
            return
        }
        if (screen === current && screen.content === activeContent) {
            return
        }
        val old = stackValue.removeAt(stackValue.lastIndex)
        old.onExit()
        detachContent()
        stackValue.add(screen)
        attachScreen(screen)
        screen.onEnter()
        notifyResize(screen)
        arrangeCurrent()
        restoreFocus(screen)
    }

    override fun onAttach() {
        if (!attachedAndEntered) {
            if (stackValue.size == 1) {
                current?.onEnter()
            }
            attachedAndEntered = true
        }
    }

    override fun onBoundsChanged(
        old: Rect,
        new: Rect,
    ) {
        if (new.isEmpty) {
            return
        }
        if (new.width != lastWidth || new.height != lastHeight) {
            lastWidth = new.width
            lastHeight = new.height
            current?.onResize(GridSize(new.width, new.height))
        }
    }

    override fun measure(constraints: Constraints): IntSize {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val contentSize =
            activeContent?.measured(
                Constraints(0, width, 0, if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE),
            )
        val preferred =
            if (contentSize == null) {
                IntSize(1, 1)
            } else {
                IntSize(
                    if (constraints.hasBoundedWidth) constraints.maxWidth else contentSize.width,
                    if (constraints.hasBoundedHeight) constraints.maxHeight else contentSize.height,
                )
            }
        return constraints.constrain(IntSize(preferred.width.coerceAtLeast(1), preferred.height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val currentScreen = current
        if (currentScreen != null && currentScreen.content !== activeContent) {
            detachContent()
            attachScreen(currentScreen)
        }
        activeContent?.arrange(Rect(0, 0, contentRect.width, contentRect.height))
    }

    override fun paint(context: PaintContext) {}

    override fun onEvent(event: GuiEvent): Boolean = handleKey(event)

    override fun onCaptureEvent(event: GuiEvent): Boolean = handleKey(event)

    private fun handleKey(event: GuiEvent): Boolean {
        if (event !is GuiKeyEvent || event.action == KeyAction.RELEASE) {
            return false
        }
        val screen = current ?: return false
        if (event.key == Key.ESCAPE) {
            if (screen.onBack()) {
                if (stackValue.size > 1) {
                    pop()
                }
                return true
            }
            return false
        }
        val map = screen.keymap ?: return false
        return map.execute(KeyChord(event.key, event.modifiers))
    }

    private fun leaveCurrent(exit: Boolean) {
        val previous = current ?: return
        val manager = guiContext?.focus
        val focused = manager?.focused
        val contentWidget = activeContent
        if (focused != null && contentWidget != null &&
            (focused === contentWidget || contentWidget.isAncestorOf(focused))
        ) {
            savedFocus[previous] = focused
            manager.clearFocus()
        }
        if (exit) {
            previous.onExit()
        }
        detachContent()
    }

    private fun attachScreen(screen: Screen) {
        screen.content.visible = true
        activeContent = screen.content
        adopt(screen.content)
    }

    private fun detachContent() {
        val old = activeContent ?: return
        old.visible = false
        drop(old)
        activeContent = null
    }

    private fun arrangeCurrent() {
        if (bounds.isEmpty) {
            return
        }
        current?.content?.arrange(Rect(0, 0, contentRect.width, contentRect.height))
    }

    private fun notifyResize(screen: Screen) {
        if (bounds.isEmpty) {
            return
        }
        screen.onResize(GridSize(bounds.width, bounds.height))
    }

    private fun restoreFocus(screen: Screen) {
        val manager = guiContext?.focus ?: return
        val saved = savedFocus[screen]
        if (saved != null) {
            manager.restore(saved)
        } else {
            manager.clearFocus()
        }
    }
}
