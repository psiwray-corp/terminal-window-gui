package com.psiwray.organization.terminal_window_gui.internal

import com.psiwray.organization.terminal_window_gui.paint.PaintContext
import com.psiwray.organization.terminal_window_gui.shape.Shape
import com.psiwray.organization.terminal_window_gui.widget.BoxStyle

internal object BoxPainter {
    fun horizontal(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_H
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_H
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_H
        }

    fun vertical(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_V
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_V
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_V
        }

    fun topLeft(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_TOP_LEFT
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_TOP_LEFT
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_TOP_LEFT
        }

    fun topRight(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_TOP_RIGHT
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_TOP_RIGHT
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_TOP_RIGHT
        }

    fun bottomLeft(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_BOTTOM_LEFT
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_BOTTOM_LEFT
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_BOTTOM_LEFT
        }

    fun bottomRight(style: BoxStyle): Shape =
        when (style) {
            BoxStyle.SINGLE -> Shape.BOX_SINGLE_BOTTOM_RIGHT
            BoxStyle.DOUBLE -> Shape.BOX_DOUBLE_BOTTOM_RIGHT
            BoxStyle.ROUNDED -> Shape.BOX_ROUNDED_BOTTOM_RIGHT
        }

    fun paint(
        context: PaintContext,
        style: BoxStyle,
        width: Int,
        height: Int,
        fg: Int,
        bg: Int,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }
        val horizontal = horizontal(style)
        val vertical = vertical(style)
        if (width == 1) {
            var y = 0
            while (y < height) {
                context.shape(vertical, 0, y, fg, bg)
                y++
            }
            return
        }
        if (height == 1) {
            var x = 0
            while (x < width) {
                context.shape(horizontal, x, 0, fg, bg)
                x++
            }
            return
        }
        context.shape(topLeft(style), 0, 0, fg, bg)
        context.shape(topRight(style), width - 1, 0, fg, bg)
        context.shape(bottomLeft(style), 0, height - 1, fg, bg)
        context.shape(bottomRight(style), width - 1, height - 1, fg, bg)
        var x = 1
        while (x < width - 1) {
            context.shape(horizontal, x, 0, fg, bg)
            context.shape(horizontal, x, height - 1, fg, bg)
            x++
        }
        var y = 1
        while (y < height - 1) {
            context.shape(vertical, 0, y, fg, bg)
            context.shape(vertical, width - 1, y, fg, bg)
            y++
        }
    }
}
