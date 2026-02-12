package com.pck.nex.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.runBlocking
import com.pck.nex.NeXApp

object HabitWidgetUpdater {

    fun toggle(
        context: Context,
        widgetId: Int,
        taskId: String
    ) {
        val app = context.applicationContext as NeXApp
        val habitRepo = app.habitWidgetRepo

        runBlocking {
            habitRepo.toggleTask(widgetId, taskId)
        }

        refreshWidget(context, widgetId)
    }

    fun refreshWidget(
        context: Context,
        widgetId: Int
    ) {
        val manager = AppWidgetManager.getInstance(context)

        val intent = android.content.Intent(context, HabitWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        }

        context.sendBroadcast(intent)
    }

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, HabitWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)

        if (ids.isNotEmpty()) {
            val intent = android.content.Intent(context, HabitWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

            context.sendBroadcast(intent)
        }
    }
}
