package com.pck.nex

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pck.nex.data.db.NeXDatabase
import com.pck.nex.data.repo.DayRepository
import com.pck.nex.data.repo.TemplateRepository
import com.pck.nex.notifications.NotificationUtils
import com.pck.nex.reminders.TomorrowReminderWorker
import java.util.concurrent.TimeUnit

class NeXApp : Application() {

    lateinit var dayRepo: DayRepository
        private set


    lateinit var templateRepo: TemplateRepository
        private set

    override fun onCreate() {
        super.onCreate()

        /* -------------------------------
         * Notifications
         * ------------------------------- */
        NotificationUtils.createChannels(this)

        val work = PeriodicWorkRequestBuilder<TomorrowReminderWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TomorrowReminderWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )

        /* -------------------------------
         * Database + Repositories
         * ------------------------------- */
        val db = Room.databaseBuilder(
            applicationContext,
            NeXDatabase::class.java,
            "nex.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        dayRepo = DayRepository(db.dayDao())

        templateRepo = TemplateRepository(
            templateDao = db.templateDao(),
            dayRepository = dayRepo
        )
    }
}
