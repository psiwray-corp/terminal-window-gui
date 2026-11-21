package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window.CursorShape
import com.psiwray.organization.terminal_window_gui.command.Keymap
import com.psiwray.organization.terminal_window_gui.input.Clipboard
import com.psiwray.organization.terminal_window_gui.input.EventRouter
import com.psiwray.organization.terminal_window_gui.input.FocusManager
import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import com.psiwray.organization.terminal_window_gui.input.HoverState
import com.psiwray.organization.terminal_window_gui.input.InMemoryClipboard
import com.psiwray.organization.terminal_window_gui.input.OverlayHost
import com.psiwray.organization.terminal_window_gui.input.Scheduler
import com.psiwray.organization.terminal_window_gui.internal.GuiContext
import com.psiwray.organization.terminal_window_gui.layout.Constraints
import com.psiwray.organization.terminal_window_gui.layout.IntSize
import com.psiwray.organization.terminal_window_gui.layout.Rect
import com.psiwray.organization.terminal_window_gui.paint.CellCanvas
import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.ResolvedShapeSet
import com.psiwray.organization.terminal_window_gui.style.Theme
import com.psiwray.organization.terminal_window_gui.widget.Widget

/**
 * Passive GUI surface driven by its caller.
 *
 * The surface owns no window, loop, thread, or timer. The caller resizes it to
 * the terminal matrix, feeds immutable [GuiEvent] values, advances the
 * scheduler, and paints into a caller-owned [CellCanvas]. Layout and paint are
 * lazy; dispatching never paints and painting never dispatches.
 *
 * A surface is single-thread-affine. The first non-zero [resize] is required
 * before [dispatch] or [paint].
 *
 * @since 1.0.0
 */
public class GuiSurface
    internal constructor(
        /** Root widget arranged at the surface origin. @since 1.0.0 */
        public val root: Widget,
        theme: Theme,
        shapes: ResolvedShapeSet,
        /** Clipboard shared with text widgets. @since 1.0.0 */
        public val clipboard: Clipboard,
        private val clock: () -> Long,
    ) {
        /** Current immutable theme. Use [setTheme] to replace it. @since 1.0.0 */
        public var theme: Theme = theme
            private set

        /** Shape catalog used by [paint]. Use [setShapes] to replace it. @since 1.0.0 */
        public var shapes: ResolvedShapeSet = shapes
            private set

        /** Current surface size in cells. Starts at zero before the first resize. @since 1.0.0 */
        public var size: GridSize = GridSize.ZERO
            private set

        /** Whether the widget tree needs a layout pass. @since 1.0.0 */
        public var needsLayout: Boolean = true
            private set

        /** Whether the surface needs a paint pass. @since 1.0.0 */
        public var needsPaint: Boolean = true
            private set

        /** Advisory cursor requested by the hovered widget, or `null`. @since 1.0.0 */
        public val pointerShape: CursorShape?
            get() {
                val path = hover.path
                var index = path.size - 1
                while (index >= 0) {
                    val shape = path[index].cursorShape
                    if (shape != null) {
                        return shape
                    }
                    index--
                }
                return null
            }

        /** Keyboard focus manager for the root and overlays. @since 1.0.0 */
        public val focus: FocusManager = FocusManager(root)

        /** Caller-driven timer queue using the surface clock. @since 1.0.0 */
        public val scheduler: Scheduler = Scheduler(clock)

        /** Hover path tracker shared with the router and overlays. @since 1.0.0 */
        public val hover: HoverState = HoverState()

        /** Overlay stack painted after the root. @since 1.0.0 */
        public val overlays: OverlayHost = OverlayHost(Rect.EMPTY, focus, hover)

        /** Command bindings checked after focused-widget handling. @since 1.0.0 */
        public val keymap: Keymap = Keymap()

        private val guiContext: GuiContext = GuiContext(theme, shapes, focus, overlays, scheduler, clipboard)
        private val router: EventRouter = EventRouter(root, focus, overlays, hover, keymap = keymap, clock = clock)
        private var layoutRevision: Long = 0
        private var paintRevision: Long = 0

        internal var layoutCount: Int = 0
            private set
        internal var paintCount: Int = 0
            private set

        init {
            require(root.parent == null) { "the surface root must not have a parent" }
            root.setSurfaceInvalidation { onLayoutInvalidated() }
            root.setSurfacePaintInvalidation { onPaintInvalidated() }
            root.attachTree(guiContext)
            overlays.setGuiContext(guiContext)
            overlays.setSurfaceInvalidation({ onLayoutInvalidated() }, { onPaintInvalidated() })
        }

        /**
         * Creates a surface with fresh runtime services.
         *
         * @since 1.0.0
         */
        public constructor(
            root: Widget,
            theme: Theme,
            shapes: ResolvedShapeSet,
            clipboard: Clipboard = InMemoryClipboard(),
        ) : this(root, theme, shapes, clipboard, System::nanoTime)

        /**
         * Sets the active screen scope for [keymap].
         *
         * @since 1.0.0
         */
        public var screenScope: String?
            get() = router.screenScope
            set(value) {
                router.screenScope = value
            }

        /**
         * Resizes the surface and arranges the root to the full grid.
         *
         * The caller should call this when the host matrix dimensions change.
         * The method never paints.
         *
         * @since 1.0.0
         */
        public fun resize(
            columns: Int,
            rows: Int,
        ) {
            if (columns <= 0 || rows <= 0) {
                throw GuiException("surface dimensions must be positive, got ${columns}x$rows")
            }
            size = GridSize(columns, rows)
            root.bounds = Rect(0, 0, columns, rows)
            root.invalidateLayout()
            overlays.screen = Rect(0, 0, columns, rows)
            onLayoutInvalidated()
        }

        /**
         * Dispatches [event] through overlays and the root tree.
         *
         * Layout is made current before routing. The method never paints and
         * never clears the paint flag.
         *
         * @return whether a handler consumed the event.
         * @since 1.0.0
         */
        public fun dispatch(event: GuiEvent): Boolean {
            ensureResized()
            ensureLayout()
            return router.dispatch(event)
        }

        /**
         * Runs timers due at [nowNanos].
         *
         * @return whether a callback ran and marked layout or paint dirty.
         * @since 1.0.0
         */
        public fun tick(nowNanos: Long): Boolean {
            val layoutBefore = layoutRevision
            val paintBefore = paintRevision
            val ran = scheduler.runDue(nowNanos)
            return ran && (layoutRevision != layoutBefore || paintRevision != paintBefore)
        }

        /**
         * Paints the complete surface into [canvas].
         *
         * The canvas must match [size]. Layout is made current first; the whole
         * canvas is filled with the theme background, then the root and
         * overlays paint. A failed layout or paint keeps both dirty flags set.
         *
         * @throws GuiException if the surface has not been resized or the
         *   canvas dimensions differ from [size].
         * @since 1.0.0
         */
        public fun paint(canvas: CellCanvas) {
            if (canvas.matrix.columns != size.columns || canvas.matrix.rows != size.rows) {
                throw GuiException(
                    "canvas is ${canvas.matrix.columns}x${canvas.matrix.rows}, " +
                        "surface is ${size.columns}x${size.rows}",
                )
            }
            val paintBefore = paintRevision
            val layoutBefore = layoutRevision
            try {
                ensureResized()
                ensureLayout()
                val currentTheme = theme
                canvas.fill(
                    Rect(0, 0, size.columns, size.rows),
                    SPACE,
                    currentTheme.text,
                    currentTheme.background,
                )
                val context = PaintContext(canvas, shapes, currentTheme)
                root.render(context)
                overlays.paint(context)
            } catch (failure: Throwable) {
                needsLayout = true
                needsPaint = true
                throw failure
            }
            needsLayout = layoutRevision != layoutBefore
            needsPaint = paintRevision != paintBefore
            paintCount++
        }

        /**
         * Invalidates layout for the whole tree.
         *
         * @since 1.0.0
         */
        public fun requestLayout() {
            root.invalidateLayout()
        }

        /**
         * Invalidates paint without changing layout.
         *
         * @since 1.0.0
         */
        public fun requestPaint() {
            root.invalidatePaint()
        }

        /**
         * Replaces the theme and invalidates paint only.
         *
         * @since 1.0.0
         */
        public fun setTheme(theme: Theme) {
            this.theme = theme
            guiContext.theme = theme
            onPaintInvalidated()
        }

        /**
         * Replaces the resolved shape catalog and invalidates paint only.
         *
         * The font is owned by [shapes]; callers that switch shape sets resolve
         * the new set against the same font.
         *
         * @since 1.0.0
         */
        public fun setShapes(shapes: ResolvedShapeSet) {
            this.shapes = shapes
            guiContext.shapes = shapes
            onPaintInvalidated()
        }

        private fun ensureResized() {
            if (size.columns <= 0 || size.rows <= 0) {
                throw GuiException("surface must be resized before dispatch or paint")
            }
        }

        private fun ensureLayout() {
            if (!needsLayout) {
                return
            }
            val layoutBefore = layoutRevision
            try {
                root.layout(Constraints.tight(IntSize(size.columns, size.rows)))
            } catch (failure: Throwable) {
                needsLayout = true
                needsPaint = true
                throw failure
            }
            layoutCount++
            needsLayout = layoutRevision != layoutBefore
        }

        private fun onLayoutInvalidated() {
            layoutRevision++
            paintRevision++
            needsLayout = true
            needsPaint = true
        }

        private fun onPaintInvalidated() {
            paintRevision++
            needsPaint = true
        }

        private companion object {
            private const val SPACE: Int = 0x0020
        }
    }
