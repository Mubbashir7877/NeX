package com.pck.nex.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pck.nex.MainActivity
import com.pck.nex.NeXApp
import com.pck.nex.R
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class NeXTodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            updateOne(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_DONE -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)

                android.util.Log.d("NeXWidget", "ACTION_TOGGLE_DONE taskId=$taskId")

                if (!taskId.isNullOrBlank()) {
                    handleToggle(context, taskId)
                }
                refreshAll(context)
            }

            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                refreshAll(context)
            }
        }
    }

    private fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, NeXTodayWidgetProvider::class.java))
        ids.forEach { updateOne(context, mgr, it) }
    }

    private fun updateOne(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val repo = (context.applicationContext as NeXApp).repo
        val today = LocalDate.now()

        val day = runBlocking { repo.getOrCreate(today) }

        val views = RemoteViews(context.packageName, R.layout.widget_nex_4x5)

        val label = today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        views.setTextViewText(R.id.w_date, "Today · $label")

        val bgColor = WidgetPalette.bgFromSeed(day.backgroundSeed, day.backgroundType)
        views.setInt(R.id.w_root, "setBackgroundColor", bgColor)

        // Refresh button
        views.setOnClickPendingIntent(
            R.id.w_refresh,
            pendingBroadcast(context, ACTION_REFRESH, reqCode = 100 + appWidgetId)
        )

        // Open app for any other click area
        views.setOnClickPendingIntent(
            R.id.w_open_app_area,
            pendingOpenApp(context)
        )


        // List adapter
        val svc = Intent(context, NeXWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setRemoteAdapter(R.id.w_list, svc)
        views.setEmptyView(R.id.w_list, R.id.w_empty)

        // Template for row checkbox clicks
        val template = Intent(context, NeXTodayWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_DONE
        }
        val templatePI = PendingIntent.getBroadcast(
            context,
            200 + appWidgetId,
            template,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        views.setPendingIntentTemplate(R.id.w_list, templatePI)

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.w_list)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun pendingOpenApp(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            999,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingBroadcast(context: Context, action: String, reqCode: Int): PendingIntent {
        val i = Intent(context, NeXTodayWidgetProvider::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.pck.nex.widget.ACTION_REFRESH"
        const val ACTION_TOGGLE_DONE = "com.pck.nex.widget.ACTION_TOGGLE_DONE"

        const val EXTRA_TASK_ID = "extra_task_id"

        fun handleToggle(context: Context, taskId: String) {
            val repo = (context.applicationContext as NeXApp).repo
            runBlocking {
                repo.setTaskDone(UUID.fromString(taskId), true)
            }
        }
    }
}
