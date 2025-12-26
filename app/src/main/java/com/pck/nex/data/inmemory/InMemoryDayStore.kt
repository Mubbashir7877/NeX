package com.pck.nex.data.inmemory

import com.pck.nex.domain.model.Day
import com.pck.nex.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Temporary in-memory store. Replaced by Room later.
 * Rule: Only one Day per date.
 */
object InMemoryDayStore {
    private val days: MutableMap<LocalDate, Day> = mutableMapOf()

    fun getOrCreate(date: LocalDate): Day {
        return days.getOrPut(date) {
            Day(
                date = date,
                tasks = emptyList(),
                lastEdited = Instant.now(),
                backgroundSeed = null
            )
        }
    }

    fun save(day: Day): Day {
        days[day.date] = day
        return day
    }

    fun upsertTask(date: LocalDate, task: Task): Day {
        val existing = getOrCreate(date)
        val updatedTasks = existing.tasks.toMutableList()
        val idx = updatedTasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) updatedTasks[idx] = task else updatedTasks.add(task)

        val updated = existing.copy(
            tasks = updatedTasks,
            lastEdited = Instant.now()
        )
        return save(updated)
    }

    fun deleteTask(date: LocalDate, taskId: UUID): Day {
        val existing = getOrCreate(date)
        val updated = existing.copy(
            tasks = existing.tasks.filterNot { it.id == taskId },
            lastEdited = Instant.now()
        )
        return save(updated)
    }
}
