package com.kuletx.forttreasure.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChestRecordDto(
    @SerialName("TimeSeconds") val timeSeconds: Float = 0f,
    @SerialName("Kind") val kind: String = "",
    @SerialName("Blueprint") val blueprint: String = "",
    @SerialName("Destroyed") val destroyed: Boolean = false,
    @SerialName("Items") val items: List<String> = emptyList(),
    @SerialName("TreasureItems") val treasureItems: List<String> = emptyList(),
)

@Serializable
data class TreasureFindDto(
    @SerialName("Item") val item: String = "",
    @SerialName("RawItem") val rawItem: String = "",
    @SerialName("PrizeId") val prizeId: String? = null,
    @SerialName("FromGoldenChest") val fromGoldenChest: Boolean = false,
    @SerialName("FromPrizeHandler") val fromPrizeHandler: Boolean = false,
    @SerialName("TimeExact") val timeExact: Boolean = false,
    @SerialName("SourceDestroyed") val sourceDestroyed: Boolean = false,
    @SerialName("ObtainedAtSeconds") val obtainedAtSeconds: Float? = null,
    @SerialName("SourceKind") val sourceKind: String = "",
    @SerialName("SourceBlueprint") val sourceBlueprint: String = "",
    @SerialName("SourceIsChest") val sourceIsChest: Boolean = true,
    @SerialName("SourceCertain") val sourceCertain: Boolean = false,
    @SerialName("AlsoPossible") val alsoPossible: List<String> = emptyList(),
    @SerialName("Teammate") val teammate: String? = null,
) {
    fun displayName(lang: String): String =
        if (prizeId != null) TreasureNames.friendly(prizeId, lang) else L.name(item, lang)

    fun sourceLabel(lang: String): String =
        if (sourceDestroyed) "${L.name(sourceKind, lang)} (${L.t(lang, "détruit", "destroyed", "destruido")})"
        else L.name(sourceKind, lang)
}

@Serializable
data class ManualEntryDto(
    @SerialName("Id") val id: String = "",
    @SerialName("Chests") val chests: Int = 0,
    @SerialName("AddedUtc") val addedUtc: String? = null,
    @SerialName("Kind") val kind: String = HAND,
    @SerialName("CareerMatches") val careerMatches: Int? = null,
    @SerialName("WeekFromUtc") val weekFromUtc: String? = null,
) {
    companion object {
        const val HAND = "hand"
        const val ESTIMATE = "estimate"
        const val EARLIER = "earlier"
    }
}

@Serializable
data class TeamChestCountDto(
    @SerialName("Name") val name: String = "",
    @SerialName("EpicId") val epicId: String? = null,
    @SerialName("IsBot") val isBot: Boolean = false,
    @SerialName("Chests") val chests: Int = 0,
    @SerialName("Destroyed") val destroyed: Int = 0,
)

@Serializable
data class SeenPrizeDto(
    @SerialName("Player") val player: String = "",
    @SerialName("PrizeId") val prizeId: String = "",
    @SerialName("TimeSeconds") val timeSeconds: Float = 0f,
)

@Serializable
data class MatchRecordDto(
    @SerialName("SessionId") val sessionId: String = "",
    @SerialName("ReplayFile") val replayFile: String = "",
    @SerialName("AnalyzedAtUtc") val analyzedAtUtc: String = "",
    @SerialName("MatchStartUtc") val matchStartUtc: String? = null,
    @SerialName("DurationSeconds") val durationSeconds: Int? = null,
    @SerialName("Playlist") val playlist: String? = null,
    @SerialName("GameVersion") val gameVersion: String? = null,
    @SerialName("PlayerName") val playerName: String? = null,
    @SerialName("EpicId") val epicId: String? = null,
    @SerialName("TrackedByUsername") val trackedByUsername: Boolean = false,
    @SerialName("NotTrackedOwner") val notTrackedOwner: String? = null,
    @SerialName("IsReplayOwner") val isReplayOwner: Boolean = false,
    @SerialName("Placement") val placement: Int? = null,
    @SerialName("TotalPlayers") val totalPlayers: Int? = null,
    @SerialName("Eliminations") val eliminations: Int? = null,
    @SerialName("ChestsOpened") val chestsOpened: Int = 0,
    @SerialName("AmmoBoxesOpened") val ammoBoxesOpened: Int = 0,
    @SerialName("OtherContainersOpened") val otherContainersOpened: Int = 0,
    @SerialName("OtherInteractions") val otherInteractions: Map<String, Int> = emptyMap(),
    @SerialName("AnalyzerVersion") val analyzerVersion: Int = 17,
    @SerialName("IsBattleRoyale") val isBattleRoyale: Boolean = true,
    @SerialName("Map") val map: String? = null,
    @SerialName("IsCustomMatch") val isCustomMatch: Boolean = false,
    @SerialName("Chests") val chests: List<ChestRecordDto> = emptyList(),
    @SerialName("TreasureFinds") val treasureFinds: List<TreasureFindDto> = emptyList(),
    @SerialName("TeamTreasureFinds") val teamTreasureFinds: List<TreasureFindDto> = emptyList(),
    @SerialName("SeenPrizes") val seenPrizes: List<SeenPrizeDto> = emptyList(),
    @SerialName("TeamChests") val teamChests: List<TeamChestCountDto> = emptyList(),
    @SerialName("TeamChestsOpened") val teamChestsOpened: Int = 0,
    @SerialName("VehicleSeconds") val vehicleSeconds: Int = 0,
    @SerialName("VehicleMeters") val vehicleMeters: Int = 0,
    @SerialName("UnrecordedVehicleLoot") val unrecordedVehicleLoot: Int = 0,
    // Android-specific optional metadata preserved cleanly without breaking desktop schema
    @SerialName("Notes") val notes: String? = null,
    @SerialName("SessionNumber") val sessionNumber: Int? = null,
    @SerialName("MatchNumber") val matchNumber: Int? = null,
) {
    val vehicleChestsMissing: Boolean get() = unrecordedVehicleLoot >= 3
}

@Serializable
data class TrackerDataJson(
    @SerialName("Version") val version: Int = 1,
    @SerialName("AnalyzerVersion") val analyzerVersion: Int = 17,
    @SerialName("CreatedUtc") val createdUtc: String = "",
    @SerialName("Matches") val matches: List<MatchRecordDto> = emptyList(),
    @SerialName("ProcessedFiles") val processedFiles: Map<String, String> = emptyMap(),
    @SerialName("ManualChests") val manualChests: Map<String, Int> = emptyMap(),
    @SerialName("ManualHistory") val manualHistory: Map<String, List<ManualEntryDto>> = emptyMap(),
    @SerialName("Obtained") val obtained: Map<String, List<String>> = emptyMap(),
)

data class TrackerView(
    val matches: List<MatchRecordDto>,
    val manualAdjust: Int,
    val manualEntries: List<ManualEntryDto>,
    val obtainedIds: List<String>,
) {
    val countedChests: Int get() = matches.sumOf { it.chestsOpened }
    val totalChests: Int get() = maxOf(0, countedChests + manualAdjust)
    val teamChests: Int get() = matches.sumOf { it.teamChestsOpened }
    val squadChests: Int get() = totalChests + teamChests
}

data class Profile(
    val key: String,
    val name: String,
    val matches: Int,
    val chests: Int,
    val lastPlayedEpochMs: Long,
)

enum class ActivityLevel { NORMAL, INFO, OK, WARN, ERR, TREASURE, MUTED }

data class ActivityLogEntry(
    val id: Long = 0,
    val timestampEpochMs: Long,
    val text: String,
    val level: ActivityLevel,
)

enum class OverlayPreviewState { IDLE, IN_PROGRESS, LIVE }

data class LiveMatchState(
    val active: Boolean = false,
    val chests: Int = 0,
    val treasures: List<TreasureFindDto> = emptyList(),
    val startedUtc: String? = null,
)

data class AppConfig(
    val language: String = "en",
    val selectedProfile: String? = null,
    val timeZone: String = "local",
    val clockFormat: String = "auto",
    val autoRefreshCatalog: Boolean = true,
    val dismissedNotices: Set<String> = emptySet(),
    val welcomeDismissed: Boolean = false,
    val overlayStyle: String = "hud",
    val overlaySize: Int = 100,
    val overlayOpacity: Int = 100,
    val showSquadTotal: Boolean = false,
    val showSquadInReport: Boolean = false,
    val showTeamTreasuresInReport: Boolean = false,
    val sessionNumber: Int = 1,
    val sessionStartedEpochMs: Long = 0L,
)

object AppInfo {
    const val VERSION_NAME = "1.5.1"
    const val REPO = "kuletxcore/Fortnite-ChestCounter"
    const val UPSTREAM_REPO = "LED-N/Fortnite-ChestCounter"
    const val REPO_URL = "https://github.com/$REPO"
    const val ISSUES_URL = "$REPO_URL/issues"
    const val CONTENT_URL = "https://raw.githubusercontent.com/$REPO/main/content/content.json"
    const val UPSTREAM_CONTENT_URL = "https://raw.githubusercontent.com/$UPSTREAM_REPO/main/content/content.json"
    const val FORTNITE_PACKAGE = "com.epicgames.fortnite"
    const val EXPERIENCE_BR_URL = "https://play.fn.gg/island/experience_br"
}
