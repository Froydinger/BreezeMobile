package com.froydinger.breeze.notifications

import com.froydinger.breeze.LocalReminder
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun monthlyReminderKeepsItsOriginalDayAfterFebruary() {
        val january = ZonedDateTime.of(2027, 1, 31, 9, 0, 0, 0, zone)
        val reminder = LocalReminder(title = "Pay rent", dueAt = january.toInstant().toEpochMilli(), repeat = ReminderRepeat.MONTHLY, repeatDayOfMonth = 31)
        val february = ReminderScheduler.nextOccurrence(reminder, january.plusMinutes(1))!!
        assertEquals(ZonedDateTime.of(2027, 2, 28, 9, 0, 0, 0, zone).toInstant().toEpochMilli(), february)
        val next = ReminderScheduler.nextOccurrence(reminder.copy(dueAt = february), Instant.ofEpochMilli(february).atZone(zone).plusMinutes(1))
        assertEquals(ZonedDateTime.of(2027, 3, 31, 9, 0, 0, 0, zone).toInstant().toEpochMilli(), next)
    }

    @Test
    fun oneTimeReminderHasNoNextOccurrence() {
        val now = ZonedDateTime.of(2026, 9, 24, 9, 0, 0, 0, zone)
        assertNull(ReminderScheduler.nextOccurrence(LocalReminder(title = "Call", dueAt = now.toInstant().toEpochMilli()), now))
    }
}
