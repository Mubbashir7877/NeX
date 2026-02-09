package com.pck.nex.domain.model

data class TemplateTask(
    val taskId: String,
    val templateId: String,
    val title: String,
    val orderIndex: Int
)
