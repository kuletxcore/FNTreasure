package com.kuletx.forttreasure.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.dp
import com.kuletx.forttreasure.domain.AppInfo
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.OverlayView
import com.kuletx.forttreasure.domain.TreasureFindDto
import com.kuletx.forttreasure.ui.components.ChestVectorIcon
import com.kuletx.forttreasure.ui.components.QuickSwitchFabMenu
import com.kuletx.forttreasure.ui.components.QuickViewMode
import com.kuletx.forttreasure.ui.screens.ChestOddsCalculatorScreen
import com.kuletx.forttreasure.ui.screens.EditChestsDialog
import com.kuletx.forttreasure.ui.screens.HelpScreen
import com.kuletx.forttreasure.ui.screens.HomeScreen
import com.kuletx.forttreasure.ui.screens.LootTrackerScreen
import com.kuletx.forttreasure.ui.screens.OverlayScreen
import com.kuletx.forttreasure.ui.screens.ReportScreen
import com.kuletx.forttreasure.ui.screens.SettingsScreen
import com.kuletx.forttreasure.ui.theme.AccentGold
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.LiveGreen
import com.kuletx.forttreasure.ui.theme.NightMuted
import com.kuletx.forttreasure.ui.theme.NightSeaBg
import com.kuletx.forttreasure.ui.theme.NightSeaSurface
import com.kuletx.forttreasure.ui.theme.ParchmentCard
import com.kuletx.forttreasure.ui.theme.ParchmentInk
import com.kuletx.forttreasure.ui.theme.TitleGold

@Composable
fun ChestCounterAppShell(
    state: MainUiState,
    widthSizeClass: WindowWidthSizeClass,
    onSelectProfile: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
    onClockFormatChange: (String) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onDismissWelcome: () -> Unit,
    onDismissNotice: (String) -> Unit,
    onAddManual: (Int, Boolean) -> Unit,
    onRemoveManualEntry: (String) -> Unit,
    onComputeEstimate: (Int) -> Unit,
    onApplyEstimate: () -> Unit,
    onClearEstimate: () -> Unit,
    onIncrementLiveChest: (Int) -> Unit,
    onAddLiveTreasure: (itemName: String, prizeId: String?, fromGoldenChest: Boolean) -> Unit,
    onRemoveLiveTreasure: (Int) -> Unit,
    onFinishLiveMatch: () -> Unit,
    onResetLiveMatch: () -> Unit,
    onStartNewSession: () -> Unit,
    onToggleShowSquadTotal: (Boolean) -> Unit,
    onToggleShowSquadInReport: (Boolean) -> Unit,
    onToggleShowTeamTreasures: (Boolean) -> Unit,
    onToggleObtained: (String, Boolean) -> Unit,
    onUpdateMatchDetails: (sessionId: String, chestsOpened: Int, treasures: List<TreasureFindDto>, matchNumber: Int?, sessionNumber: Int?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onOverlayStyleChange: (String) -> Unit,
    onOverlaySizeChange: (Int) -> Unit,
    onOverlayOpacityChange: (Int) -> Unit,
    onOverlayPreviewStateChange: (com.kuletx.forttreasure.domain.OverlayPreviewState) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onStartFloatingOverlay: (OverlayView) -> Unit,
    onStopFloatingOverlay: () -> Unit,
    onCopyObsHtml: (OverlayView) -> Unit,
    onSaveObsHtml: (OverlayView) -> Unit,
    onPickReplays: () -> Unit,
    onPickDemosFolder: () -> Unit,
    onRescanDemosFolder: () -> Unit,
    onImportDataJson: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onExportCsv: () -> Unit,
    onExportDataJson: () -> Unit,
    onClearAllData: () -> Unit,
    onLaunchExperienceBr: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onConsumeSnackbar: () -> Unit,
) {
    var activeTab by remember { mutableStateOf(AppTab.HOME) }
    var quickView by remember { mutableStateOf<QuickViewMode?>(null) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lang = state.config.language

    // Back press on secondary tabs or quick views returns cleanly to Home (Logbook)
    BackHandler(enabled = !fabMenuExpanded && (quickView != null || activeTab != AppTab.HOME)) {
        quickView = null
        activeTab = AppTab.HOME
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onConsumeSnackbar()
        }
    }

    val navItems = remember(lang) {
        listOf(
            Triple(AppTab.HOME, L.t(lang, "Accueil", "Home", "Inicio"), Icons.Default.Home),
            Triple(AppTab.REPORT, L.t(lang, "Rapport", "Report", "Informe"), Icons.Default.Assessment),
            Triple(AppTab.OVERLAY, L.t(lang, "Overlay", "Overlay", "Overlay"), Icons.Default.Layers),
            Triple(AppTab.SETTINGS, L.t(lang, "Réglages", "Settings", "Ajustes"), Icons.Default.Settings),
            Triple(AppTab.HELP, L.t(lang, "Aide", "Help", "Ayuda"), Icons.AutoMirrored.Filled.HelpOutline),
        )
    }

    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val activeQuickMode = when {
        quickView != null -> quickView
        activeTab == AppTab.HOME -> QuickViewMode.LOGBOOK
        else -> null
    }

    Scaffold(
        containerColor = NightSeaBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            QuickSwitchFabMenu(
                expanded = fabMenuExpanded,
                onExpandedChange = { fabMenuExpanded = it },
                activeMode = activeQuickMode,
                lang = lang,
                onSelectMode = { mode ->
                    when (mode) {
                        QuickViewMode.LOGBOOK -> {
                            quickView = null
                            activeTab = AppTab.HOME
                        }
                        QuickViewMode.LOOT_TRACKER -> {
                            quickView = QuickViewMode.LOOT_TRACKER
                        }
                        QuickViewMode.CHEST_ODDS_CALCULATOR -> {
                            quickView = QuickViewMode.CHEST_ODDS_CALCULATOR
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!useRail) {
                NavigationBar(
                    containerColor = NightSeaSurface,
                    contentColor = ParchmentInk,
                ) {
                    navItems.forEach { (tab, label, icon) ->
                        val selected = quickView == null && activeTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                fabMenuExpanded = false
                                quickView = null
                                activeTab = tab
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CardInk,
                                selectedTextColor = TitleGold,
                                indicatorColor = AccentGold,
                                unselectedIconColor = NightMuted,
                                unselectedTextColor = NightMuted,
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = NightSeaSurface,
                    contentColor = ParchmentInk,
                ) {
                    navItems.forEach { (tab, label, icon) ->
                        NavigationRailItem(
                            selected = quickView == null && activeTab == tab,
                            onClick = {
                                fabMenuExpanded = false
                                quickView = null
                                activeTab = tab
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CardInk,
                                selectedTextColor = TitleGold,
                                indicatorColor = AccentGold,
                                unselectedIconColor = NightMuted,
                                unselectedTextColor = NightMuted,
                            ),
                            modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LogbookHeader(
                        state = state,
                        onSelectProfile = onSelectProfile,
                    )
                    HorizontalDivider(color = AccentGold, thickness = 2.dp)

                    if (quickView != null) {
                        when (quickView) {
                            QuickViewMode.LOOT_TRACKER -> LootTrackerScreen(
                                state = state,
                                onEditChests = { showEditDialog = true },
                                onOpenFullReport = {
                                    quickView = null
                                    activeTab = AppTab.REPORT
                                },
                                onToggleShowTeamTreasures = onToggleShowTeamTreasures,
                                onToggleObtained = onToggleObtained,
                                onIncrementLiveChest = onIncrementLiveChest,
                                onAddLiveTreasure = onAddLiveTreasure,
                                onRemoveLiveTreasure = onRemoveLiveTreasure,
                                onFinishLiveMatch = onFinishLiveMatch,
                                onResetLiveMatch = onResetLiveMatch,
                                onStartNewSession = onStartNewSession,
                            )
                            QuickViewMode.CHEST_ODDS_CALCULATOR -> ChestOddsCalculatorScreen(
                                state = state,
                                onOpenFullReport = {
                                    quickView = null
                                    activeTab = AppTab.REPORT
                                },
                            )
                            QuickViewMode.LOGBOOK, null -> Unit
                        }
                    } else {
                        when (activeTab) {
                            AppTab.HOME -> HomeScreen(
                                state = state,
                                onOpenReport = { activeTab = AppTab.REPORT },
                                onEditChests = { showEditDialog = true },
                                onIncrementLiveChest = onIncrementLiveChest,
                                onAddLiveTreasure = onAddLiveTreasure,
                                onRemoveLiveTreasure = onRemoveLiveTreasure,
                                onFinishLiveMatch = onFinishLiveMatch,
                                onResetLiveMatch = onResetLiveMatch,
                                onStartNewSession = onStartNewSession,
                                onUpdateMatchDetails = onUpdateMatchDetails,
                                onDeleteMatch = onDeleteMatch,
                                onPickReplays = onPickReplays,
                                onPickDemosFolder = onPickDemosFolder,
                                onImportDataJson = onImportDataJson,
                                onRefreshCatalog = onRefreshCatalog,
                                onLaunchExperienceBr = onLaunchExperienceBr,
                                onLanguageChange = onLanguageChange,
                                onDismissWelcome = onDismissWelcome,
                                onDismissNotice = onDismissNotice,
                                onOpenUrl = onOpenUrl,
                            )
                            AppTab.REPORT -> ReportScreen(
                                state = state,
                                onEditChests = { showEditDialog = true },
                                onToggleShowSquadInReport = onToggleShowSquadInReport,
                                onToggleShowTeamTreasures = onToggleShowTeamTreasures,
                                onToggleObtained = onToggleObtained,
                                onUpdateMatchDetails = onUpdateMatchDetails,
                                onDeleteMatch = onDeleteMatch,
                                onExportCsv = onExportCsv,
                                onExportDataJson = onExportDataJson,
                                onOpenUrl = onOpenUrl,
                            )
                            AppTab.OVERLAY -> OverlayScreen(
                                state = state,
                                onStyleChange = onOverlayStyleChange,
                                onSizeChange = onOverlaySizeChange,
                                onOpacityChange = onOverlayOpacityChange,
                                onShowSquadChange = onToggleShowSquadTotal,
                                onPreviewStateChange = onOverlayPreviewStateChange,
                                onRequestOverlayPermission = onRequestOverlayPermission,
                                onStartFloatingOverlay = onStartFloatingOverlay,
                                onStopFloatingOverlay = onStopFloatingOverlay,
                                onCopyObsHtml = onCopyObsHtml,
                                onSaveObsHtml = onSaveObsHtml,
                            )
                            AppTab.SETTINGS -> SettingsScreen(
                                state = state,
                                onLanguageChange = onLanguageChange,
                                onTimeZoneChange = onTimeZoneChange,
                                onClockFormatChange = onClockFormatChange,
                                onProfileChange = onSelectProfile,
                                onAutoRefreshChange = onAutoRefreshChange,
                                onPickDemosFolder = onPickDemosFolder,
                                onRescanDemosFolder = onRescanDemosFolder,
                                onPickReplays = onPickReplays,
                                onImportDataJson = onImportDataJson,
                            )
                            AppTab.HELP -> HelpScreen(
                                state = state,
                                onLaunchExperienceBr = onLaunchExperienceBr,
                                onExportCsv = onExportCsv,
                                onExportDataJson = onExportDataJson,
                                onClearAllData = onClearAllData,
                                onOpenUrl = onOpenUrl,
                            )
                        }
                    }

                    Spacer(Modifier.height(84.dp))
                }

                if (fabMenuExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { fabMenuExpanded = false },
                            )
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditChestsDialog(
            state = state,
            onDismiss = {
                showEditDialog = false
                onClearEstimate()
            },
            onAddManual = onAddManual,
            onRemoveEntry = onRemoveManualEntry,
            onComputeEstimate = onComputeEstimate,
            onApplyEstimate = onApplyEstimate,
        )
    }
}

@Composable
private fun LogbookHeader(
    state: MainUiState,
    onSelectProfile: (String) -> Unit,
) {
    val lang = state.config.language
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChestVectorIcon(size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = L.t(lang, "Compteur de coffres Fortnite", "Fortnite Chest Counter", "Contador de cofres de Fortnite"),
                style = MaterialTheme.typography.headlineLarge,
                color = TitleGold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.profiles.size > 1) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { menuOpen = true },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AccentGold),
                        ) {
                            Text(
                                text = state.currentProfile?.let { "${it.name} (${it.matches})" } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = ParchmentInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            state.profiles.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.matches})") },
                                    onClick = {
                                        onSelectProfile(p.key)
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }
                } else if (state.currentProfile != null) {
                    Text(state.currentProfile.name, style = MaterialTheme.typography.bodySmall, color = ParchmentInk)
                }
                Text("v${AppInfo.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = NightMuted)
            }
        }

        // Status Pill
        val busy = state.scanProgress != null
        val pillText = when {
            busy -> L.t(lang, "Analyse…", "Scanning…", "Analizando…")
            state.fortniteStatus.isInstalled -> "experience_br"
            else -> L.t(lang, "Prêt", "Ready", "Listo")
        }
        Surface(
            color = ParchmentCard,
            shape = RoundedCornerShape(999.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (busy) AccentGold else LiveGreen)
                )
                Text(
                    text = pillText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = CardInk,
                )
            }
        }
    }
}
