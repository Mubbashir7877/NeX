package com.pck.nex.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Task"
        val taskId = intent.getIntExtra(EXTRA_ID_INT, 0)
        val day = intent.getStringExtra(EXTRA_DAY) // "YYYY-MM-DD"

        val label = if (day != null) {
            val d = LocalDate.parse(day)
            d.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        } else "Today"

        NotificationUtils.notifyTask(
            context = context,
            id = taskId,
            title = "Due: $taskTitle",
            text = "Scheduled for $label"
        )
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_ID_INT = "idInt"
        const val EXTRA_DAY = "day" // yyyy-MM-dd
    }
}
