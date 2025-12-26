package com.pck.nex.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TaskAlarmScheduler {

    fun scheduleTaskAlarm(
        context: Context,
        day: LocalDate,
        taskTitle: String,
        requestId: Int,
        triggerAt: LocalDateTime
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TITLE, taskTitle)
            putExtra(TaskAlarmReceiver.EXTRA_ID_INT, requestId)
            putExtra(TaskAlarmReceiver.EXTRA_DAY, day.toString())
        }

        val pi = PendingIntent.getBroadcast(
            context,
            requestId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val epochMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else true

            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
            } else {
                // Fallback: still schedule, just not exact
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
            }
        } catch (_: SecurityException) {
            // Fallback again; avoid crashing the app
            am.set(AlarmManager.RTC_WAKEUP, epochMillis, pi)
        }
    }

    fun cancelTaskAlarm(context: Context, requestId: Int) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }
}
