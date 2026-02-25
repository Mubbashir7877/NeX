package com.pck.nex.data.repo

import com.pck.nex.data.db.TemplateDao
import com.pck.nex.data.db.TemplateEntity
import com.pck.nex.data.db.TemplateTaskEntity
import com.pck.nex.domain.model.Template
import com.pck.nex.domain.model.Task
import com.pck.nex.domain.model.TemplateTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class TemplateRepository(
    private val templateDao: TemplateDao,
    private val dayRepository: DayRepository, // reuse your existing logic for add-to-day
) {

    fun observeTemplates(): Flow<List<Template>> =
        templateDao.observeTemplates().map { list -> list.map { it.toDomain() } }

    fun observeTemplateTasks(templateId: String): Flow<List<TemplateTask>> =
        templateDao.observeTemplateTasks(templateId).map { list -> list.map { it.toDomain() } }

    suspend fun createTemplate(name: String): Template {
        val now = System.currentTimeMillis()
        val entity = TemplateEntity(
            templateId = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        templateDao.insertTemplate(entity)
        return entity.toDomain()
    }

    suspend fun renameTemplate(templateId: String, newName: String) {
        val now = System.currentTimeMillis()
        templateDao.renameTemplate(templateId, newName.trim(), now)
    }

    suspend fun deleteTemplate(templateId: String) {
        templateDao.deleteTemplate(templateId)
        // tasks cascade delete via FK
    }

    suspend fun addTask(templateId: String, title: String) {
        val max = templateDao.getMaxOrderIndex(templateId)
        val entity = TemplateTaskEntity(
            taskId = UUID.randomUUID().toString(),
            templateId = templateId,
            title = title.trim(),
            orderIndex = max + 1
        )
        templateDao.upsertTemplateTask(entity)

        touchTemplate(templateId)
    }

    suspend fun addTemplateToDay(templateId: String, targetDate: LocalDate) {
        // Ensure the day exists
        val day = dayRepository.getOrCreate(targetDate)

        // Fetch template tasks
        val templateTasks = templateDao.getTemplateTasksOnce(templateId)
            .sortedBy { it.orderIndex }

        // Determine starting order index
        val existingTasks = dayRepository
            .tasks(targetDate)
            .map { it.size }
            .first()

        var orderIndex = existingTasks

        // Insert tasks
        for (t in templateTasks) {
            dayRepository.upsertTask(
                date = targetDate,
                task = Task(
                    id = UUID.randomUUID(), // new task instance
                    title = t.title,
                    isDone = false,
                    dueTime = null
                ),
                order = orderIndex++
            )
        }
    }
    suspend fun deleteTask(taskId: String, templateId: String) {
        templateDao.deleteTemplateTask(taskId)
        touchTemplate(templateId)
        // (optional) you can compact orderIndex after deletes; not required for correctness
    }

    suspend fun updateTaskTitle(taskId: String, templateId: String, newTitle: String) {
        val tasks = templateDao.getTemplateTasksOnce(templateId)
        val t = tasks.firstOrNull { it.taskId == taskId } ?: return
        templateDao.upsertTemplateTask(t.copy(title = newTitle.trim()))
        touchTemplate(templateId)
    }
    suspend fun getTemplateNameSync(templateId: String): String? {
        return templateDao.getTemplate(templateId)?.name
    }

    /**
     * Copies template tasks into a chosen day using your existing DayRepository/TaskEntity model.
     * You already have the add/update mechanics and widget refresh.
     */


    private suspend fun touchTemplate(templateId: String) {
        val t = templateDao.getTemplateById(templateId) ?: return
        val now = System.currentTimeMillis()
        templateDao.updateTemplate(t.copy(updatedAtEpochMs = now))
    }
    suspend fun getTemplateTasksSync(
        templateId: String
    ): List<TemplateTask> {
        return templateDao.getTemplateTasksOnce(templateId)
            .map {
                TemplateTask(
                    taskId = it.taskId,
                    templateId = it.templateId,
                    title = it.title,
                    orderIndex = it.orderIndex
                )
            }
    }


    private fun TemplateEntity.toDomain() =
        Template(templateId, name, createdAtEpochMs, updatedAtEpochMs)

    private fun TemplateTaskEntity.toDomain() =
        TemplateTask(taskId, templateId, title, orderIndex)
}
