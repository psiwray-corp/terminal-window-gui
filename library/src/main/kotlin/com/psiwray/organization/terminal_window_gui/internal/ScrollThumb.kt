package com.psiwray.organization.terminal_window_gui.internal

internal object ScrollThumb {
    fun length(
        viewport: Int,
        content: Int,
        track: Int,
    ): Int {
        if (track <= 0) {
            return 0
        }
        if (content <= 0 || content <= viewport) {
            return track
        }
        val scaled = (viewport.toLong() * track / content).toInt()
        return scaled.coerceIn(1, track)
    }

    fun position(
        offset: Int,
        maxOffset: Int,
        track: Int,
        thumb: Int,
    ): Int {
        val travel = track - thumb
        if (travel <= 0 || maxOffset <= 0) {
            return 0
        }
        val numerator = offset.toLong() * travel
        return ((numerator + maxOffset / 2) / maxOffset).toInt().coerceIn(0, travel)
    }

    fun offsetForPosition(
        position: Int,
        maxOffset: Int,
        track: Int,
        thumb: Int,
    ): Int {
        val travel = track - thumb
        if (travel <= 0 || maxOffset <= 0) {
            return 0
        }
        val numerator = position.coerceIn(0, travel).toLong() * maxOffset
        return ((numerator + travel / 2) / travel).toInt().coerceIn(0, maxOffset)
    }
}
