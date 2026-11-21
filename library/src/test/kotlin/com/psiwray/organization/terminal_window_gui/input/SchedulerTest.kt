package com.psiwray.organization.terminal_window_gui.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchedulerTest {
    @Test
    fun dueCallbacksRunInDeadlineThenInsertionOrder() {
        val now = 0L
        val scheduler = Scheduler { now }
        val order = ArrayList<Int>()
        scheduler.postDelayed(10, { order.add(1) })
        scheduler.postDelayed(5, { order.add(2) })
        scheduler.postDelayed(5, { order.add(3) })

        assertEquals(5_000_000L, scheduler.nextDeadlineNanos())
        assertFalse(scheduler.runDue(4_999_999))
        assertTrue(scheduler.runDue(5_000_000))
        assertEquals(listOf(2, 3), order)
        assertTrue(scheduler.runDue(10_000_000))
        assertEquals(listOf(2, 3, 1), order)
        assertNull(scheduler.nextDeadlineNanos())
    }

    @Test
    fun cancelPreventsCallback() {
        var now = 0L
        val scheduler = Scheduler { now }
        var runs = 0
        val handle = scheduler.postDelayed(100L, { runs++ })
        assertFalse(handle.isCancelled)
        scheduler.cancel(handle)
        assertTrue(handle.isCancelled)
        now = 200_000_000
        assertFalse(scheduler.runDue(now))
        assertEquals(0, runs)
        assertNull(scheduler.nextDeadlineNanos())
    }

    @Test
    fun callbackCanScheduleAnotherDueEntry() {
        val now = 0L
        val scheduler = Scheduler { now }
        val order = ArrayList<String>()
        scheduler.postDelayed(0) {
            order.add("first")
            scheduler.postDelayed(0) { order.add("second") }
        }
        assertTrue(scheduler.runDue(0))
        assertEquals(listOf("first", "second"), order)
    }
}
