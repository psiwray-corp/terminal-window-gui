package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.shape.Shape

/**
 * Eager tree node with a stable key, label, optional icon, and children.
 *
 * Lazy child providers are not part of v1. Use [TreeView] accessor lambdas for
 * arbitrary node types.
 *
 * @property key Stable selection and expansion key.
 * @property label Display label.
 * @property icon Optional one-cell icon.
 * @property children Child nodes, in display order.
 * @since 1.0.0
 */
public data class TreeNode<T>(
    public val key: T,
    public val label: String,
    public val icon: Shape? = null,
    public val children: List<TreeNode<T>> = emptyList(),
)
