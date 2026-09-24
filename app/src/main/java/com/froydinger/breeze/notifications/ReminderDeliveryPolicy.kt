package com.froydinger.breeze.notifications

/** A failed notification leaves the occurrence pending for a later retry. */
internal enum class ReminderPostAction { KEEP_PENDING, COMPLETE, ADVANCE }

internal fun reminderPostAction(posted: Boolean, repeat: ReminderRepeat): ReminderPostAction = when {
    !posted -> ReminderPostAction.KEEP_PENDING
    repeat == ReminderRepeat.NONE -> ReminderPostAction.COMPLETE
    else -> ReminderPostAction.ADVANCE
}
