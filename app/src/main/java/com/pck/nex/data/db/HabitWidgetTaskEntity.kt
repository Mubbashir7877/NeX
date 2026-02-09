package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "habit_widget_tasks",
    primaryKeys = ["appWidgetId", "taskId", "epochDay"],
    indices = [Index("appWidgetId")]
)
data class HabitWidgetTaskEntity(
    val appWidgetId: Int,
    val taskId: String,
    val epochDay: Long,
    val isChecked: Boolean
)
