package com.pck.nex.domain.model

import java.time.Instant
import java.time.LocalDate

data class Day(
    val date: LocalDate,
    val tasks: List<Task>,
    val lastEdited: Instant,
    val backgroundSeed: Long? = null,
    val backgroundType: Int? = null
)
