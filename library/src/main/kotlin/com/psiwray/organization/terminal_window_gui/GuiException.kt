package com.psiwray.organization.terminal_window_gui

/**
 * Invalid surface, canvas, or widget-tree state.
 *
 * The surface is passive and single-thread-affine; an exception describes a
 * caller error such as dispatching or painting before the first non-zero
 * resize.
 *
 * @param message Diagnostic text.
 * @param cause Optional underlying failure.
 * @since 1.0.0
 */
public class GuiException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
