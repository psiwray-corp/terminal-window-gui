package com.psiwray.organization.terminal_window_gui.input

/**
 * Deterministic, caller-driven timer queue.
 *
 * No threads, coroutines, or timers are created. Call [runDue] on the owning
 * thread with a monotonic nanosecond timestamp.
 *
 * @property clock Monotonic clock used to timestamp new entries.
 * @since 1.0.0
 */
public class Scheduler(
    private val clock: () -> Long = System::nanoTime,
) {
    private val entries: MutableList<Entry> = ArrayList()
    private var nextId: Long = 0
    private var nextSequence: Long = 0

    /**
     * Schedules [action] after [delayMillis].
     *
     * @since 1.0.0
     */
    public fun postDelayed(
        delayMillis: Long,
        action: () -> Unit,
    ): TimerHandle {
        require(delayMillis >= 0) { "delayMillis must be non-negative, got $delayMillis" }
        val handle = TimerHandle(nextId++)
        val deadline = clock() + delayMillis * 1_000_000L
        entries.add(Entry(handle, deadline, nextSequence++, action))
        return handle
    }

    /**
     * Schedules [action] after [delayMillis].
     *
     * @since 1.0.0
     */
    public fun postDelayed(
        delayMillis: Int,
        action: () -> Unit,
    ): TimerHandle = postDelayed(delayMillis.toLong(), action)

    /** Cancels [handle] if it has not run. @since 1.0.0 */
    public fun cancel(handle: TimerHandle) {
        handle.cancelled = true
        entries.removeAll { it.handle === handle }
    }

    /**
     * Runs every entry due at or before [nowNanos] in deadline and insertion
     * order.
     *
     * @return whether at least one callback ran.
     * @since 1.0.0
     */
    public fun runDue(nowNanos: Long): Boolean {
        var ran = false
        while (true) {
            var best = -1
            var index = 0
            while (index < entries.size) {
                val entry = entries[index]
                if (!entry.handle.cancelled && entry.deadline <= nowNanos) {
                    if (best < 0) {
                        best = index
                    } else {
                        val current = entries[best]
                        if (entry.deadline < current.deadline ||
                            (entry.deadline == current.deadline && entry.sequence < current.sequence)
                        ) {
                            best = index
                        }
                    }
                }
                index++
            }
            if (best < 0) {
                return ran
            }
            val entry = entries.removeAt(best)
            if (entry.handle.cancelled) {
                continue
            }
            entry.handle.cancelled = true
            ran = true
            entry.action()
        }
    }

    internal fun now(): Long = clock()

    /** Earliest pending deadline, or `null` when no entry is pending. @since 1.0.0 */
    public fun nextDeadlineNanos(): Long? {
        var next: Long? = null
        for (entry in entries) {
            if (entry.handle.cancelled) {
                continue
            }
            if (next == null || entry.deadline < next) {
                next = entry.deadline
            }
        }
        return next
    }

    private class Entry(
        val handle: TimerHandle,
        val deadline: Long,
        val sequence: Long,
        val action: () -> Unit,
    )
}
