package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "templates",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class TemplateEntity(
    @PrimaryKey val templateId: String, // UUID string
    val name: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
