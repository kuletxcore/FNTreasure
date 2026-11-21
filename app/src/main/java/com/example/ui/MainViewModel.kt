package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.FortniteLauncher
import com.example.data.FortniteStatus
import com.example.data.local.AppDatabase
import com.example.data.local.ChestCounterRepository
import com.example.data.local.SettingsRepository
import com.example.domain.ActivityLevel
import com.example.domain.ActivityLogEntry
import com.example.domain.AppConfig
import com.example.domain.ChestEstimate
import com.example.domain.L
import com.example.domain.MatchRecordDto
import com.example.domain.Notice
import com.example.domain.OverlayPreviewState
import com.example.domain.OverlayView
import com.example.domain.Profile
import com.example.domain.Profiles
import com.example.domain.ReportExporter
import com.example.domain.ScanProgress
import com.example.domain.TrackerView
import com.example.domain.TreasureCatalog
import com.example.overlay.FloatingOverlayService
import com.example.overlay.OverlayPermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

enum class AppTab { HOME, REPORT, OVERLAY, SETTINGS, HELP }

data class MainUiState(
    val config: AppConfig = AppConfig(),
    val allMatches: List<MatchRecordDto> = emptyList(),
    val view: TrackerView = TrackerView(emptyList(), 0, emptyList(), emptyList()),
    val profiles: List<Profile> = emptyList(),
    val currentProfile: Profile? = null,
    val notices: List<Notice> = emptyList(),
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val scanProgress: ScanProgress? = null,
    val nowUtc: Instant = Instant.now(),
    val fortniteStatus: FortniteStatus = FortniteStatus(false, null),
    val overlayPreviewState: OverlayPreviewState = OverlayPreviewState.IDLE,
    val estimateResult: ChestEstimate.Result? = null,
    val estimateMessage: String? = null,
    val errorLogText: String = "",
    val catalogRevision: Int = 3,
    val isSystem24Hour: Boolean = true,
    val snackbarMessage: String? = null,
    val canDrawOverlays: Boolean = false,
    val floatingOverlayActive: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "chest_counter.db"
    ).build()

    private val settings = SettingsRepository(application)
    private val repo = ChestCounterRepository(application, db, settings)

    private val scanProgressFlow = MutableStateFlow<ScanProgress?>(null)
    private val overlayPreviewStateFlow = MutableStateFlow(OverlayPreviewState.IDLE)
    private val estimateStateFlow = MutableStateFlow<Pair<ChestEstimate.Result?, String?>>(null to null)
    private val errorLogFlow = MutableStateFlow("")
    private val catalogRevFlow = MutableStateFlow(1)
    private val snackbarFlow = MutableStateFlow<String?>(null)
    private val fortniteStatusFlow = MutableStateFlow(FortniteLauncher.status(application))
    private val overlayPermFlow = MutableStateFlow(
        OverlayPermissionHelper.canDrawOverlays(application) to FloatingOverlayService.isRunning
    )

    private val tickerFlow = flow {
        while (true) {
            emit(Instant.now())
            delay(1000L)
        }
    }

    init {
        TreasureCatalog.init(application)
        catalogRevFlow.value = TreasureCatalog.data.revision
        viewModelScope.launch {
            errorLogFlow.value = repo.readErrorLog()
            val cfg = settings.configFlow.first()
            if (cfg.autoRefreshCatalog) {
                val changed = repo.refreshCatalog(cfg.language)
                if (changed) {
                    catalogRevFlow.value = TreasureCatalog.data.revision
                }
            }
        }
    }

    private data class AuxiliaryState(
        val progress: ScanProgress?,
        val now: Instant,
        val fortnite: FortniteStatus,
        val preview: OverlayPreviewState,
        val estimate: Pair<ChestEstimate.Result?, String?>,
        val errorLog: String,
        val catalogRev: Int,
        val snackbar: String?,
        val canDrawOverlays: Boolean,
        val floatingActive: Boolean,
    )

    private val auxFlow = combine(
        scanProgressFlow,
        tickerFlow,
        fortniteStatusFlow,
        overlayPreviewStateFlow,
        estimateStateFlow,
    ) { prog, now, fn, preview, est ->
        AuxiliaryState(prog, now, fn, preview, est, "", catalogRevFlow.value, null, false, false)
    }.combine(
        combine(errorLogFlow, catalogRevFlow, snackbarFlow, overlayPermFlow) { err, rev, sb, perm ->
            listOf(err, rev, sb, perm)
        }
    ) { a, b ->
        @Suppress("UNCHECKED_CAST")
        val perm = b[3] as Pair<Boolean, Boolean>
        a.copy(
            errorLog = b[0] as String,
            catalogRev = b[1] as Int,
            snackbar = b[2] as String?,
            canDrawOverlays = perm.first,
            floatingActive = perm.second,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        settings.configFlow,
        repo.matchesFlow,
        repo.manualEntriesFlow,
        repo.obtainedPrizesFlow,
        repo.activityLogFlow,
        auxFlow,
    ) { args ->
        val config = args[0] as AppConfig
        @Suppress("UNCHECKED_CAST")
        val matches = args[1] as List<MatchRecordDto>
        @Suppress("UNCHECKED_CAST")
        val manualMap = args[2] as Map<String, List<com.example.domain.ManualEntryDto>>
        @Suppress("UNCHECKED_CAST")
        val obtainedMap = args[3] as Map<String, List<String>>
        @Suppress("UNCHECKED_CAST")
        val log = args[4] as List<ActivityLogEntry>
        val aux = args[5] as AuxiliaryState

        val lang = config.language
        val profiles = Profiles.list(matches, lang)
        val current = Profiles.current(matches, config.selectedProfile, lang)
        val view = Profiles.buildView(matches, manualMap, obtainedMap, config.selectedProfile, lang)
        val notices = TreasureCatalog.activeNotices(aux.now).filter { it.id !in config.dismissedNotices }
        val sys24 = DateFormat.is24HourFormat(getApplication())

        MainUiState(
            config = config,
            allMatches = matches,
            view = view,
            profiles = profiles,
            currentProfile = current,
            notices = notices,
            activityLog = log,
            scanProgress = aux.progress,
            nowUtc = aux.now,
            fortniteStatus = aux.fortnite,
            overlayPreviewState = aux.preview,
            estimateResult = aux.estimate.first,
            estimateMessage = aux.estimate.second,
            errorLogText = aux.errorLog,
            catalogRevision = aux.catalogRev,
            isSystem24Hour = sys24,
            snackbarMessage = aux.snackbar,
            canDrawOverlays = aux.canDrawOverlays,
            floatingOverlayActive = aux.floatingActive,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            isSystem24Hour = DateFormat.is24HourFormat(application),
            fortniteStatus = FortniteLauncher.status(application),
            canDrawOverlays = OverlayPermissionHelper.canDrawOverlays(application),
            floatingOverlayActive = FloatingOverlayService.isRunning,
        ),
    )

    fun refreshOverlayPermissionState() {
        val ctx = getApplication<Application>()
        overlayPermFlow.value = OverlayPermissionHelper.canDrawOverlays(ctx) to FloatingOverlayService.isRunning
    }

    fun startOrUpdateFloatingOverlay(overlayView: OverlayView) {
        val ctx = getApplication<Application>()
        val cfg = uiState.value.config
        if (!OverlayPermissionHelper.canDrawOverlays(ctx)) {
            refreshOverlayPermissionState()
            return
        }
        FloatingOverlayService.startOrUpdate(
            context = ctx,
            total = overlayView.total,
            last = overlayView.last ?: "—",
            session = overlayView.session,
            treasures = overlayView.treasures,
            squad = overlayView.squad,
            style = cfg.overlayStyle,
            sizePct = cfg.overlaySize,
            opacityPct = cfg.overlayOpacity,
            live = overlayView.currentLive,
        )
        overlayPermFlow.value = true to true
    }

    fun stopFloatingOverlay() {
        val ctx = getApplication<Application>()
        FloatingOverlayService.stop(ctx)
        overlayPermFlow.value = OverlayPermissionHelper.canDrawOverlays(ctx) to false
    }

    fun refreshFortniteStatus() {
        fortniteStatusFlow.value = FortniteLauncher.status(getApplication())
    }

    fun launchExperienceBr() {
        refreshFortniteStatus()
        val ok = FortniteLauncher.openExperienceBr(getApplication())
        val lang = uiState.value.config.language
        if (ok) {
            viewModelScope.launch {
                repo.log(
                    L.t(
                        lang,
                        "Lancement de Fortnite Battle Royale (experience_br)",
                        "Launching Fortnite Battle Royale (experience_br)",
                        "Abriendo Fortnite Battle Royale (experience_br)",
                    ),
                    ActivityLevel.INFO,
                )
            }
        } else {
            snackbarFlow.value = L.t(
                lang,
                "Impossible d'ouvrir le lien experience_br",
                "Unable to open experience_br link",
                "No se pudo abrir el enlace experience_br",
            )
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { settings.setLanguage(code) }
    }

    fun setProfile(key: String) {
        viewModelScope.launch { settings.setProfile(key) }
    }

    fun setTimeZone(tz: String) {
        viewModelScope.launch { settings.setTimeZone(tz) }
    }

    fun setClockFormat(fmt: String) {
        viewModelScope.launch { settings.setClockFormat(fmt) }
    }

    fun setAutoRefreshCatalog(on: Boolean) {
        viewModelScope.launch { settings.setAutoRefreshCatalog(on) }
    }

    fun dismissNotice(id: String) {
        viewModelScope.launch { settings.dismissNotice(id) }
    }

    fun dismissWelcome() {
        viewModelScope.launch { settings.dismissWelcome() }
    }

    fun setOverlayStyle(style: String) {
        viewModelScope.launch { settings.setOverlayStyle(style) }
    }

    fun setOverlaySize(size: Int) {
        viewModelScope.launch { settings.setOverlaySize(size) }
    }

    fun setOverlayOpacity(opacity: Int) {
        viewModelScope.launch { settings.setOverlayOpacity(opacity) }
    }

    fun setOverlayPreviewState(state: OverlayPreviewState) {
        overlayPreviewStateFlow.value = state
    }

    fun setShowSquadTotal(on: Boolean) {
        viewModelScope.launch { settings.setShowSquadTotal(on) }
    }

    fun setShowSquadInReport(on: Boolean) {
        viewModelScope.launch { settings.setShowSquadInReport(on) }
    }

    fun setShowTeamTreasuresInReport(on: Boolean) {
        viewModelScope.launch { settings.setShowTeamTreasuresInReport(on) }
    }

    fun setObtained(prizeId: String, on: Boolean) {
        val profileKey = uiState.value.currentProfile?.key ?: ""
        viewModelScope.launch { repo.setObtained(profileKey, prizeId, on) }
    }

    fun addManualChests(chests: Int, thisWeek: Boolean) {
        if (chests == 0) return
        val state = uiState.value
        val profileKey = state.currentProfile?.key ?: ""
        val weekFrom = if (thisWeek) TreasureCatalog.shownWeek(state.nowUtc)?.from else null
        viewModelScope.launch {
            repo.addManual(profileKey, chests, weekFromUtc = weekFrom)
            val sign = if (chests > 0) "+${L.n(state.config.language, chests)}" else L.n(state.config.language, chests)
            repo.log(
                L.t(
                    state.config.language,
                    "Ajustement manuel : $sign coffres",
                    "Manual adjustment: $sign chests",
                    "Ajuste manual: $sign cofres",
                ),
                ActivityLevel.INFO,
            )
        }
    }

    fun computeEstimate(careerMatches: Int) {
        val state = uiState.value
        val lang = state.config.language
        val counted = state.view.matches.size
        if (counted < ChestEstimate.MIN_COUNTED_MATCHES) {
            estimateStateFlow.value = null to L.t(
                lang,
                "Il faut au moins ${ChestEstimate.MIN_COUNTED_MATCHES} parties comptées pour une moyenne fiable.",
                "At least ${ChestEstimate.MIN_COUNTED_MATCHES} counted matches are needed for a reliable average.",
                "Hacen falta al menos ${ChestEstimate.MIN_COUNTED_MATCHES} partidas contadas para una media fiable.",
            )
            return
        }
        val res = ChestEstimate.fromCareer(state.view, careerMatches)
        if (res == null) {
            estimateStateFlow.value = null to L.t(
                lang,
                "Rien à ajouter : tes parties comptées couvrent déjà ce nombre.",
                "Nothing to add: your counted matches already cover that number.",
                "Nada que añadir: tus partidas contadas ya cubren ese número.",
            )
            return
        }
        val avgTxt = L.f(lang, res.average, 1)
        val msg = L.t(
            lang,
            "${L.n(lang, res.missingMatches)} parties manquantes × $avgTxt coffres/partie = ${L.n(lang, res.chests)} coffres",
            "${L.n(lang, res.missingMatches)} missing matches × $avgTxt chests/match = ${L.n(lang, res.chests)} chests",
            "${L.n(lang, res.missingMatches)} partidas faltantes × $avgTxt cofres/partida = ${L.n(lang, res.chests)} cofres",
        )
        estimateStateFlow.value = res to msg
    }

    fun applyEstimate() {
        val res = estimateStateFlow.value.first ?: return
        val state = uiState.value
        val profileKey = state.currentProfile?.key ?: ""
        viewModelScope.launch {
            repo.addManual(profileKey, res.chests, careerMatches = res.careerMatches)
            estimateStateFlow.value = null to null
            repo.log(
                L.t(
                    state.config.language,
                    "Estimation Carrière : +${L.n(state.config.language, res.chests)} coffres (${L.n(state.config.language, res.careerMatches)} parties)",
                    "Career estimate: +${L.n(state.config.language, res.chests)} chests (${L.n(state.config.language, res.careerMatches)} matches)",
                    "Estimación Carrera: +${L.n(state.config.language, res.chests)} cofres (${L.n(state.config.language, res.careerMatches)} partidas)",
                ),
                ActivityLevel.INFO,
            )
        }
    }

    fun clearEstimate() {
        estimateStateFlow.value = null to null
    }

    fun removeManualEntry(id: String) {
        val profileKey = uiState.value.currentProfile?.key ?: ""
        viewModelScope.launch { repo.removeManual(profileKey, id) }
    }

    fun updateMatchChests(sessionId: String, chestsOpened: Int) {
        viewModelScope.launch { repo.updateMatchChests(sessionId, chestsOpened) }
    }

    fun deleteMatch(sessionId: String) {
        viewModelScope.launch { repo.deleteMatch(sessionId) }
    }

    fun refreshCatalogNow() {
        val lang = uiState.value.config.language
        viewModelScope.launch {
            val changed = repo.refreshCatalog(lang)
            catalogRevFlow.value = TreasureCatalog.data.revision
            snackbarFlow.value = if (changed) {
                L.t(
                    lang,
                    "Catalogue mis à jour (rév. ${TreasureCatalog.data.revision})",
                    "Catalog updated (rev. ${TreasureCatalog.data.revision})",
                    "Catálogo actualizado (rev. ${TreasureCatalog.data.revision})",
                )
            } else {
                L.t(
                    lang,
                    "Catalogue déjà à jour (rév. ${TreasureCatalog.data.revision})",
                    "Catalog already up to date (rev. ${TreasureCatalog.data.revision})",
                    "Catálogo al día (rev. ${TreasureCatalog.data.revision})",
                )
            }
        }
    }

    fun onReplaysFolderSelected(uri: Uri?) {
        if (uri == null) return
        repo.persistTreePermission(uri)
        val uriStr = uri.toString()
        val lang = uiState.value.config.language
        viewModelScope.launch {
            settings.setDemosTreeUri(uriStr)
            repo.scanSavedDemosFolder(uriStr, lang) { scanProgressFlow.value = it }
            errorLogFlow.value = repo.readErrorLog()
        }
    }

    fun rescanSavedDemosFolder() {
        val uriStr = uiState.value.config.demosTreeUri ?: return
        val lang = uiState.value.config.language
        viewModelScope.launch {
            repo.scanSavedDemosFolder(uriStr, lang) { scanProgressFlow.value = it }
            errorLogFlow.value = repo.readErrorLog()
        }
    }

    fun importReplayFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val lang = uiState.value.config.language
        viewModelScope.launch {
            repo.scanReplayUris(uris, lang) { scanProgressFlow.value = it }
            errorLogFlow.value = repo.readErrorLog()
        }
    }

    fun importDataJson(uri: Uri?) {
        if (uri == null) return
        val lang = uiState.value.config.language
        viewModelScope.launch {
            try {
                val count = repo.importDataJson(uri, lang)
                snackbarFlow.value = L.t(
                    lang,
                    "${L.n(lang, count)} parties importées depuis data.json",
                    "Imported ${L.n(lang, count)} matches from data.json",
                    "Importadas ${L.n(lang, count)} partidas desde data.json",
                )
            } catch (e: Exception) {
                repo.appendErrorLog("data.json", e.message ?: "Import error")
                errorLogFlow.value = repo.readErrorLog()
                snackbarFlow.value = L.t(
                    lang,
                    "Erreur d'importation : ${e.message}",
                    "Import failed: ${e.message}",
                    "Error al importar: ${e.message}",
                )
            }
        }
    }

    fun writeCsvToUri(uri: Uri?) {
        if (uri == null) return
        val state = uiState.value
        viewModelScope.launch {
            val csv = ReportExporter.matchesCsv(
                matches = state.allMatches,
                lang = state.config.language,
                timeZone = state.config.timeZone,
                clockFormat = state.config.clockFormat,
                isSystem24 = state.isSystem24Hour,
            )
            writeTextToUri(uri, csv)
            snackbarFlow.value = L.t(state.config.language, "matches.csv enregistré", "matches.csv saved", "matches.csv guardado")
        }
    }

    fun writeDataJsonToUri(uri: Uri?) {
        if (uri == null) return
        val lang = uiState.value.config.language
        viewModelScope.launch {
            val dataJson = repo.exportDataJson()
            writeTextToUri(uri, dataJson)
            snackbarFlow.value = L.t(lang, "data.json enregistré", "data.json saved", "data.json guardado")
        }
    }

    fun writeOverlayHtmlToUri(uri: Uri?, overlayView: com.example.domain.OverlayView) {
        if (uri == null) return
        val state = uiState.value
        viewModelScope.launch {
            val html = ReportExporter.overlayHtml(overlayView, state.config.overlayStyle, state.config.language)
            writeTextToUri(uri, html)
            snackbarFlow.value = L.t(state.config.language, "overlay.html enregistré", "overlay.html saved", "overlay.html guardado")
        }
    }

    fun copyOverlayHtml(overlayView: com.example.domain.OverlayView) {
        val state = uiState.value
        val html = ReportExporter.overlayHtml(overlayView, state.config.overlayStyle, state.config.language)
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("overlay.html", html))
        snackbarFlow.value = L.t(state.config.language, "Copié !", "Copied!", "¡Copiado!")
    }

    private fun writeTextToUri(uri: Uri, content: String) {
        try {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        } catch (_: Exception) {
        }
    }

    fun clearAllData() {
        val lang = uiState.value.config.language
        viewModelScope.launch {
            repo.clearAllData(lang)
            settings.resetAll()
            errorLogFlow.value = ""
            snackbarFlow.value = L.t(lang, "Données supprimées", "All data cleared", "Datos eliminados")
        }
    }

    fun consumeSnackbar() {
        snackbarFlow.value = null
    }
}
