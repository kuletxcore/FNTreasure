package com.kuletx.forttreasure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuletx.forttreasure.data.FortniteLauncher
import com.kuletx.forttreasure.domain.OverlayView
import com.kuletx.forttreasure.overlay.OverlayPermissionHelper
import com.kuletx.forttreasure.ui.ChestCounterAppShell
import com.kuletx.forttreasure.ui.MainViewModel
import com.kuletx.forttreasure.ui.theme.ChestCounterTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChestCounterTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.uiState.collectAsStateWithLifecycle()
                val windowSizeClass = calculateWindowSizeClass(this)
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            vm.refreshOverlayPermissionState()
                            vm.refreshFortniteStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                var pendingOverlayView by remember { mutableStateOf<OverlayView?>(null) }

                val pickReplaysLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    vm.importReplayFiles(uris)
                }

                val pickDemosFolderLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    vm.onReplaysFolderSelected(uri)
                }

                val importDataJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    vm.importDataJson(uri)
                }

                val exportCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    vm.writeCsvToUri(uri)
                }

                val exportDataJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    vm.writeDataJsonToUri(uri)
                }

                val exportOverlayHtmlLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/html")
                ) { uri ->
                    pendingOverlayView?.let { vm.writeOverlayHtmlToUri(uri, it) }
                    pendingOverlayView = null
                }

                ChestCounterAppShell(
                    state = state,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    onSelectProfile = vm::setProfile,
                    onLanguageChange = vm::setLanguage,
                    onTimeZoneChange = vm::setTimeZone,
                    onClockFormatChange = vm::setClockFormat,
                    onAutoRefreshChange = vm::setAutoRefreshCatalog,
                    onDismissWelcome = vm::dismissWelcome,
                    onDismissNotice = vm::dismissNotice,
                    onAddManual = vm::addManualChests,
                    onRemoveManualEntry = vm::removeManualEntry,
                    onComputeEstimate = vm::computeEstimate,
                    onApplyEstimate = vm::applyEstimate,
                    onClearEstimate = vm::clearEstimate,
                    onIncrementLiveChest = vm::incrementLiveChest,
                    onAddLiveTreasure = vm::addLiveTreasure,
                    onRemoveLiveTreasure = vm::removeLiveTreasure,
                    onFinishLiveMatch = vm::finishLiveMatch,
                    onResetLiveMatch = vm::resetLiveMatch,
                    onStartNewSession = vm::startNewSession,
                    onToggleShowSquadTotal = vm::setShowSquadTotal,
                    onToggleShowSquadInReport = vm::setShowSquadInReport,
                    onToggleShowTeamTreasures = vm::setShowTeamTreasuresInReport,
                    onToggleObtained = vm::setObtained,
                    onUpdateMatchDetails = vm::updateMatchDetails,
                    onDeleteMatch = vm::deleteMatch,
                    onOverlayStyleChange = vm::setOverlayStyle,
                    onOverlaySizeChange = vm::setOverlaySize,
                    onOverlayOpacityChange = vm::setOverlayOpacity,
                    onOverlayPreviewStateChange = vm::setOverlayPreviewState,
                    onRequestOverlayPermission = {
                        startActivity(OverlayPermissionHelper.createManageOverlayIntent(this))
                    },
                    onStartFloatingOverlay = vm::startOrUpdateFloatingOverlay,
                    onStopFloatingOverlay = vm::stopFloatingOverlay,
                    onCopyObsHtml = vm::copyOverlayHtml,
                    onSaveObsHtml = { ov ->
                        pendingOverlayView = ov
                        exportOverlayHtmlLauncher.launch("overlay.html")
                    },
                    onPickReplays = {
                        pickReplaysLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    onPickDemosFolder = {
                        pickDemosFolderLauncher.launch(null)
                    },
                    onRescanDemosFolder = vm::rescanSavedDemosFolder,
                    onImportDataJson = {
                        importDataJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onRefreshCatalog = vm::refreshCatalogNow,
                    onExportCsv = {
                        exportCsvLauncher.launch("matches.csv")
                    },
                    onExportDataJson = {
                        exportDataJsonLauncher.launch("data.json")
                    },
                    onClearAllData = vm::clearAllData,
                    onLaunchExperienceBr = vm::launchExperienceBr,
                    onOpenUrl = { url -> FortniteLauncher.openUrl(this, url) },
                    onConsumeSnackbar = vm::consumeSnackbar,
                )
            }
        }
    }
}
