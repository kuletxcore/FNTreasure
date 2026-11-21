package com.kuletx.forttreasure.data.local

import android.content.Context
import android.net.Uri
import com.kuletx.forttreasure.domain.ActivityLevel
import com.kuletx.forttreasure.domain.ActivityLogEntry
import com.kuletx.forttreasure.domain.ChestKinds
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.LiveMatchState
import com.kuletx.forttreasure.domain.ManualEntryDto
import com.kuletx.forttreasure.domain.MatchRecordDto
import com.kuletx.forttreasure.domain.OverlayModel
import com.kuletx.forttreasure.domain.Profiles
import com.kuletx.forttreasure.domain.TrackerDataJson
import com.kuletx.forttreasure.domain.TreasureCatalog
import com.kuletx.forttreasure.domain.TreasureFindDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ChestCounterRepository(
    private val context: Context,
    private val db: AppDatabase,
    val settings: SettingsRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val errorLogFile = File(context.filesDir, "errors.log")

    val matchesFlow: Flow<List<MatchRecordDto>> = db.matchDao().observeAll().map { rows ->
        rows.mapNotNull { row ->
            try {
                json.decodeFromString<MatchRecordDto>(row.payloadJson)
            } catch (_: Exception) {
                null
            }
        }
    }

    val manualEntriesFlow: Flow<Map<String, List<ManualEntryDto>>> =
        db.manualEntryDao().observeAll().map { rows ->
            rows.groupBy { it.profileKey }.mapValues { (_, list) ->
                list.map {
                    ManualEntryDto(
                        id = it.id,
                        chests = it.chests,
                        addedUtc = it.addedUtc,
                        kind = it.kind,
                        careerMatches = it.careerMatches,
                        weekFromUtc = it.weekFromUtc,
                    )
                }
            }
        }

    val obtainedPrizesFlow: Flow<Map<String, List<String>>> =
        db.obtainedPrizeDao().observeAll().map { rows ->
            rows.groupBy { it.profileKey }.mapValues { (_, list) -> list.map { it.prizeId } }
        }

    val activityLogFlow: Flow<List<ActivityLogEntry>> =
        db.activityLogDao().observeRecent().map { rows ->
            rows.map { row ->
                ActivityLogEntry(
                    id = row.id,
                    timestampEpochMs = row.timestampEpochMs,
                    text = row.text,
                    level = runCatching { ActivityLevel.valueOf(row.level) }.getOrDefault(ActivityLevel.NORMAL),
                )
            }
        }

    suspend fun log(text: String, level: ActivityLevel = ActivityLevel.NORMAL) {
        db.activityLogDao().insert(
            ActivityLogEntity(
                timestampEpochMs = System.currentTimeMillis(),
                text = text,
                level = level.name,
            )
        )
        db.activityLogDao().trimOld()
    }

    suspend fun appendErrorLog(header: String, detail: String) = withContext(Dispatchers.IO) {
        try {
            errorLogFile.appendText("[${Instant.now()}] $header: $detail\n")
        } catch (_: Exception) {
        }
    }

    suspend fun readErrorLog(): String = withContext(Dispatchers.IO) {
        if (errorLogFile.exists()) errorLogFile.readText() else ""
    }

    suspend fun refreshCatalog(lang: String): Boolean = withContext(Dispatchers.IO) {
        val updated = TreasureCatalog.refreshBlocking(context.filesDir)
        if (updated) {
            log(
                L.t(
                    lang,
                    "Catalogue mis à jour (révision ${TreasureCatalog.data.revision})",
                    "Catalog updated (revision ${TreasureCatalog.data.revision})",
                    "Catálogo actualizado (revisión ${TreasureCatalog.data.revision})",
                ),
                ActivityLevel.INFO,
            )
        }
        updated
    }

    suspend fun setObtained(profileKey: String, prizeId: String, on: Boolean) {
        if (on) {
            db.obtainedPrizeDao().upsert(ObtainedPrizeEntity(profileKey, prizeId))
            if (profileKey.isNotEmpty()) {
                db.obtainedPrizeDao().delete("", prizeId)
            }
        } else {
            db.obtainedPrizeDao().delete(profileKey, prizeId)
            if (profileKey.isNotEmpty()) {
                db.obtainedPrizeDao().delete("", prizeId)
            }
        }
    }

    suspend fun addManual(
        profileKey: String,
        chests: Int,
        careerMatches: Int? = null,
        weekFromUtc: Instant? = null,
    ) {
        val existing = getSettledHistory(profileKey).toMutableList()
        if (careerMatches != null) {
            existing.removeAll { it.kind == ManualEntryDto.ESTIMATE }
        }
        existing.add(Profiles.newManualEntry(chests, careerMatches, weekFromUtc))
        saveManualHistory(profileKey, existing)
    }

    suspend fun removeManual(profileKey: String, entryId: String): Boolean {
        val existing = getSettledHistory(profileKey).toMutableList()
        val removed = existing.removeAll { it.id == entryId }
        if (removed) {
            saveManualHistory(profileKey, existing)
        }
        return removed
    }

    private suspend fun getSettledHistory(profileKey: String): List<ManualEntryDto> {
        val profileRows = db.manualEntryDao().getForProfile(profileKey)
        val earlyRows = if (profileKey.isNotEmpty()) db.manualEntryDao().getForProfile("") else emptyList()
        if (profileKey.isNotEmpty() && earlyRows.isNotEmpty()) {
            db.manualEntryDao().deleteForProfile("")
        }
        val combined = (profileRows + earlyRows).map {
            ManualEntryDto(
                id = it.id,
                chests = it.chests,
                addedUtc = it.addedUtc,
                kind = it.kind,
                careerMatches = it.careerMatches,
                weekFromUtc = it.weekFromUtc,
            )
        }
        return Profiles.reconcileHistory(combined, combined.sumOf { it.chests })
    }

    private suspend fun saveManualHistory(profileKey: String, list: List<ManualEntryDto>) {
        db.manualEntryDao().deleteForProfile(profileKey)
        db.manualEntryDao().upsertAll(
            list.map {
                ManualEntryEntity(
                    id = it.id,
                    profileKey = profileKey,
                    chests = it.chests,
                    addedUtc = it.addedUtc,
                    kind = it.kind,
                    careerMatches = it.careerMatches,
                    weekFromUtc = it.weekFromUtc,
                )
            }
        )
    }

    suspend fun updateMatchChests(sessionId: String, chestsOpened: Int) {
        val existing = db.matchDao().getAll().firstOrNull { it.sessionId == sessionId } ?: return
        val dto = json.decodeFromString<MatchRecordDto>(existing.payloadJson)
        val updated = dto.copy(chestsOpened = maxOf(0, chestsOpened))
        db.matchDao().upsert(
            existing.copy(payloadJson = json.encodeToString(updated))
        )
    }

    suspend fun updateMatchDetails(
        sessionId: String,
        chestsOpened: Int,
        treasureFinds: List<TreasureFindDto>,
        matchNumber: Int? = null,
        sessionNumber: Int? = null,
        profileKey: String = "",
        lang: String = "en",
    ) {
        val existing = db.matchDao().getAll().firstOrNull { it.sessionId == sessionId } ?: return
        val dto = json.decodeFromString<MatchRecordDto>(existing.payloadJson)
        val updated = dto.copy(
            chestsOpened = maxOf(0, chestsOpened),
            treasureFinds = treasureFinds,
            matchNumber = matchNumber ?: dto.matchNumber,
            sessionNumber = sessionNumber ?: dto.sessionNumber,
        )
        db.matchDao().upsert(
            existing.copy(payloadJson = json.encodeToString(updated))
        )
        val effectiveProfile = existing.profileKey.ifBlank { profileKey }
        treasureFinds.forEach { find ->
            val pid = find.prizeId
            if (!pid.isNullOrBlank()) {
                val prize = TreasureCatalog.find(pid)
                if (prize != null) {
                    setObtained(effectiveProfile, prize.id, true)
                }
            }
        }
        val mLabel = updated.matchNumber?.let { "Match #$it" } ?: updated.replayFile
        log(
            L.t(
                lang,
                "Modifié : $mLabel · ${updated.chestsOpened} coffres · ★ ${treasureFinds.size}",
                "Updated: $mLabel · ${updated.chestsOpened} chests · ★ ${treasureFinds.size}",
                "Actualizado: $mLabel · ${updated.chestsOpened} cofres · ★ ${treasureFinds.size}",
            ),
            ActivityLevel.INFO,
        )
    }

    suspend fun incrementLiveChest(delta: Int, lang: String) {
        settings.ensureSessionInitialized()
        settings.updateLiveMatch { cur ->
            val nowIso = cur.startedUtc ?: Instant.now().toString()
            val nextChests = (cur.chests + delta).coerceAtLeast(0)
            cur.copy(
                active = true,
                chests = nextChests,
                startedUtc = nowIso,
            )
        }
    }

    suspend fun addLiveTreasure(
        itemName: String,
        prizeId: String?,
        fromGoldenChest: Boolean,
        profileKey: String,
        lang: String,
    ) {
        settings.ensureSessionInitialized()
        var started: Instant? = null
        settings.updateLiveMatch { cur ->
            val startStr = cur.startedUtc ?: Instant.now().toString()
            started = L.parseUtc(startStr)
            val elapsedSec = started?.let {
                Duration.between(it, Instant.now()).seconds.coerceAtLeast(0L).toFloat()
            }
            val find = TreasureFindDto(
                item = itemName,
                rawItem = prizeId ?: itemName,
                prizeId = prizeId,
                fromGoldenChest = fromGoldenChest,
                timeExact = true,
                obtainedAtSeconds = elapsedSec,
                sourceKind = ChestKinds.NORMAL,
                sourceIsChest = true,
                sourceCertain = true,
            )
            cur.copy(
                active = true,
                treasures = cur.treasures + find,
                startedUtc = startStr,
            )
        }
        if (!prizeId.isNullOrBlank()) {
            TreasureCatalog.find(prizeId)?.let { prize ->
                setObtained(profileKey, prize.id, true)
            }
        }
        val display = if (!prizeId.isNullOrBlank()) {
            com.kuletx.forttreasure.domain.TreasureNames.friendly(prizeId, lang)
        } else {
            L.name(itemName, lang)
        }
        log("★ $display", ActivityLevel.TREASURE)
    }

    suspend fun removeLiveTreasure(index: Int) {
        settings.updateLiveMatch { cur ->
            if (index !in cur.treasures.indices) cur
            else cur.copy(treasures = cur.treasures.toMutableList().apply { removeAt(index) })
        }
    }

    suspend fun finishLiveMatch(
        profileKey: String,
        playerName: String?,
        lang: String,
        playlist: String = "experience_br",
    ): MatchRecordDto? {
        settings.ensureSessionInitialized()
        val cfg = settings.configFlow.first()
        val live = settings.liveMatchFlow.first()
        if (!live.active && live.chests == 0 && live.treasures.isEmpty()) {
            return null
        }

        val allMatches = db.matchDao().getAll().mapNotNull {
            runCatching { json.decodeFromString<MatchRecordDto>(it.payloadJson) }.getOrNull()
        }
        val counted = Profiles.counted(allMatches)
        val filteredForProfile = if (profileKey.isNotBlank()) {
            counted.filter { Profiles.key(it) == profileKey }
        } else {
            counted
        }
        val sessionMatches = filteredForProfile.filter {
            OverlayModel.isInSession(it, cfg.sessionNumber, cfg.sessionStartedEpochMs)
        }
        val matchNumber = OverlayModel.nextMatchNumber(sessionMatches)

        val now = Instant.now()
        val startInst = L.parseUtc(live.startedUtc) ?: now
        val durSec = Duration.between(startInst, now).seconds.coerceAtLeast(1L).toInt()
        val sessionId = "live_${now.toEpochMilli()}_${UUID.randomUUID().toString().take(6)}"

        val effectivePlayer = playerName?.takeIf { it.isNotBlank() } ?: profileKey.takeIf { it.isNotBlank() }
        val match = MatchRecordDto(
            sessionId = sessionId,
            replayFile = "Match #$matchNumber (Session #${cfg.sessionNumber})",
            analyzedAtUtc = now.toString(),
            matchStartUtc = startInst.toString(),
            durationSeconds = durSec,
            playlist = playlist,
            playerName = effectivePlayer,
            epicId = profileKey.takeIf { it.isNotBlank() && it != effectivePlayer },
            chestsOpened = live.chests,
            chests = List(live.chests) { idx ->
                com.kuletx.forttreasure.domain.ChestRecordDto(
                    timeSeconds = (idx + 1).toFloat(),
                    kind = ChestKinds.NORMAL,
                )
            },
            treasureFinds = live.treasures,
            sessionNumber = cfg.sessionNumber,
            matchNumber = matchNumber,
            notes = L.t(
                lang,
                "Partie $matchNumber · Session ${cfg.sessionNumber} (compteur en direct)",
                "Match $matchNumber · Session ${cfg.sessionNumber} (live counter)",
                "Partida $matchNumber · Sesión ${cfg.sessionNumber} (contador en directo)",
            ),
        )

        db.matchDao().upsert(
            MatchEntity(
                sessionId = sessionId,
                profileKey = Profiles.key(match),
                matchStartEpochMs = startInst.toEpochMilli(),
                payloadJson = json.encodeToString(match),
            )
        )
        live.treasures.forEach { find ->
            val pid = find.prizeId
            if (!pid.isNullOrBlank()) {
                TreasureCatalog.find(pid)?.let { prize ->
                    setObtained(Profiles.key(match), prize.id, true)
                }
            }
        }
        settings.clearLiveMatch()

        val tSummary = if (live.treasures.isNotEmpty()) {
            " · ★ " + live.treasures.joinToString(", ") { it.displayName(lang) }
        } else {
            ""
        }
        log(
            L.t(
                lang,
                "Partie $matchNumber (Session ${cfg.sessionNumber}) : ${L.count(lang, live.chests, "coffre", "chest", "cofre")}$tSummary",
                "Match $matchNumber (Session ${cfg.sessionNumber}): ${L.count(lang, live.chests, "coffre", "chest", "cofre")}$tSummary",
                "Partida $matchNumber (Sesión ${cfg.sessionNumber}): ${L.count(lang, live.chests, "coffre", "chest", "cofre")}$tSummary",
            ),
            ActivityLevel.OK,
        )
        return match
    }

    suspend fun resetLiveMatch(lang: String) {
        settings.clearLiveMatch()
        log(
            L.t(
                lang,
                "Compteur de la partie en cours réinitialisé.",
                "Current live match reset.",
                "Contador de partida actual reiniciado.",
            ),
            ActivityLevel.INFO,
        )
    }

    suspend fun startNewSession(lang: String, clearLive: Boolean = true) {
        if (clearLive) {
            settings.clearLiveMatch()
        }
        settings.startNewSession()
        val cfg = settings.configFlow.first()
        log(
            L.t(
                lang,
                "Nouvelle session #${cfg.sessionNumber} démarrée (Partie 1).",
                "New Session #${cfg.sessionNumber} started (Match 1).",
                "Nueva sesión #${cfg.sessionNumber} iniciada (Partida 1).",
            ),
            ActivityLevel.INFO,
        )
    }

    suspend fun deleteMatch(sessionId: String) {
        db.matchDao().deleteBySessionId(sessionId)
    }

    suspend fun importDataJson(uri: Uri, lang: String): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Cannot open file")
        val parsed = json.decodeFromString<TrackerDataJson>(text)

        val matchEntities = parsed.matches.map { m ->
            val whenMs = (L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH).toEpochMilli()
            MatchEntity(
                sessionId = m.sessionId.ifBlank { "${m.replayFile}_$whenMs" },
                profileKey = Profiles.key(m),
                matchStartEpochMs = whenMs,
                payloadJson = json.encodeToString(m),
            )
        }
        db.matchDao().upsertAll(matchEntities)

        val manualEntities = mutableListOf<ManualEntryEntity>()
        val allProfileKeys = (parsed.manualHistory.keys + parsed.manualChests.keys).distinct()
        for (pKey in allProfileKeys) {
            val baseList = parsed.manualHistory[pKey].orEmpty()
            val targetTotal = parsed.manualChests[pKey] ?: baseList.sumOf { it.chests }
            val reconciled = Profiles.reconcileHistory(baseList, targetTotal)
            for (e in reconciled) {
                manualEntities.add(
                    ManualEntryEntity(
                        id = "${pKey}_${e.id}".ifBlank { e.id },
                        profileKey = pKey,
                        chests = e.chests,
                        addedUtc = e.addedUtc,
                        kind = e.kind,
                        careerMatches = e.careerMatches,
                        weekFromUtc = e.weekFromUtc,
                    )
                )
            }
        }
        if (manualEntities.isNotEmpty()) {
            db.manualEntryDao().upsertAll(manualEntities)
        }

        val obtainedEntities = parsed.obtained.flatMap { (pKey, ids) ->
            ids.map { id -> ObtainedPrizeEntity(pKey, id) }
        }
        if (obtainedEntities.isNotEmpty()) {
            db.obtainedPrizeDao().upsertAll(obtainedEntities)
        }

        val processedEntities = parsed.processedFiles.entries.map { (k, v) -> ProcessedFileEntity(k, v) }
        if (processedEntities.isNotEmpty()) {
            db.processedFileDao().upsertAll(processedEntities)
        }

        log(
            L.t(
                lang,
                "Importé : ${L.count(lang, matchEntities.size, "partie", "match", "partida")} depuis data.json",
                "Imported ${L.count(lang, matchEntities.size, "partie", "match", "partida")} from data.json",
                "Importadas ${L.count(lang, matchEntities.size, "partie", "match", "partida")} desde data.json",
            ),
            ActivityLevel.OK,
        )
        matchEntities.size
    }

    suspend fun exportDataJson(): String = withContext(Dispatchers.IO) {
        val matches = db.matchDao().getAll().mapNotNull {
            runCatching { json.decodeFromString<MatchRecordDto>(it.payloadJson) }.getOrNull()
        }
        val manualRows = db.manualEntryDao().getAll()
        val manualByProfile = manualRows.groupBy { it.profileKey }.mapValues { (_, list) ->
            list.map {
                ManualEntryDto(
                    id = it.id,
                    chests = it.chests,
                    addedUtc = it.addedUtc,
                    kind = it.kind,
                    careerMatches = it.careerMatches,
                    weekFromUtc = it.weekFromUtc,
                )
            }
        }
        val manualSums = manualByProfile.mapValues { (_, list) -> list.sumOf { it.chests } }
        val obtained = db.obtainedPrizeDao().getAll().groupBy { it.profileKey }.mapValues { (_, list) -> list.map { it.prizeId } }
        val processed = db.processedFileDao().getAll().associate { it.fileKey to it.result }

        val data = TrackerDataJson(
            version = 1,
            analyzerVersion = 17,
            createdUtc = Instant.now().toString(),
            matches = matches,
            processedFiles = processed,
            manualChests = manualSums,
            manualHistory = manualByProfile,
            obtained = obtained,
        )
        json.encodeToString(data)
    }

    suspend fun clearAllData(lang: String) {
        db.matchDao().deleteAll()
        db.manualEntryDao().deleteAll()
        db.obtainedPrizeDao().deleteAll()
        db.processedFileDao().deleteAll()
        db.activityLogDao().deleteAll()
        withContext(Dispatchers.IO) {
            if (errorLogFile.exists()) errorLogFile.delete()
        }
        log(
            L.t(lang, "Historique effacé.", "History cleared.", "Historial borrado."),
            ActivityLevel.INFO,
        )
    }
}
