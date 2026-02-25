package com.pck.nex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DayEntity::class,
        TaskEntity::class,
        TemplateEntity::class,
        TemplateTaskEntity::class,
    ]
    ,
    version = 4,
    exportSchema = false
)
abstract class NeXDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun templateDao(): TemplateDao

}
