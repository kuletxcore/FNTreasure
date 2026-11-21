package com.example.data.local

import android.content.Context
import androidx.room.Room
import com.example.domain.TreasureCatalog

object AppContainer {
    @Volatile
    private var repoInstance: ChestCounterRepository? = null

    fun repository(context: Context): ChestCounterRepository {
        return repoInstance ?: synchronized(this) {
            repoInstance ?: run {
                val appCtx = context.applicationContext
                TreasureCatalog.init(appCtx)
                val db = Room.databaseBuilder(
                    appCtx,
                    AppDatabase::class.java,
                    "chest_counter.db",
                ).build()
                val settings = SettingsRepository(appCtx)
                ChestCounterRepository(appCtx, db, settings).also {
                    repoInstance = it
                }
            }
        }
    }
}
