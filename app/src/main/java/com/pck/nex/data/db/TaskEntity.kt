package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val dayDate: String,
    val title: String,
    val isDone: Boolean,
    val dueTime: String?,
    val orderIndex: Int
)
