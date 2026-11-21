package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.ReplayFileParser
import com.example.data.ReplayParseOutcome
import com.example.domain.ActivityLevel
import com.example.domain.ActivityLogEntry
import com.example.domain.L
import com.example.domain.ManualEntryDto
import com.example.domain.MatchRecordDto
import com.example.domain.Profiles
import com.example.domain.ScanProgress
import com.example.domain.TrackerDataJson
import com.example.domain.TreasureCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

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

    suspend fun scanReplayUris(
        uris: List<Uri>,
        lang: String,
        onProgress: (ScanProgress?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        val processed = db.processedFileDao().getAll().associateBy { it.fileKey }
        var added = 0
        var compressedNoticeShown = false
        onProgress(ScanProgress(0, uris.size))

        uris.forEachIndexed { idx, uri ->
            val name = queryDisplayName(uri) ?: "replay_${idx + 1}.replay"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val key = "$name|${bytes.size}"
                if (!processed.containsKey(key)) {
                    when (val outcome = ReplayFileParser.parse(name, bytes, lang)) {
                        is ReplayParseOutcome.Parsed -> {
                            val m = outcome.match
                            val whenMs = (L.parseUtc(m.matchStartUtc) ?: Instant.now()).toEpochMilli()
                            db.matchDao().upsert(
                                MatchEntity(
                                    sessionId = outcome.sessionId,
                                    profileKey = Profiles.key(m),
                                    matchStartEpochMs = whenMs,
                                    payloadJson = json.encodeToString(m),
                                )
                            )
                            db.processedFileDao().upsert(ProcessedFileEntity(key, outcome.sessionId))
                            added++
                            if (outcome.hasCompressedDataChunks && !compressedNoticeShown) {
                                compressedNoticeShown = true
                                log(
                                    L.t(
                                        lang,
                                        "$name : en-tête et classement lus (coffres en ReplayData compressé Oodle : modifie ou importe data.json pour le détail complet)",
                                        "$name: header & placement parsed (chest actors use Oodle-compressed ReplayData; edit match chests or import desktop data.json)",
                                        "$name: cabecera y puesto leídos (cofres en ReplayData comprimido Oodle; edita o importa data.json)",
                                    ),
                                    ActivityLevel.WARN,
                                )
                            } else {
                                log(
                                    L.t(
                                        lang,
                                        "$name : partie ajoutée",
                                        "$name: match added",
                                        "$name: partida añadida",
                                    ),
                                    ActivityLevel.OK,
                                )
                            }
                        }
                        is ReplayParseOutcome.Skipped -> {
                            db.processedFileDao().upsert(ProcessedFileEntity(key, "skipped"))
                            log("$name: ${outcome.reason}", ActivityLevel.MUTED)
                        }
                        is ReplayParseOutcome.Failed -> {
                            db.processedFileDao().upsert(ProcessedFileEntity(key, "error: ${outcome.error}"))
                            appendErrorLog(name, outcome.error)
                            log("$name: ${outcome.error}", ActivityLevel.ERR)
                        }
                    }
                }
            }
            onProgress(ScanProgress(idx + 1, uris.size))
        }
        onProgress(null)
        if (added == 0 && uris.isNotEmpty()) {
            log(
                L.t(lang, "Aucun nouveau replay trouvé.", "No new replay found.", "No se encontraron repeticiones nuevas."),
                ActivityLevel.INFO,
            )
        }
    }

    suspend fun scanSavedDemosFolder(
        treeUriString: String,
        lang: String,
        onProgress: (ScanProgress?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val treeUri = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return@withContext
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
        val replayUris = dir.listFiles()
            .filter { it.isFile && (it.name?.endsWith(".replay", ignoreCase = true) == true) }
            .map { it.uri }
        if (replayUris.isEmpty()) {
            log(
                L.t(
                    lang,
                    "Aucun fichier .replay dans ce dossier.",
                    "No .replay files found in this folder.",
                    "No hay archivos .replay en esta carpeta.",
                ),
                ActivityLevel.WARN,
            )
            return@withContext
        }
        scanReplayUris(replayUris, lang, onProgress)
    }

    fun persistTreePermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
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
