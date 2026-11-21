package com.kuletx.forttreasure.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuletx.forttreasure.domain.AppConfig
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.LiveMatchState
import com.kuletx.forttreasure.domain.TreasureFindDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

private val Context.dataStore by preferencesDataStore(name = "chest_counter_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val PROFILE = stringPreferencesKey("selected_profile")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_catalog")
        val DISMISSED_NOTICES = stringSetPreferencesKey("dismissed_notices")
        val WELCOME_DISMISSED = booleanPreferencesKey("welcome_dismissed")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
        val OVERLAY_SIZE = intPreferencesKey("overlay_size")
        val OVERLAY_OPACITY = intPreferencesKey("overlay_opacity")
        val SHOW_SQUAD_TOTAL = booleanPreferencesKey("show_squad_total")
        val SHOW_SQUAD_REPORT = booleanPreferencesKey("show_squad_in_report")
        val SHOW_TEAM_TREASURES_REPORT = booleanPreferencesKey("show_team_treasures_in_report")
        val SESSION_NUMBER = intPreferencesKey("session_number")
        val SESSION_STARTED_MS = longPreferencesKey("session_started_ms")
        val LIVE_ACTIVE = booleanPreferencesKey("live_match_active")
        val LIVE_CHESTS = intPreferencesKey("live_match_chests")
        val LIVE_TREASURES_JSON = stringPreferencesKey("live_match_treasures_json")
        val LIVE_STARTED_UTC = stringPreferencesKey("live_match_started_utc")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            language = prefs[Keys.LANGUAGE] ?: L.systemDefault(),
            selectedProfile = prefs[Keys.PROFILE],
            timeZone = prefs[Keys.TIME_ZONE] ?: "local",
            clockFormat = prefs[Keys.CLOCK_FORMAT] ?: "auto",
            autoRefreshCatalog = prefs[Keys.AUTO_REFRESH] ?: true,
            dismissedNotices = prefs[Keys.DISMISSED_NOTICES] ?: emptySet(),
            welcomeDismissed = prefs[Keys.WELCOME_DISMISSED] ?: false,
            overlayStyle = prefs[Keys.OVERLAY_STYLE] ?: "hud",
            overlaySize = prefs[Keys.OVERLAY_SIZE] ?: 100,
            overlayOpacity = prefs[Keys.OVERLAY_OPACITY] ?: 100,
            showSquadTotal = prefs[Keys.SHOW_SQUAD_TOTAL] ?: false,
            showSquadInReport = prefs[Keys.SHOW_SQUAD_REPORT] ?: false,
            showTeamTreasuresInReport = prefs[Keys.SHOW_TEAM_TREASURES_REPORT] ?: false,
            sessionNumber = (prefs[Keys.SESSION_NUMBER] ?: 1).coerceAtLeast(1),
            sessionStartedEpochMs = prefs[Keys.SESSION_STARTED_MS] ?: 0L,
        )
    }

    val liveMatchFlow: Flow<LiveMatchState> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.LIVE_TREASURES_JSON]
        val treasures = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<TreasureFindDto>>(raw) }.getOrDefault(emptyList())
        }
        LiveMatchState(
            active = prefs[Keys.LIVE_ACTIVE] ?: false,
            chests = (prefs[Keys.LIVE_CHESTS] ?: 0).coerceAtLeast(0),
            treasures = treasures,
            startedUtc = prefs[Keys.LIVE_STARTED_UTC],
        )
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = L.normalize(lang) ?: "en" }
    }

    suspend fun setProfile(profileKey: String?) {
        context.dataStore.edit {
            if (profileKey == null) it.remove(Keys.PROFILE) else it[Keys.PROFILE] = profileKey
        }
    }

    suspend fun setTimeZone(tz: String) {
        context.dataStore.edit { it[Keys.TIME_ZONE] = if (tz == "utc") "utc" else "local" }
    }

    suspend fun setClockFormat(fmt: String) {
        context.dataStore.edit { it[Keys.CLOCK_FORMAT] = if (fmt == "12" || fmt == "24") fmt else "auto" }
    }

    suspend fun setAutoRefreshCatalog(on: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_REFRESH] = on }
    }

    suspend fun dismissNotice(id: String) {
        context.dataStore.edit { prefs ->
            val next = (prefs[Keys.DISMISSED_NOTICES] ?: emptySet()) + id
            prefs[Keys.DISMISSED_NOTICES] = next
        }
    }

    suspend fun dismissWelcome() {
        context.dataStore.edit { it[Keys.WELCOME_DISMISSED] = true }
    }

    suspend fun setOverlayStyle(style: String) {
        context.dataStore.edit { it[Keys.OVERLAY_STYLE] = if (style == "classic") "classic" else "hud" }
    }

    suspend fun setOverlaySize(size: Int) {
        context.dataStore.edit { it[Keys.OVERLAY_SIZE] = size }
    }

    suspend fun setOverlayOpacity(opacity: Int) {
        context.dataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity }
    }

    suspend fun setShowSquadTotal(on: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SQUAD_TOTAL] = on }
    }

    suspend fun setShowSquadInReport(on: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SQUAD_REPORT] = on }
    }

    suspend fun setShowTeamTreasuresInReport(on: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TEAM_TREASURES_REPORT] = on }
    }

    suspend fun updateLiveMatch(transform: (LiveMatchState) -> LiveMatchState) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.LIVE_TREASURES_JSON]
            val currentTreasures = if (raw.isNullOrBlank()) emptyList()
            else runCatching { json.decodeFromString<List<TreasureFindDto>>(raw) }.getOrDefault(emptyList())
            val current = LiveMatchState(
                active = prefs[Keys.LIVE_ACTIVE] ?: false,
                chests = (prefs[Keys.LIVE_CHESTS] ?: 0).coerceAtLeast(0),
                treasures = currentTreasures,
                startedUtc = prefs[Keys.LIVE_STARTED_UTC],
            )
            val next = transform(current)
            prefs[Keys.LIVE_ACTIVE] = next.active
            prefs[Keys.LIVE_CHESTS] = next.chests.coerceAtLeast(0)
            prefs[Keys.LIVE_TREASURES_JSON] = json.encodeToString(next.treasures)
            if (next.startedUtc != null) prefs[Keys.LIVE_STARTED_UTC] = next.startedUtc
            else prefs.remove(Keys.LIVE_STARTED_UTC)
        }
    }

    suspend fun clearLiveMatch() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LIVE_ACTIVE] = false
            prefs[Keys.LIVE_CHESTS] = 0
            prefs.remove(Keys.LIVE_TREASURES_JSON)
            prefs.remove(Keys.LIVE_STARTED_UTC)
        }
    }

    suspend fun startNewSession() {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.SESSION_NUMBER] ?: 1).coerceAtLeast(1)
            prefs[Keys.SESSION_NUMBER] = current + 1
            prefs[Keys.SESSION_STARTED_MS] = System.currentTimeMillis()
        }
    }

    suspend fun ensureSessionInitialized() {
        val current = configFlow.first()
        if (current.sessionStartedEpochMs == 0L) {
            context.dataStore.edit { prefs ->
                if ((prefs[Keys.SESSION_STARTED_MS] ?: 0L) == 0L) {
                    prefs[Keys.SESSION_STARTED_MS] = System.currentTimeMillis()
                }
                if ((prefs[Keys.SESSION_NUMBER] ?: 0) < 1) {
                    prefs[Keys.SESSION_NUMBER] = 1
                }
            }
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
