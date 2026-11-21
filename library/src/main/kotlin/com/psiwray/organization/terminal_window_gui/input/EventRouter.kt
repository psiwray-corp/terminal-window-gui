package com.psiwray.organization.terminal_window_gui.input

import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.KeyAction
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window.input.MouseButton
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.command.Keymap
import com.psiwray.organization.terminal_window_gui.internal.ScreenPoint
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.widget.MenuPopup
import com.psiwray.organization.terminal_window_gui.widget.Widget
import kotlin.math.abs

/**
 * Routes input through the widget tree.
 *
 * Pointer events are converted to receiver-local coordinates at each hop.
 * Capture runs root-to-target on [Widget.onCaptureEvent], followed by the
 * target and bubbling on [Widget.onEvent]. A `true` return stops routing.
 *
 * @property root Root widget.
 * @property focus Keyboard focus manager.
 * @property overlays Overlay stack painted after the root, if any.
 * @property hover Hover path tracker.
 * @property pointerCapture Pointer capture tracker.
 * @property keymap Optional command bindings checked after widget handling.
 * @since 1.0.0
 */
public class EventRouter(
    public val root: Widget,
    public val focus: FocusManager = FocusManager(root),
    public val overlays: OverlayHost? = null,
    public val hover: HoverState = HoverState(),
    public val pointerCapture: PointerCapture = PointerCapture(),
    public val keymap: Keymap? = null,
    private val clock: () -> Long = System::nanoTime,
) {
    /** Active screen scope for [keymap]. @since 1.0.0 */
    public var screenScope: String? = null

    /** Alias for [screenScope]. @since 1.0.0 */
    public var scope: String?
        get() = screenScope
        set(value) {
            screenScope = value
        }

    private var pointerKnown: Boolean = false
    private var lastPointerScreenX: Int = 0
    private var lastPointerScreenY: Int = 0
    private var lastClickCount: Int = 1
    private val clicks: ClickTracker = ClickTracker()
    private val implicitCapture: MutableMap<Class<*>, Boolean> = HashMap()

    /**
     * Creates a router with a fresh focus manager and an overlay host.
     *
     * @since 1.0.0
     */
    public constructor(
        root: Widget,
        overlays: OverlayHost,
    ) : this(root, FocusManager(root), overlays)

    /**
     * Dispatches [event]. Returns whether a handler consumed it.
     *
     * @since 1.0.0
     */
    public fun dispatch(event: GuiEvent): Boolean =
        when (event) {
            is GuiKeyEvent -> dispatchKey(event)
            is GuiCharEvent -> dispatchText(event)
            is GuiPointerMoveEvent -> dispatchMove(event)
            is GuiPointerButtonEvent -> dispatchButton(event)
            is GuiScrollEvent -> dispatchScroll(event)
            is GuiFocusEvent -> dispatchFocus(event)
            is GuiResizeEvent -> false
        }

    private fun dispatchKey(event: GuiKeyEvent): Boolean {
        if (event.key == Key.ESCAPE && event.action != KeyAction.RELEASE) {
            val top = overlays?.topmost?.widget
            if (top is MenuPopup) {
                top.dismissRoot()
                return true
            }
            if (overlays?.dismissEscape() == true) {
                return true
            }
        }
        if (dispatchMnemonic(event)) {
            return true
        }
        if (overlays?.hasModal != true && event.action != KeyAction.RELEASE && dispatchGlobalKey(root, event)) {
            return true
        }
        val modal = overlays?.topmostModal
        val current = focus.focused
        val target =
            when {
                modal != null && current != null && FocusTraversal.isWithin(current, modal.widget) -> current
                modal != null -> modal.widget
                current != null -> current
                else -> root
            }
        if (dispatchPath(target, event, pointerBase(target), null)) {
            return true
        }
        if (event.action == KeyAction.RELEASE) {
            return false
        }
        if (event.key == Key.TAB &&
            !event.repeat &&
            !event.modifiers.has(Modifiers.CONTROL) &&
            !event.modifiers.has(Modifiers.ALT) &&
            !event.modifiers.has(Modifiers.SUPER)
        ) {
            val moved =
                if (event.modifiers.has(Modifiers.SHIFT)) {
                    focus.focusPrevious()
                } else {
                    focus.focusNext()
                }
            if (moved) {
                return true
            }
        }
        val map = keymap ?: return false
        return map.execute(KeyChord(event.key, event.modifiers), screenScope)
    }

    private fun dispatchMnemonic(event: GuiKeyEvent): Boolean {
        if (event.action != KeyAction.PRESS || event.repeat || !event.modifiers.has(Modifiers.ALT)) {
            return false
        }
        if (event.modifiers.has(Modifiers.CONTROL) || event.modifiers.has(Modifiers.SUPER)) {
            return false
        }
        val codePoint = MnemonicKeys.codePoint(event.key)
        if (codePoint == 0) {
            return false
        }
        return activateMnemonic(mnemonicScope(), codePoint)
    }

    private fun mnemonicScope(): Widget {
        val modal = overlays?.topmostModal
        if (modal != null) {
            return modal.widget
        }
        val current = focus.focused ?: return root
        var scope: Widget? = current
        while (scope != null) {
            if (scope.focusScope == FocusScope.TRAP) {
                return scope
            }
            scope = scope.parent
        }
        var top = current
        while (true) {
            val next = top.parent ?: break
            top = next
        }
        return top
    }

    private fun activateMnemonic(
        widget: Widget,
        codePoint: Int,
    ): Boolean {
        if (!widget.enabled || !widget.visible || widget.bounds.isEmpty || widget.focusScope == FocusScope.SKIP) {
            return false
        }
        if (widget.matchesMnemonic(codePoint) && widget.activateMnemonic(codePoint)) {
            return true
        }
        for (child in widget.traversalChildren()) {
            if (activateMnemonic(child, codePoint)) {
                return true
            }
        }
        return false
    }

    private fun dispatchGlobalKey(
        widget: Widget,
        event: GuiKeyEvent,
    ): Boolean {
        if (!widget.enabled || !widget.visible || widget.bounds.isEmpty) {
            return false
        }
        if (widget.onGlobalKey(event)) {
            return true
        }
        for (child in widget.traversalChildren()) {
            if (dispatchGlobalKey(child, event)) {
                return true
            }
        }
        return false
    }

    private fun dispatchText(event: GuiCharEvent): Boolean {
        val modal = overlays?.topmostModal
        val current = focus.focused
        val target =
            when {
                modal != null && current != null && FocusTraversal.isWithin(current, modal.widget) -> current
                modal != null -> modal.widget
                current != null -> current
                else -> root
            }
        return dispatchPath(target, event, pointerBase(target), null)
    }

    private fun dispatchMove(event: GuiPointerMoveEvent): Boolean {
        pointerKnown = true
        lastPointerScreenX = event.screenX
        lastPointerScreenY = event.screenY
        clicks.moved(event.screenX, event.screenY)
        val capture = activeCapture()
        val hit = if (capture == null) pointerTarget(event.screenX, event.screenY) else null
        val target = capture ?: hit?.target
        if (target == null) {
            hover.clear()
            return false
        }
        hover.update(target)
        val base = hit?.base ?: pointerBase(target)
        return dispatchPath(target, event, base, ScreenPoint(event.screenX, event.screenY))
    }

    private fun dispatchButton(event: GuiPointerButtonEvent): Boolean {
        pointerKnown = true
        lastPointerScreenX = event.screenX
        lastPointerScreenY = event.screenY
        val press = event.action == KeyAction.PRESS
        if (press) {
            clicks.moved(event.screenX, event.screenY)
        }
        var capture = activeCapture()
        if (press) {
            if (overlays?.dismissOutside(event.screenX, event.screenY) == true) {
                pointerCapture.clear()
                clicks.reset()
                return true
            }
            if (capture == null) {
                val hit = pointerTarget(event.screenX, event.screenY)
                if (hit != null) {
                    focusHit(hit.target)
                    if (hit.target.wantsPointerCapture) {
                        pointerCapture.capture(hit.target, event.button)
                        capture = hit.target
                    }
                }
            }
            lastClickCount = clicks.press(event.button, event.screenX, event.screenY, clock())
        }
        val hit = if (capture == null) pointerTarget(event.screenX, event.screenY) else null
        val target = capture ?: hit?.target
        if (target == null) {
            if (!press) {
                pointerCapture.clear()
            }
            return false
        }
        val clickCount = if (press) lastClickCount else lastClickCount.coerceIn(1, 3)
        val localEvent = event.copy(clickCount = clickCount)
        val base = hit?.base ?: pointerBase(target)
        val consumed = dispatchPath(target, localEvent, base, ScreenPoint(event.screenX, event.screenY))
        if (!press) {
            pointerCapture.release()
        }
        return consumed
    }

    private fun dispatchScroll(event: GuiScrollEvent): Boolean {
        val screenX: Int
        val screenY: Int
        if (pointerKnown) {
            screenX = lastPointerScreenX
            screenY = lastPointerScreenY
        } else {
            val origin = root.screenOriginPoint()
            screenX = origin.x + event.localX
            screenY = origin.y + event.localY
        }
        val hit = pointerTarget(screenX, screenY)
        if (hit == null && overlays?.hasModal == true) {
            return false
        }
        val focused = focus.focused
        val target = hit?.target ?: focused ?: root
        val base = hit?.base ?: pointerBase(target)
        return dispatchPath(target, event, base, ScreenPoint(screenX, screenY))
    }

    private fun dispatchFocus(event: GuiFocusEvent): Boolean {
        if (!event.focused) {
            pointerCapture.clear()
            hover.clear()
            clicks.reset()
            pointerKnown = false
        }
        return false
    }

    private fun activeCapture(): Widget? {
        val captured = pointerCapture.captured ?: return null
        if (!captured.visible || !captured.enabled || !captured.isAttached) {
            pointerCapture.clear()
            return null
        }
        return captured
    }

    private fun focusHit(target: Widget) {
        var current: Widget? = target
        while (current != null) {
            if (FocusTraversal.isEligible(current)) {
                focus.focus(current)
                return
            }
            current = current.parent
        }
        val descendant = FocusTraversal.order(target).firstOrNull()
        if (descendant != null) {
            focus.focus(descendant)
        }
    }

    private fun pointerTarget(
        screenX: Int,
        screenY: Int,
    ): Hit? {
        val host = overlays
        if (host != null) {
            val overlay = host.hitTest(screenX, screenY)
            if (overlay != null) {
                val target = host.targetAt(overlay, screenX, screenY)
                return Hit(target, IntSize(overlay.screenRect.x, overlay.screenRect.y))
            }
            if (host.hasModal) {
                return null
            }
        }
        val origin = root.screenOriginPoint()
        val target = root.hitTest(screenX - origin.x, screenY - origin.y)
        return if (target == null) null else Hit(target, IntSize.ZERO)
    }

    private fun dispatchPath(
        target: Widget,
        event: GuiEvent,
        base: IntSize,
        screen: ScreenPoint?,
    ): Boolean {
        val path = pathTo(target)
        var index = 0
        val limit = path.size - 1
        while (index < limit) {
            val widget = path[index]
            if (widget.enabled && widget.visible) {
                val localEvent = eventFor(widget, event, base, screen)
                if (widget.onCaptureEvent(localEvent)) {
                    return true
                }
                if (event is GuiPointerMoveEvent || event is GuiPointerButtonEvent) {
                    if (usesImplicitCapture(widget) && widget.onEvent(localEvent)) {
                        return true
                    }
                }
            }
            index++
        }
        if (limit >= 0) {
            val widget = path[limit]
            if (widget.enabled && widget.visible) {
                if (widget.onEvent(eventFor(widget, event, base, screen))) {
                    return true
                }
            }
        }
        index = limit - 1
        while (index >= 0) {
            val widget = path[index]
            if (widget.enabled && widget.visible) {
                if (widget.onEvent(eventFor(widget, event, base, screen))) {
                    return true
                }
            }
            index--
        }
        return false
    }

    private fun usesImplicitCapture(widget: Widget): Boolean =
        implicitCapture.getOrPut(widget.javaClass) {
            widget.javaClass
                .getMethod("onCaptureEvent", GuiEvent::class.java)
                .declaringClass == Widget::class.java
        }

    private fun eventFor(
        widget: Widget,
        event: GuiEvent,
        base: IntSize,
        screen: ScreenPoint?,
    ): GuiEvent {
        if (screen == null) {
            return event
        }
        val origin = widget.screenOriginPoint()
        val localX = screen.x - base.width - origin.x
        val localY = screen.y - base.height - origin.y
        return when (event) {
            is GuiPointerMoveEvent -> event.copy(localX = localX, localY = localY)
            is GuiPointerButtonEvent -> event.copy(localX = localX, localY = localY)
            is GuiScrollEvent -> event.copy(localX = localX, localY = localY)
            else -> event
        }
    }

    private fun pointerBase(widget: Widget): IntSize {
        val entry = overlayEntryFor(widget) ?: return IntSize.ZERO
        return IntSize(entry.screenRect.x, entry.screenRect.y)
    }

    private fun overlayEntryFor(widget: Widget): OverlayEntry? {
        val host = overlays ?: return null
        for (entry in host.entries) {
            if (entry.widget === widget || entry.widget.isAncestorOf(widget)) {
                return entry
            }
        }
        return null
    }

    private fun pathTo(target: Widget): List<Widget> {
        val path = ArrayList<Widget>()
        var current: Widget? = target
        while (current != null) {
            path.add(current)
            current = current.parent
        }
        path.reverse()
        return path
    }

    private class Hit(
        val target: Widget,
        val base: IntSize,
    )

    private class ClickTracker {
        private var button: MouseButton? = null
        private var lastX: Int = 0
        private var lastY: Int = 0
        private var lastNanos: Long = 0
        private var count: Int = 1

        fun moved(
            x: Int,
            y: Int,
        ) {
            if (button == null) {
                return
            }
            if (abs(x - lastX) > CLICK_TOLERANCE || abs(y - lastY) > CLICK_TOLERANCE) {
                reset()
            }
        }

        fun press(
            button: MouseButton,
            x: Int,
            y: Int,
            nowNanos: Long,
        ): Int {
            val same =
                button == this.button &&
                    nowNanos >= lastNanos &&
                    nowNanos - lastNanos <= CLICK_THRESHOLD_NANOS &&
                    abs(x - lastX) <= CLICK_TOLERANCE &&
                    abs(y - lastY) <= CLICK_TOLERANCE
            count = if (same) (if (count >= 3) 1 else count + 1) else 1
            this.button = button
            lastX = x
            lastY = y
            lastNanos = nowNanos
            return count
        }

        fun reset() {
            button = null
            count = 1
        }

        private companion object {
            private const val CLICK_THRESHOLD_NANOS: Long = 400_000_000L
            private const val CLICK_TOLERANCE: Int = 1
        }
    }
}
