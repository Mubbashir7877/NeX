package com.pck.nex.domain.model

import java.time.Instant
import java.time.LocalDate

data class Day(
    val date: LocalDate,
    val tasks: List<Task>,
    val lastEdited:  Long = 0L,
    val backgroundSeed: Long,
    val backgroundType: Int
)
