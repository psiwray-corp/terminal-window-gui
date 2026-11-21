package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.PaintContext

/**
 * Menu, content, and status stack.
 *
 * A present menu bar occupies one row, the content receives the remaining
 * rows, and a present status bar occupies the last row. The shell never owns a
 * window, a frame loop, or a title. Callers that need a native title read
 * [ScreenHost.currentTitle] and apply it themselves.
 *
 * @property menuBar Optional one-row menu.
 * @property content Main content.
 * @property statusBar Optional one-row status.
 * @since 1.0.0
 */
public class AppShell(
    menuBar: Widget? = null,
    content: Widget,
    statusBar: Widget? = null,
) : ContainerWidget() {
    private var menuBarValue: Widget? = null
    private var contentValue: Widget = content
    private var statusBarValue: Widget? = null

    /** Children in paint order: menu, content, status. @since 1.0.0 */
    public val children: List<Widget> get() = java.util.Collections.unmodifiableList(childList)

    /** Optional menu bar. @since 1.0.0 */
    public var menuBar: Widget?
        get() = menuBarValue
        set(value) {
            if (menuBarValue === value) {
                return
            }
            menuBarValue?.let { drop(it) }
            menuBarValue = value
            value?.let { adopt(it, 0) }
            invalidateLayout()
        }

    /** Main content. @since 1.0.0 */
    public var content: Widget
        get() = contentValue
        set(value) {
            if (contentValue === value) {
                return
            }
            drop(contentValue)
            contentValue = value
            val index = if (menuBarValue != null) 1 else 0
            adopt(value, index.coerceAtMost(childList.size))
            invalidateLayout()
        }

    /** Optional status bar. @since 1.0.0 */
    public var statusBar: Widget?
        get() = statusBarValue
        set(value) {
            if (statusBarValue === value) {
                return
            }
            statusBarValue?.let { drop(it) }
            statusBarValue = value
            value?.let { adopt(it) }
            invalidateLayout()
        }

    /** Alias for [menuBar]. @since 1.0.0 */
    public var menu: Widget?
        get() = menuBar
        set(value) {
            menuBar = value
        }

    /** Alias for [statusBar]. @since 1.0.0 */
    public var status: Widget?
        get() = statusBar
        set(value) {
            statusBar = value
        }

    init {
        menuBarValue = menuBar
        menuBar?.let { adopt(it) }
        adopt(content)
        statusBarValue = statusBar
        statusBar?.let { adopt(it) }
    }

    override fun measure(constraints: Constraints): IntSize {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val contentSize =
            contentValue.measured(
                Constraints(0, width, 0, if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE),
            )
        val widthResult = if (constraints.hasBoundedWidth) constraints.maxWidth else contentSize.width.coerceAtLeast(1)
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                contentSize.height +
                    (if (menuBarValue != null) 1 else 0) +
                    (if (statusBarValue != null) 1 else 0)
            }
        return constraints.constrain(IntSize(widthResult.coerceAtLeast(1), height.coerceAtLeast(1)))
    }

    override fun arrangeChildren() {
        val width = contentRect.width
        val height = contentRect.height
        val menuRows = if (menuBarValue != null && height >= 1) 1 else 0
        val statusRows = if (statusBarValue != null && height - menuRows >= 1) 1 else 0
        val contentRows = (height - menuRows - statusRows).coerceAtLeast(0)
        menuBarValue?.let {
            it.arrange(if (menuRows == 1) Rect(0, 0, width, 1) else Rect.EMPTY)
        }
        contentValue.arrange(Rect(0, menuRows, width, contentRows))
        statusBarValue?.let {
            it.arrange(if (statusRows == 1) Rect(0, height - 1, width, 1) else Rect.EMPTY)
        }
    }

    override fun paint(context: PaintContext) {
        if (bounds.isEmpty) {
            return
        }
        context.fill(Rect(0, 0, bounds.width, bounds.height), context.theme.text, context.theme.surface)
    }
}
