package com.froydinger.breeze.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDeliveryPolicyTest {
    @Test
    fun blockedNotificationKeepsOneTimeAndRecurringOccurrencesPending() {
        assertEquals(ReminderPostAction.KEEP_PENDING, reminderPostAction(false, ReminderRepeat.NONE))
        assertEquals(ReminderPostAction.KEEP_PENDING, reminderPostAction(false, ReminderRepeat.DAILY))
    }

    @Test
    fun successfulNotificationCompletesOrAdvancesTheRightOccurrence() {
        assertEquals(ReminderPostAction.COMPLETE, reminderPostAction(true, ReminderRepeat.NONE))
        assertEquals(ReminderPostAction.ADVANCE, reminderPostAction(true, ReminderRepeat.DAILY))
    }
}
