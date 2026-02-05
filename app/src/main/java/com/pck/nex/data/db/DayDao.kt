package com.pck.nex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    // -------------------------
    // Widget support
    // -------------------------

    @Query(
        """
        SELECT * FROM tasks
        WHERE dayDate = :date AND isDone = 0
        ORDER BY orderIndex
        """
    )
    suspend fun uncheckedTasksForDay(date: String): List<TaskEntity>

    @Query("UPDATE tasks SET isDone = :isDone WHERE id = :id")
    suspend fun setTaskDone(id: String, isDone: Boolean)

    // -------- Library queries (only days with >= 1 task) --------

    data class LibraryRow(
        val date: String,
        val backgroundSeed: Long,
        val backgroundType: Int,
        val taskCount: Int
    )

    @Query(
        """
        SELECT d.date AS date,
               d.backgroundSeed AS backgroundSeed,
               d.backgroundType AS backgroundType,
               COUNT(t.id) AS taskCount
        FROM days d
        INNER JOIN tasks t ON t.dayDate = d.date
        GROUP BY d.date, d.backgroundSeed, d.backgroundType
        ORDER BY d.date DESC
        """
    )
    fun libraryDaysNewest(): Flow<List<LibraryRow>>

    @Query(
        """
        SELECT d.date AS date,
               d.backgroundSeed AS backgroundSeed,
               d.backgroundType AS backgroundType,
               COUNT(t.id) AS taskCount
        FROM days d
        INNER JOIN tasks t ON t.dayDate = d.date
        GROUP BY d.date, d.backgroundSeed, d.backgroundType
        ORDER BY d.date ASC
        """
    )
    fun libraryDaysOldest(): Flow<List<LibraryRow>>

    @Query(
        """
        SELECT d.date AS date,
               d.backgroundSeed AS backgroundSeed,
               d.backgroundType AS backgroundType,
               COUNT(t.id) AS taskCount
        FROM days d
        INNER JOIN tasks t ON t.dayDate = d.date
        GROUP BY d.date, d.backgroundSeed, d.backgroundType
        ORDER BY taskCount DESC, d.date DESC
        """
    )
    fun libraryDaysMostTasks(): Flow<List<LibraryRow>>

    // -------- Day mode: "previous day with tasks" --------

    @Query(
        """
        SELECT dayDate
        FROM tasks
        WHERE dayDate < :fromDate
        GROUP BY dayDate
        ORDER BY dayDate DESC
        LIMIT 1
        """
    )
    suspend fun previousDayWithTasks(fromDate: String): String?
}
