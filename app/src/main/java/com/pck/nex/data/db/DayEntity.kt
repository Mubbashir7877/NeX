package com.pck.nex.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val backgroundSeed: Long,
    val backgroundType: Int
)
