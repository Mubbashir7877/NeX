package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_tasks",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["templateId", "orderIndex"], unique = true)
    ]
)
data class TemplateTaskEntity(
    @PrimaryKey val taskId: String,      // UUID string
    val templateId: String,              // FK
    val title: String,
    val orderIndex: Int
)
