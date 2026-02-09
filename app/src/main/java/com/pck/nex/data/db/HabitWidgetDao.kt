package com.pck.nex.data.db

import androidx.room.*

@Dao
interface HabitWidgetDao {

    @Query("SELECT * FROM habit_widgets WHERE appWidgetId = :id")
    suspend fun getWidget(id: Int): HabitWidgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidget(entity: HabitWidgetEntity)

    @Delete
    suspend fun deleteWidget(entity: HabitWidgetEntity)

    @Query("""
        SELECT * FROM habit_widget_tasks
        WHERE appWidgetId = :widgetId AND epochDay = :epochDay
    """)
    suspend fun tasksForDay(
        widgetId: Int,
        epochDay: Long
    ): List<HabitWidgetTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(entity: HabitWidgetTaskEntity)
}
