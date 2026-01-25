package com.pck.nex.data.repo

import com.pck.nex.data.db.*
import com.pck.nex.domain.model.Day
import com.pck.nex.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DayRepository(private val dao: DayDao) {

    suspend fun getOrCreate(date: LocalDate): Day {
        val key = date.toString()
        val existing = dao.getDay(key)

        if (existing == null) {
            val seed = date.toEpochDay() * 1103515245L + 12345L
            val type = (seed % 16).toInt()
            dao.upsertDay(DayEntity(key, seed, type))
            return Day(date, emptyList(), backgroundSeed = seed, backgroundType = type)
        }

        return Day(
            date,
            emptyList(),
            backgroundSeed = existing.backgroundSeed,
            backgroundType = existing.backgroundType
        )
    }

    fun tasks(date: LocalDate): Flow<List<Task>> =
        dao.tasksForDay(date.toString()).map {
            it.map { e ->
                Task(
                    UUID.fromString(e.id),
                    e.title,
                    e.isDone,
                    e.dueTime?.let(LocalTime::parse)
                )
            }
        }

    suspend fun upsertTask(date: LocalDate, task: Task, order: Int) {
        dao.upsertTask(
            TaskEntity(
                task.id.toString(),
                date.toString(),
                task.title,
                task.isDone,
                task.dueTime?.toString(),
                order
            )
        )
    }

    suspend fun deleteTask(id: UUID) {
        dao.deleteTask(id.toString())
    }

    suspend fun updateBackground(date: LocalDate, seed: Long, type: Int) {
        dao.upsertDay(DayEntity(date.toString(), seed, type))
    }
}
