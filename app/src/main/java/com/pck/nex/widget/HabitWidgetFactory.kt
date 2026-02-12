package com.pck.nex.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.pck.nex.NeXApp
import com.pck.nex.R
import java.time.LocalDate

class HabitWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    private val app = context.applicationContext as NeXApp
    private val habitRepo = app.habitWidgetRepo
    private val templateRepo = app.templateRepo

    private var items: List<HabitItem> = emptyList()

    override fun onDataSetChanged() {
        val today = LocalDate.now().toEpochDay()

        kotlinx.coroutines.runBlocking {
            val widget = habitRepo.getWidgetSync(widgetId) ?: return@runBlocking

            val templateTasks =
                templateRepo.getTemplateTasksSync(widget.templateId)

            val states =
                habitRepo.getTasksForDaySync(widgetId, today)

            items = templateTasks.map { t ->
                HabitItem(
                    taskId = t.taskId,
                    title = t.title,
                    checked = states.any {
                        it.taskId == t.taskId && it.isChecked
                    }
                )
            }
        }
    }


    override fun getCount() = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_habit_row)

        rv.setTextViewText(R.id.habit_task_text, item.title)
        val icon = if (item.checked)
            R.drawable.ic_checkbox_checked
        else
            R.drawable.ic_checkbox_unchecked

        rv.setImageViewResource(R.id.habit_checkbox, icon)

        val fillIntent = Intent().apply {
            putExtra(HabitWidgetProvider.EXTRA_TASK_ID, item.taskId)
        }

        rv.setOnClickFillInIntent(R.id.habit_checkbox, fillIntent)
        return rv
    }

    override fun getViewTypeCount() = 1
    override fun hasStableIds() = true
    override fun getItemId(position: Int) = position.toLong()
    override fun getLoadingView(): RemoteViews? = null
    override fun onCreate() {}
    override fun onDestroy() {}
}

data class HabitItem(
    val taskId: String,
    val title: String,
    val checked: Boolean
)
