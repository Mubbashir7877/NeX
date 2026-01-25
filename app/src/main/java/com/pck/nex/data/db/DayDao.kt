package com.pck.nex.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    @Query("SELECT * FROM days WHERE date = :date")
    suspend fun getDay(date: String): DayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: DayEntity)

    @Query("SELECT * FROM tasks WHERE dayDate = :date ORDER BY orderIndex")
    fun tasksForDay(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}
