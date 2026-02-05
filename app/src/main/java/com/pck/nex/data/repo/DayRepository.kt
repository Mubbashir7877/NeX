package com.pck.nex.data.repo

import com.pck.nex.data.db.DayDao
import com.pck.nex.data.db.DayEntity
import com.pck.nex.data.db.TaskEntity
import com.pck.nex.domain.model.Day
import com.pck.nex.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DayRepository(private val dao: DayDao) {

    enum class LibrarySort {
        NEWEST_TO_OLDEST,
        OLDEST_TO_NEWEST,
        TASKS_MOST_TO_LEAST
    }

    data class LibraryDay(
        val date: LocalDate,
        val backgroundSeed: Long,
        val backgroundType: Int,
        val taskCount: Int
    )

    suspend fun getOrCreate(date: LocalDate): Day {
        val key = date.toString()
        val existing = dao.getDay(key)

        if (existing == null) {
            val seed = seedForDayStart(date)
            val type = (seed % 16).toInt()
            dao.upsertDay(DayEntity(key, seed, type))
            return Day(
                date = date,
                tasks = emptyList(),
                backgroundSeed = seed,
                backgroundType = type
            )
        }

        return Day(
            date = date,
            tasks = emptyList(),
            backgroundSeed = existing.backgroundSeed,
            backgroundType = existing.backgroundType
        )
    }

    fun tasks(date: LocalDate): Flow<List<Task>> =
        dao.tasksForDay(date.toString()).map { list ->
            list.map { e ->
                Task(
                    id = UUID.fromString(e.id),
                    title = e.title,
                    isDone = e.isDone,
                    dueTime = e.dueTime?.let(LocalTime::parse)
                )
            }
        }

    suspend fun upsertTask(date: LocalDate, task: Task, order: Int) {
        dao.upsertTask(
            TaskEntity(
                id = task.id.toString(),
                dayDate = date.toString(),
                title = task.title,
                isDone = task.isDone,
                dueTime = task.dueTime?.toString(),
                orderIndex = order
            )
        )
    }

    suspend fun deleteTask(id: UUID) {
        dao.deleteTask(id.toString())
    }

    suspend fun updateBackground(date: LocalDate, seed: Long, type: Int) {
        dao.upsertDay(DayEntity(date.toString(), seed, type))
    }

    fun libraryDays(sort: LibrarySort): Flow<List<LibraryDay>> {
        val flow = when (sort) {
            LibrarySort.NEWEST_TO_OLDEST -> dao.libraryDaysNewest()
            LibrarySort.OLDEST_TO_NEWEST -> dao.libraryDaysOldest()
            LibrarySort.TASKS_MOST_TO_LEAST -> dao.libraryDaysMostTasks()
        }
        return flow.map { rows ->
            rows.map { r ->
                LibraryDay(
                    date = LocalDate.parse(r.date),
                    backgroundSeed = r.backgroundSeed,
                    backgroundType = r.backgroundType,
                    taskCount = r.taskCount
                )
            }
        }
    }

    suspend fun previousDayWithTasks(fromDate: LocalDate): LocalDate? {
        val s = dao.previousDayWithTasks(fromDate.toString()) ?: return null
        return LocalDate.parse(s)
    }

    // -------------------------
    // Widget helpers
    // -------------------------

    suspend fun uncheckedTasksForToday(date: LocalDate): List<Task> {
        return dao.uncheckedTasksForDay(date.toString()).map { e ->
            Task(
                id = UUID.fromString(e.id),
                title = e.title,
                isDone = e.isDone,
                dueTime = e.dueTime?.let(LocalTime::parse)
            )
        }
    }

    suspend fun setTaskDone(taskId: UUID, done: Boolean) {
        dao.setTaskDone(taskId.toString(), done)
    }

    private fun seedForDayStart(date: LocalDate): Long {
        return date.toEpochDay() * 1103515245L + 12345L
    }
}
