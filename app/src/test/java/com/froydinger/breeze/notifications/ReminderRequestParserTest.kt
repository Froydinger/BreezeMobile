package com.froydinger.breeze.notifications

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderRequestParserTest {
    private val zone = ZoneId.of("America/Chicago")
    private val now = ZonedDateTime.of(2026, 9, 23, 14, 10, 0, 0, zone)

    @Test
    fun tomorrowAtThreePmUsesTheFollowingLocalDate() {
        val reminder = ReminderRequestParser.parse("Remind me to buy eggs tomorrow at 3pm", now)
        assertNotNull(reminder)
        val due = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(reminder!!.dueAt), zone)
        assertEquals(LocalDate.of(2026, 9, 24), due.toLocalDate())
        assertEquals(LocalTime.of(15, 0), due.toLocalTime())
        assertEquals("buy eggs", reminder.title)
    }

    @Test
    fun missingReminderDetailsAreAskedInChatAndCanBeProvidedInFollowUp() {
        val initial = "Remind me to buy eggs"
        assertEquals("When should I remind you to buy eggs?", ReminderRequestParser.missingDetailQuestion(initial))

        val completed = ReminderRequestParser.appendFollowUp(initial, "tomorrow at 3pm")
        val reminder = ReminderRequestParser.parse(completed, now)
        assertNotNull(reminder)
        assertEquals("buy eggs", reminder!!.title)
        assertEquals("What time should I remind you to buy eggs?", ReminderRequestParser.missingDetailQuestion("Remind me to buy eggs tomorrow"))
    }

    @Test
    fun missingReminderTaskCanBeProvidedAfterTheTime() {
        val initial = "Remind me tomorrow"
        assertEquals("What should I remind you to do?", ReminderRequestParser.missingDetailQuestion(initial))
        val completed = ReminderRequestParser.appendFollowUp(initial, "to buy eggs at 3pm")
        assertEquals("buy eggs", ReminderRequestParser.parse(completed, now)?.title)
    }

    @Test
    fun oneMinuteReminderIsExactlyOneMinuteAway() {
        val reminder = ReminderRequestParser.parse("Remind me to buy eggs in 1 minute", now)
        assertNotNull(reminder)
        assertEquals(now.plusMinutes(1).toInstant().toEpochMilli(), reminder!!.dueAt)
    }
}
