package com.psiwray.organization.terminal_window_gui.widget

/**
 * One tab: a stable key, a title for the tab bar, and its content widget.
 *
 * Content is attached only while its tab is active. The tab bar itself never
 * paints, measures, or focuses inactive content.
 *
 * @property key Stable application key.
 * @property title Text shown in the tab bar.
 * @property content Widget shown when the tab is active.
 * @property enabled Whether the tab can be selected.
 * @since 1.0.0
 */
public data class Tab(
    public val key: Any,
    public var title: String,
    public val content: Widget,
    public var enabled: Boolean = true,
) {
    /** Creates a tab whose key is its title. @since 1.0.0 */
    public constructor(
        title: String,
        content: Widget,
    ) : this(title, title, content, true)
}
