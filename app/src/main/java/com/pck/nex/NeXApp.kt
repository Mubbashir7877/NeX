package com.pck.nex

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pck.nex.notifications.NotificationUtils
import com.pck.nex.reminders.TomorrowReminderWorker
import java.util.concurrent.TimeUnit

class NeXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channels once
        NotificationUtils.createChannels(this)

        // Enqueue the periodic reminder (15-minute cadence)
        val work = PeriodicWorkRequestBuilder<TomorrowReminderWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TomorrowReminderWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
}
