package com.pck.nex.domain.model

data class Template(
    val templateId: String,
    val name: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
