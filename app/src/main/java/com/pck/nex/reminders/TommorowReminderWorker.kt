package com.pck.nex.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pck.nex.data.prefs.PrefsDataStore
import com.pck.nex.notifications.NotificationUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

class TomorrowReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_NAME = "nex_tomorrow_reminder_worker"
        private const val NOTIF_ID = 900001
    }

    override suspend fun doWork(): Result {
        val prefs = PrefsDataStore(applicationContext)
        val enabled = (prefs.reminderEnabled.first() == 1)
        if (!enabled) return Result.success()

        val minuteOfDay = prefs.reminderMinuteOfDay.first()
        val now = LocalDateTime.now()
        val minutesNow = now.hour * 60 + now.minute

        // Only fire around/after the target time (this runs every ~15 min)
        if (minutesNow < minuteOfDay) return Result.success()

        val tomorrow = LocalDate.now().plusDays(1)

        // TODO: Replace this with a real DB check once Room is added.
        // For now, pretend "tomorrow’s Day does not exist" to demonstrate:
        val tomorrowExists = false

        if (!tomorrowExists) {
            NotificationUtils.notifyReminder(
                applicationContext,
                NOTIF_ID,
                "Plan tomorrow’s tasks",
                "Tap to create your list for ${tomorrow.toString()}."
            )
        }
        return Result.success()
    }
}
