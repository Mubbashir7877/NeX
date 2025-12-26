package com.pck.nex.domain.model

import java.time.LocalTime
import java.util.UUID

data class Task(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val isDone: Boolean = false,
    val dueTime: LocalTime? = null
)
