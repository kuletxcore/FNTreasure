package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppConfig
import com.example.domain.L
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "chest_counter_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val PROFILE = stringPreferencesKey("selected_profile")
        val DEMOS_TREE_URI = stringPreferencesKey("demos_tree_uri")
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
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            language = prefs[Keys.LANGUAGE] ?: L.systemDefault(),
            selectedProfile = prefs[Keys.PROFILE],
            demosTreeUri = prefs[Keys.DEMOS_TREE_URI],
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

    suspend fun setDemosTreeUri(uri: String?) {
        context.dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(Keys.DEMOS_TREE_URI) else it[Keys.DEMOS_TREE_URI] = uri
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

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
