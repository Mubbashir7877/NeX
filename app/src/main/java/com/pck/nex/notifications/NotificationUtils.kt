package com.pck.nex.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pck.nex.R

object NotificationUtils {
    const val CHANNEL_TASKS = "tasks_channel"
    const val CHANNEL_REMINDERS = "reminders_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tasks = NotificationChannel(
                CHANNEL_TASKS,
                "Task Due Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for tasks with a specific due time." }

            val reminders = NotificationChannel(
                CHANNEL_REMINDERS,
                "Planning Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminders to create tomorrow's task list." }

            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(tasks)
            nm?.createNotificationChannel(reminders)
        }
    }

    fun notifyTask(context: Context, id: Int, title: String, text: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }

    fun notifyReminder(context: Context, id: Int, title: String, text: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
