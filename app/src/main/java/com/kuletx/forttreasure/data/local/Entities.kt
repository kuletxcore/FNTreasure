package com.kuletx.forttreasure.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val sessionId: String,
    val profileKey: String,
    val matchStartEpochMs: Long,
    val payloadJson: String,
)

@Entity(tableName = "manual_entries")
data class ManualEntryEntity(
    @PrimaryKey val id: String,
    val profileKey: String,
    val chests: Int,
    val addedUtc: String?,
    val kind: String,
    val careerMatches: Int?,
    val weekFromUtc: String?,
)

@Entity(tableName = "obtained_prizes", primaryKeys = ["profileKey", "prizeId"])
data class ObtainedPrizeEntity(
    val profileKey: String,
    val prizeId: String,
)

@Entity(tableName = "processed_files")
data class ProcessedFileEntity(
    @PrimaryKey val fileKey: String,
    val result: String,
)

@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val text: String,
    val level: String,
)
