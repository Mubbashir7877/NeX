package com.pck.nex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    // ---------- Templates ----------
    @Query("SELECT * FROM templates ORDER BY updatedAtEpochMs DESC")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY updatedAtEpochMs DESC")
    suspend fun getTemplatesOnce(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE templateId = :templateId LIMIT 1")
    suspend fun getTemplateById(templateId: String): TemplateEntity?

    @Query("SELECT * FROM templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(entity: TemplateEntity)

    @Update
    suspend fun updateTemplate(entity: TemplateEntity)

    @Query("SELECT * FROM templates WHERE templateId = :id LIMIT 1")
    suspend fun getTemplate(id: String): TemplateEntity?


    @Query("DELETE FROM templates WHERE templateId = :templateId")
    suspend fun deleteTemplate(templateId: String)

    // ---------- Template Tasks ----------
    @Query("SELECT * FROM template_tasks WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun observeTemplateTasks(templateId: String): Flow<List<TemplateTaskEntity>>

    @Query("SELECT * FROM template_tasks WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getTemplateTasksOnce(templateId: String): List<TemplateTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplateTask(entity: TemplateTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplateTasks(entities: List<TemplateTaskEntity>)

    @Query("DELETE FROM template_tasks WHERE taskId = :taskId")
    suspend fun deleteTemplateTask(taskId: String)

    @Query("DELETE FROM template_tasks WHERE templateId = :templateId")
    suspend fun deleteAllTasksForTemplate(templateId: String)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM template_tasks WHERE templateId = :templateId")
    suspend fun getMaxOrderIndex(templateId: String): Int

    // ---------- Convenience ----------
    @Transaction
    suspend fun renameTemplate(templateId: String, newName: String, nowEpochMs: Long) {
        val t = getTemplateById(templateId) ?: return
        updateTemplate(t.copy(name = newName, updatedAtEpochMs = nowEpochMs))
    }
}
