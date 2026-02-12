package com.pck.nex.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pck.nex.R

class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.pck.nex.HABIT_TOGGLE"
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_habit)

            val intent = Intent(context, HabitWidgetService::class.java)
            views.setRemoteAdapter(R.id.habit_list, intent)

            val clickIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }

            val pending = PendingIntent.getBroadcast(
                context,
                widgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setPendingIntentTemplate(R.id.habit_list, pending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

            HabitWidgetUpdater.toggle(context, widgetId, taskId)
        }
    }
}
