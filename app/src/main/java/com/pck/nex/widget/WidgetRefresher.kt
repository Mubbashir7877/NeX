package com.pck.nex.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.pck.nex.R

object WidgetRefresher {
    fun refreshTodayWidget(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, NeXTodayWidgetProvider::class.java))
        if (ids.isEmpty()) return

        // Forces RemoteViewsFactory.onDataSetChanged() for the list
        ids.forEach { id ->
            mgr.notifyAppWidgetViewDataChanged(id, R.id.w_list)
        }

        // Also triggers full RemoteViews rebind via provider logic
        // (simplest: broadcast ACTION_REFRESH which your provider already handles)
        context.sendBroadcast(
            android.content.Intent(context, NeXTodayWidgetProvider::class.java).apply {
                action = NeXTodayWidgetProvider.ACTION_REFRESH
            }
        )
    }
}
