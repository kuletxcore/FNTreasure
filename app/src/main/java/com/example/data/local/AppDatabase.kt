package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MatchEntity::class,
        ManualEntryEntity::class,
        ObtainedPrizeEntity::class,
        ProcessedFileEntity::class,
        ActivityLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun manualEntryDao(): ManualEntryDao
    abstract fun obtainedPrizeDao(): ObtainedPrizeDao
    abstract fun processedFileDao(): ProcessedFileDao
    abstract fun activityLogDao(): ActivityLogDao
}
