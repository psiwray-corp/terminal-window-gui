package com.psiwray.organization.terminal_window_gui.widget

/**
 * A tight flex child that fills its Row or Column allocation.
 *
 * @since 1.0.0
 */
public class Expanded(
    flex: Int = 1,
    child: Widget? = null,
) : Flexible(flex, FlexFit.TIGHT, child)
