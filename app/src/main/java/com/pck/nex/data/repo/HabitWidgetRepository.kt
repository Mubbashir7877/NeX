package com.pck.nex.data.repo

import com.pck.nex.data.db.HabitWidgetDao
import com.pck.nex.data.db.HabitWidgetEntity
import com.pck.nex.data.db.HabitWidgetTaskEntity
import java.time.LocalDate

class HabitWidgetRepository(
    private val dao: HabitWidgetDao
) {

    suspend fun toggleTask(
        widgetId: Int,
        taskId: String
    ): Boolean {
        val today = LocalDate.now().toEpochDay()
        val tasks = dao.tasksForDay(widgetId, today)
        val existing = tasks.find { it.taskId == taskId }

        val newState = !(existing?.isChecked ?: false)

        dao.upsertTask(
            HabitWidgetTaskEntity(
                appWidgetId = widgetId,
                taskId = taskId,
                epochDay = today,
                isChecked = newState
            )
        )

        return newState
    }

    suspend fun updateStreakIfComplete(
        widget: HabitWidgetEntity,
        allDoneToday: Boolean
    ): HabitWidgetEntity {
        val today = LocalDate.now().toEpochDay()

        if (!allDoneToday || widget.isCompleted) return widget

        val newStreak =
            if (widget.lastCompletedEpochDay == today - 1) widget.streak + 1
            else 1

        val completed = newStreak >= widget.targetDays

        val updated = widget.copy(
            streak = newStreak,
            lastCompletedEpochDay = today,
            isCompleted = completed
        )

        dao.upsertWidget(updated)
        return updated
    }
}
