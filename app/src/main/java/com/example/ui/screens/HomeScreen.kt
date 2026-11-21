package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ActivityLevel
import com.example.domain.AppInfo
import com.example.domain.L
import com.example.domain.OverlayModel
import com.example.domain.Pretty
import com.example.ui.MainUiState
import com.example.ui.components.HeroChestCard
import com.example.ui.components.LogbookPrimaryButton
import com.example.ui.components.LogbookSecondaryButton
import com.example.ui.components.NoticeCard
import com.example.ui.components.ParchmentCard
import com.example.ui.components.SectionHeading
import com.example.ui.components.StatTile
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.PurpleStar
import com.example.ui.theme.TealAccent
import com.example.ui.theme.WarnCrimson
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onOpenReport: () -> Unit,
    onEditChests: () -> Unit,
    onPickReplays: () -> Unit,
    onPickDemosFolder: () -> Unit,
    onImportDataJson: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onLaunchExperienceBr: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismissWelcome: () -> Unit,
    onDismissNotice: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val lang = state.config.language
    val view = state.view
    val matches = view.matches
    val sessionMatches = matches.filter(OverlayModel::isThisSession)
    val sessionChests = sessionMatches.sumOf { it.chestsOpened }
    val lastMatch = matches.maxByOrNull { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
    val treasures = matches.sumOf { it.treasureFinds.size }
    val avg = if (matches.isNotEmpty()) view.countedChests.toDouble() / matches.size else 0.0
    var showLootCalcSheet by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Notices from content.json
        state.notices.forEach { notice ->
            NoticeCard(
                notice = notice,
                lang = lang,
                onDismiss = { onDismissNotice(notice.id) },
                onOpenLink = onOpenUrl,
            )
        }

        // First-run Welcome Card
        if (!state.config.welcomeDismissed) {
            ParchmentCard {
                Text(
                    text = L.t(lang, "Bienvenue !", "Welcome!", "¡Bienvenido!"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = HeroChestBg,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = L.t(
                        lang,
                        "Active d'abord les rediffusions dans Fortnite : Paramètres → Jeu → Rediffusions → Enregistrer les rediffusions. Sur Android, importe tes fichiers .replay ou data.json, ou lance Battle Royale (experience_br) directement depuis ici. Des coffres ouverts avant ? Le crayon de « Coffres au total » permet de les ajouter.",
                        "First turn on replays in Fortnite: Settings → Game → Replays → Record Replays. On Android, import your .replay files or desktop data.json, or launch Battle Royale (experience_br) directly from here. Chests opened before? The pencil on \"Chests in total\" adds them.",
                        "Primero activa las repeticiones en Fortnite: Ajustes → Juego → Repeticiones → Grabar repeticiones. En Android, importa tus archivos .replay o data.json, o abre Battle Royale (experience_br) desde aquí. ¿Cofres abiertos antes? El lápiz de «Cofres en total» permite añadirlos.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardInk,
                )
                Spacer(Modifier.height(10.dp))
                var langExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded },
                ) {
                    OutlinedTextField(
                        value = L.nativeName(lang),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(L.t(lang, "Langue", "Language", "Idioma")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = CardLine,
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("welcome_language_dropdown"),
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                    ) {
                        L.codes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(L.nativeName(code)) },
                                onClick = {
                                    onLanguageChange(code)
                                    langExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LogbookPrimaryButton(
                    text = L.t(lang, "C'est parti !", "Let's go!", "¡Vamos!"),
                    onClick = onDismissWelcome,
                    modifier = Modifier.testTag("dismiss_welcome_button"),
                )
            }
        }

        // Scan Progress Banner
        state.scanProgress?.let { prog ->
            ParchmentCard {
                Text(
                    text = "${L.t(lang, "Analyse de tes replays…", "Analyzing your replays…", "Analizando tus repeticiones…")} ${prog.done} / ${prog.total}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (prog.total > 0) prog.done.toFloat() / prog.total else 0f },
                    color = AccentGold,
                    trackColor = CardLine,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        }

        // Fortnite Android (com.epicgames.fortnite) + experience_br Launch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentCard, contentColor = CardInk),
            border = BorderStroke(1.dp, AccentGold),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fortnite Android · experience_br",
                            style = MaterialTheme.typography.titleMedium,
                            color = CardInk,
                        )
                        Text(
                            text = AppInfo.FORTNITE_PACKAGE,
                            style = MaterialTheme.typography.bodySmall,
                            color = CardMuted,
                        )
                    }
                    Surface(
                        color = if (state.fortniteStatus.isInstalled) LiveGreen.copy(alpha = 0.16f) else CardLine.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.fortniteStatus.isInstalled) LiveGreen else CardMuted)
                            )
                            Text(
                                text = if (state.fortniteStatus.isInstalled) {
                                    L.t(lang, "Installé", "Installed", "Instalado") +
                                        (state.fortniteStatus.versionName?.let { " v$it" } ?: "")
                                } else {
                                    L.t(lang, "Non installé", "Not installed", "No instalado")
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CardInk,
                            )
                        }
                    }
                }
                LogbookPrimaryButton(
                    text = L.t(
                        lang,
                        "Jouer à Battle Royale (experience_br)",
                        "Launch Battle Royale (experience_br)",
                        "Abrir Battle Royale (experience_br)",
                    ),
                    onClick = onLaunchExperienceBr,
                    modifier = Modifier.fillMaxWidth().testTag("launch_experience_br_button"),
                )
            }
        }

        // Hero Chests Total Tile
        val manualLine = if (view.manualAdjust != 0) {
            val signed = if (view.manualAdjust > 0) "+${L.n(lang, view.manualAdjust)}" else L.n(lang, view.manualAdjust)
            L.t(
                lang,
                "dont $signed ajoutés à la main",
                "$signed of them added by hand",
                "de ellos, $signed añadidos a mano",
            )
        } else null

        HeroChestCard(
            totalChests = L.n(lang, view.totalChests),
            title = L.t(lang, "Coffres au total", "Chests in total", "Cofres en total"),
            subtitle = L.t(lang, "ouverts ou détruits par toi", "opened or destroyed by you", "abiertos o destruidos por ti"),
            manualLine = manualLine,
            onEditClick = onEditChests,
            editContentDescription = L.t(lang, "Modifier mon nombre de coffres", "Edit my number of chests", "Modificar mi número de cofres"),
        )

        // Secondary Stat Tiles Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = L.n(lang, sessionChests),
                label = "${L.t(lang, "Session", "Session", "Sesión")} · ${L.count(lang, sessionMatches.size, "partie", "parties", "match", "matches", "partida", "partidas")}",
                modifier = Modifier.weight(1f),
            )
            val lastSub = lastMatch?.let { m ->
                val whenTxt = (L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc))?.let {
                    L.dateTimeShort(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                }
                listOfNotNull(whenTxt, Pretty.playlist(m.playlist, lang)).joinToString(" · ")
            }
            StatTile(
                value = lastMatch?.let { L.n(lang, it.chestsOpened) } ?: "—",
                label = L.t(lang, "Dernière partie", "Last match", "Última partida"),
                sub = lastSub,
                modifier = Modifier.weight(1f),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "★ ${L.n(lang, treasures)}",
                label = L.t(lang, "Trésors", "Treasures", "Tesoros"),
                sub = "${L.count(lang, matches.size, "partie", "parties", "match", "matches", "partida", "partidas")} · ${L.t(lang, "${L.f(lang, avg, 1)} par partie", "${L.f(lang, avg, 1)} per match", "${L.f(lang, avg, 1)} por partida")}",
                valueColor = PurpleStar,
                modifier = Modifier.weight(1f),
            )
            if (state.config.showSquadTotal && view.teamChests > 0) {
                StatTile(
                    value = L.n(lang, view.squadChests),
                    label = L.t(lang, "Total d'équipe", "Squad total", "Total del equipo"),
                    sub = L.t(
                        lang,
                        "toi + ${L.n(lang, view.teamChests)} par tes coéquipiers",
                        "you + ${L.n(lang, view.teamChests)} by your teammates",
                        "tú + ${L.n(lang, view.teamChests)} de tus compañeros",
                    ),
                    badgeText = L.t(lang, "imprécis", "inaccurate", "impreciso"),
                    infoText = L.t(
                        lang,
                        "Compté dans TES replays : un coffre ouvert par un coéquipier à plus de 150 m de toi n'y est pas, et rien n'est enregistré après ton départ de la partie.",
                        "Counted in YOUR replays: a chest a teammate opens more than 150 m away from you is not in them, and nothing is recorded once you leave the match.",
                        "Contado en TUS repeticiones: un cofre que un compañero abre a más de 150 m de ti no está en ellas, y no se graba nada después de que salgas de la partida.",
                    ),
                    lang = lang,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Action Buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogbookPrimaryButton(
                text = L.t(lang, "Ouvrir le rapport", "Open the report", "Abrir el informe"),
                onClick = onOpenReport,
                modifier = Modifier.testTag("open_report_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Importer .replay", "Import .replay", "Importar .replay"),
                onClick = onPickReplays,
                modifier = Modifier.testTag("import_replays_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Dossier replays", "Replays folder", "Carpeta repeticiones"),
                onClick = onPickDemosFolder,
                modifier = Modifier.testTag("pick_demos_folder_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Importer data.json", "Import data.json", "Importar data.json"),
                onClick = onImportDataJson,
                modifier = Modifier.testTag("import_data_json_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Calculer chances", "Loot odds", "Probabilidades"),
                onClick = { showLootCalcSheet = true },
                modifier = Modifier.testTag("open_loot_calculator_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Actualiser cotes", "Refresh catalog", "Actualizar catálogo"),
                onClick = onRefreshCatalog,
                modifier = Modifier.testTag("refresh_catalog_button"),
            )
        }

        // Activity Log Card
        SectionHeading(L.t(lang, "Activité", "Activity", "Actividad"))
        ParchmentCard {
            if (state.activityLog.isEmpty()) {
                Text(
                    text = L.t(
                        lang,
                        "Rien pour l'instant. Importe des fichiers .replay ou un historique data.json.",
                        "Nothing yet. Import .replay files or a ChestCounter data.json backup.",
                        "Nada por ahora. Importa archivos .replay o una copia data.json.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.activityLog.forEach { entry ->
                        val color = when (entry.level) {
                            ActivityLevel.ERR -> Color(0xFFB3261E)
                            ActivityLevel.WARN -> Color(0xFF9A5B00)
                            ActivityLevel.OK -> Color(0xFF1D7A3C)
                            ActivityLevel.INFO -> Color(0xFF1F6F8B)
                            ActivityLevel.TREASURE -> PurpleStar
                            ActivityLevel.MUTED -> CardMuted
                            ActivityLevel.NORMAL -> CardInk
                        }
                        val stamp = L.clock(
                            Instant.ofEpochMilli(entry.timestampEpochMs),
                            lang,
                            state.config.timeZone,
                            state.config.clockFormat,
                            state.isSystem24Hour,
                            seconds = true,
                        )
                        Text(
                            text = "[$stamp] ${entry.text}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = color,
                        )
                    }
                }
            }
        }
    }

    if (showLootCalcSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLootCalcSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = com.example.ui.theme.NightSeaBg,
            contentColor = com.example.ui.theme.ParchmentInk,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeading(
                    L.t(
                        lang,
                        "Calculateur de chances par objet",
                        "Island Loot Odds Calculator",
                        "Calculadora de probabilidades de botín",
                    )
                )
                LootOddsCalculatorContent(state = state)
            }
        }
    }
}
