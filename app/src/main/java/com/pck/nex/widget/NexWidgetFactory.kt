package com.pck.nex.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.pck.nex.NeXApp
import com.pck.nex.R
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class NeXWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private data class Row(val id: String, val title: String)

    private var rows: List<Row> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Called by system; ok to do blocking IO here in RemoteViewsFactory
        val repo = (context.applicationContext as NeXApp).repo
        val today = LocalDate.now()

        val tasks = runBlocking { repo.uncheckedTasksForToday(today) }
        rows = tasks.map { Row(it.id.toString(), it.title) }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_task_row)

        rv.setTextViewText(R.id.w_task_title, row.title)
        rv.setTextViewText(R.id.w_task_check, "☐")

        val fillIn = Intent().apply {
            action = NeXTodayWidgetProvider.ACTION_TOGGLE_DONE
            putExtra(NeXTodayWidgetProvider.EXTRA_TASK_ID, row.id)
        }

        // Set click intent ONLY on the checkbox, not the entire row
        rv.setOnClickFillInIntent(R.id.w_task_check, fillIn)

        return rv
    }



    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = rows[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}
