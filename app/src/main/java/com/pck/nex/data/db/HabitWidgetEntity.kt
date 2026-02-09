package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_widgets")
data class HabitWidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    val templateId: String,
    val templateName: String,
    val targetDays: Int,
    val streak: Int,
    val lastCompletedEpochDay: Long?, // LocalDate.toEpochDay()
    val isCompleted: Boolean
)
