package com.pck.nex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DayEntity::class, TaskEntity::class],
    version = 1
)
abstract class NeXDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
}
