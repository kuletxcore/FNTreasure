package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY matchStartEpochMs DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches")
    suspend fun getAll(): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MatchEntity>)

    @Query("DELETE FROM matches WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()
}

@Dao
interface ManualEntryDao {
    @Query("SELECT * FROM manual_entries")
    fun observeAll(): Flow<List<ManualEntryEntity>>

    @Query("SELECT * FROM manual_entries WHERE profileKey = :profileKey")
    suspend fun getForProfile(profileKey: String): List<ManualEntryEntity>

    @Query("SELECT * FROM manual_entries")
    suspend fun getAll(): List<ManualEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ManualEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<ManualEntryEntity>)

    @Query("DELETE FROM manual_entries WHERE profileKey = :profileKey")
    suspend fun deleteForProfile(profileKey: String)

    @Query("DELETE FROM manual_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM manual_entries")
    suspend fun deleteAll()
}

@Dao
interface ObtainedPrizeDao {
    @Query("SELECT * FROM obtained_prizes")
    fun observeAll(): Flow<List<ObtainedPrizeEntity>>

    @Query("SELECT * FROM obtained_prizes")
    suspend fun getAll(): List<ObtainedPrizeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ObtainedPrizeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ObtainedPrizeEntity>)

    @Query("DELETE FROM obtained_prizes WHERE profileKey = :profileKey AND prizeId = :prizeId")
    suspend fun delete(profileKey: String, prizeId: String)

    @Query("DELETE FROM obtained_prizes")
    suspend fun deleteAll()
}

@Dao
interface ProcessedFileDao {
    @Query("SELECT * FROM processed_files")
    suspend fun getAll(): List<ProcessedFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProcessedFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProcessedFileEntity>)

    @Query("DELETE FROM processed_files")
    suspend fun deleteAll()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_log ORDER BY id DESC LIMIT 100")
    fun observeRecent(): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insert(entry: ActivityLogEntity)

    @Query("DELETE FROM activity_log WHERE id NOT IN (SELECT id FROM activity_log ORDER BY id DESC LIMIT 100)")
    suspend fun trimOld()

    @Query("DELETE FROM activity_log")
    suspend fun deleteAll()
}
