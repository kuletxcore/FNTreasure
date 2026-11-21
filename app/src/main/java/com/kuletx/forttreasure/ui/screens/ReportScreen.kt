package com.kuletx.forttreasure.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuletx.forttreasure.domain.ChestKinds
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.MatchRecordDto
import com.kuletx.forttreasure.domain.OverlayModel
import com.kuletx.forttreasure.domain.Pretty
import com.kuletx.forttreasure.domain.TreasureCatalog
import com.kuletx.forttreasure.domain.TreasureFindDto
import com.kuletx.forttreasure.domain.TreasurePrize
import com.kuletx.forttreasure.ui.MainUiState
import com.kuletx.forttreasure.ui.components.CollapsibleSection
import com.kuletx.forttreasure.ui.components.HeroChestCard
import com.kuletx.forttreasure.ui.components.LogbookSecondaryButton
import com.kuletx.forttreasure.ui.components.MatchDetailDialog
import com.kuletx.forttreasure.ui.components.ParchmentCard
import com.kuletx.forttreasure.ui.components.SectionHeading
import com.kuletx.forttreasure.ui.components.StatTile
import com.kuletx.forttreasure.ui.components.TreasureThumbnail
import com.kuletx.forttreasure.ui.theme.AccentGold
import com.kuletx.forttreasure.ui.theme.AccentGoldText
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.CardLine
import com.kuletx.forttreasure.ui.theme.CardMuted
import com.kuletx.forttreasure.ui.theme.CardTrack
import com.kuletx.forttreasure.ui.theme.HeroChestBg
import com.kuletx.forttreasure.ui.theme.ParchmentCard
import com.kuletx.forttreasure.ui.theme.ParchmentCardLight
import com.kuletx.forttreasure.ui.theme.ParchmentInkBright
import com.kuletx.forttreasure.ui.theme.PurpleStar
import com.kuletx.forttreasure.ui.theme.TealAccent
import com.kuletx.forttreasure.ui.theme.WarnCrimson
import com.kuletx.forttreasure.ui.theme.WarnRedBright
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(
    state: MainUiState,
    onEditChests: () -> Unit,
    onToggleShowSquadInReport: (Boolean) -> Unit,
    onToggleShowTeamTreasures: (Boolean) -> Unit,
    onToggleObtained: (String, Boolean) -> Unit,
    onUpdateMatchDetails: (sessionId: String, chestsOpened: Int, treasures: List<TreasureFindDto>, matchNumber: Int?, sessionNumber: Int?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportDataJson: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val lang = state.config.language
    val view = state.view
    val matches = remember(view.matches) {
        view.matches.sortedByDescending { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
    }
    val liveChests = if (state.liveMatch.active) state.liveMatch.chests else 0
    val liveTreasures = if (state.liveMatch.active) state.liveMatch.treasures.size else 0
    val totalChests = view.countedChests
    val grandTotal = view.totalChests + liveChests
    val teamChests = view.teamChests
    val avg = if (matches.isNotEmpty()) totalChests.toDouble() / matches.size else 0.0
    val best = matches.maxOfOrNull { it.chestsOpened } ?: 0
    val treasures = matches.sumOf { it.treasureFinds.size } + liveTreasures
    val teamTreasures = matches.sumOf { it.teamTreasureFinds.size }
    val sessionMatches = matches.filter {
        OverlayModel.isInSession(it, state.config.sessionNumber, state.config.sessionStartedEpochMs)
    }
    val session = sessionMatches.sumOf { it.chestsOpened } + liveChests
    val every = if (treasures > 0) ((totalChests + liveChests).toDouble() / treasures).roundToInt() else 0

    // Seen prize IDs in replays auto-tick collection items
    val seenPrizeIds = remember(matches) {
        matches.flatMap { it.treasureFinds + it.teamTreasureFinds }
            .mapNotNull { it.prizeId?.lowercase() }
            .toSet()
    }
    val autoObtainedIds = remember(seenPrizeIds) {
        val allPrizes = listOf(TreasureCatalog.data.pirateShopAccess) + TreasureCatalog.data.weeks.flatMap { it.prizes }
        allPrizes.filter { p -> p.prizeIds.any { it.lowercase() in seenPrizeIds } }.map { it.id }.toSet()
    }
    val obtainedSet = remember(view.obtainedIds, autoObtainedIds) {
        view.obtainedIds.toSet() + autoObtainedIds
    }

    var selectedMatch by remember { mutableStateOf<MatchRecordDto?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Summary Tiles
        val manualLine = if (view.manualAdjust != 0) {
            val signed = if (view.manualAdjust > 0) "+${L.n(lang, view.manualAdjust)}" else L.n(lang, view.manualAdjust)
            L.t(lang, "dont $signed ajoutés à la main", "$signed of them added by hand", "de ellos, $signed añadidos a mano")
        } else null
        HeroChestCard(
            totalChests = L.n(lang, grandTotal),
            title = L.t(lang, "Coffres au total", "Chests in total", "Cofres en total"),
            subtitle = L.t(
                lang,
                "ouverts ou détruits par toi · ${L.n(lang, session)} depuis le lancement",
                "opened or destroyed by you · ${L.n(lang, session)} since the tool started",
                "abiertos o destruidos por ti · ${L.n(lang, session)} desde que se abrió",
            ),
            manualLine = manualLine,
            onEditClick = onEditChests,
            editContentDescription = L.t(lang, "Modifier mon nombre de coffres", "Edit my number of chests", "Modificar mi número de cofres"),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = L.n(lang, matches.size),
                label = L.t(lang, "parties analysées", "matches analyzed", "partidas analizadas"),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = L.f(lang, avg, 1),
                label = L.t(lang, "coffres par partie", "chests per match", "cofres por partida"),
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = L.n(lang, best),
                label = L.t(lang, "record en une partie", "best match", "récord en una partida"),
                modifier = Modifier.weight(1f),
            )
            val treasureSub = buildList {
                if (every > 0) add(L.t(lang, "1 tous les ${L.n(lang, every)} coffres", "1 every ${L.n(lang, every)} chests", "1 cada ${L.n(lang, every)} cofres"))
                if (teamTreasures > 0) add(L.t(lang, "+$teamTreasures par l'équipe (estimation)", "+$teamTreasures by the team (estimate)", "+$teamTreasures del equipo (estimación)"))
            }.joinToString("\n").ifEmpty { null }
            StatTile(
                value = L.n(lang, treasures),
                label = L.t(lang, "trésors trouvés", "treasures found", "tesoros encontrados"),
                sub = treasureSub,
                valueColor = PurpleStar,
                modifier = Modifier.weight(1f),
            )
        }

        if (teamChests > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    L.t(lang, "Afficher le total d'équipe", "Show the squad total", "Mostrar el total del equipo"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentInkBright,
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state.config.showSquadInReport,
                    onCheckedChange = onToggleShowSquadInReport,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealAccent),
                    modifier = Modifier.testTag("report_squad_switch"),
                )
            }
            if (state.config.showSquadInReport) {
                StatTile(
                    value = L.n(lang, grandTotal + teamChests),
                    label = L.t(lang, "total de l'équipe", "squad total", "total del equipo"),
                    sub = L.t(
                        lang,
                        "toi + ${L.n(lang, teamChests)} par tes coéquipiers",
                        "you + ${L.n(lang, teamChests)} by your teammates",
                        "tú + ${L.n(lang, teamChests)} de tus compañeros",
                    ),
                    badgeText = L.t(lang, "imprécis", "inaccurate", "impreciso"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Chests Per Day 30-day Bar Chart
        ChestsPerDayChart(
            matches = matches,
            nowUtc = state.nowUtc,
            timeZone = state.config.timeZone,
            lang = lang,
        )

        // By Chest Type
        SectionHeading(L.t(lang, "Par type de coffre", "By chest type", "Por tipo de cofre"))
        ParchmentCard {
            val allChests = matches.flatMap { it.chests }
            val byKind = allChests.groupBy { ChestKinds.canonical(it.kind) }.entries.sortedByDescending { it.value.size }
            val kindMax = maxOf(1, byKind.maxOfOrNull { it.value.size } ?: 1)
            val otherKinds = matches.flatMap { m ->
                val map = if (m.otherInteractions.isNotEmpty()) m.otherInteractions
                else mapOf(ChestKinds.AMMO_BOX to m.ammoBoxesOpened, ChestKinds.OTHER_CONTAINER to m.otherContainersOpened)
                map.entries
            }.groupBy { it.key }.mapValues { (_, list) -> list.sumOf { it.value } }.filter { it.value > 0 }
                .entries.sortedByDescending { it.value }

            if (byKind.isEmpty() && otherKinds.isEmpty()) {
                Text(
                    text = L.t(lang, "Pas encore de détail par type de coffre.", "No chest breakdown yet.", "Sin desglose de cofres aún."),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            } else {
                byKind.forEachIndexed { idx, (kind, items) ->
                    if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                    val barCol = Color(ChestKinds.colorHex(kind))
                    val frac = (items.size.toFloat() / kindMax.toFloat()).coerceIn(0.02f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(barCol, RoundedCornerShape(2.dp)))
                        Text(L.name(kind, lang), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Box(modifier = Modifier.width(100.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardTrack)) {
                            Box(modifier = Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barCol))
                        }
                        Text(L.n(lang, items.size), style = MaterialTheme.typography.titleMedium)
                    }
                }
                val destroyedTotal = allChests.count { it.destroyed }
                otherKinds.forEach { (kind, count) ->
                    HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${L.name(kind, lang)} ${L.t(lang, "(hors total)", "(not in the total)", "(fuera del total)")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CardMuted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(L.n(lang, count), style = MaterialTheme.typography.bodyMedium, color = CardMuted)
                    }
                }
                if (destroyedTotal > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = L.t(
                            lang,
                            "dont ${L.n(lang, destroyedTotal)} détruits (cassés au lieu d'être ouverts)",
                            "of which ${L.n(lang, destroyedTotal)} destroyed (broken instead of opened)",
                            "de ellos, ${L.n(lang, destroyedTotal)} destruidos (rotos en lugar de abiertos)",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
            }
        }

        // Where Your Treasures Come From
        val treasureRows = remember(matches) { matches.flatMap { m -> m.treasureFinds.map { m to it } } }
        val teamRows = remember(matches) { matches.flatMap { m -> m.teamTreasureFinds.map { m to it } } }
        SectionHeading(L.t(lang, "D'où viennent tes trésors", "Where your treasures come from", "De dónde vienen tus tesoros"))
        ParchmentCard {
            if (treasureRows.isEmpty()) {
                Text(
                    L.t(lang, "Pas encore de trésor.", "No treasure yet.", "Todavía ningún tesoro."),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            } else {
                val grouped = treasureRows.groupBy { it.second.sourceLabel(lang) }.entries.sortedByDescending { it.value.size }
                grouped.forEachIndexed { idx, (srcLabel, list) ->
                    if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                    val isChest = list.first().second.sourceIsChest
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isChest) srcLabel else "$srcLabel · ${L.t(lang, "fiabilité faible", "low reliability", "fiabilidad baja")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChest) CardInk else WarnCrimson,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = L.n(lang, list.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isChest) CardInk else WarnCrimson,
                        )
                    }
                }
            }
        }

        // Your Chances
        ReportChancesSection(state = state, obtainedSet = obtainedSet, onOpenUrl = onOpenUrl)

        // Island Loot Odds Calculator
        LootOddsCalculatorSection(state = state, initiallyOpen = true)

        // They Got It In Your Matches (Experimental)
        val seenRows = remember(matches) {
            matches.flatMap { m ->
                m.seenPrizes.mapNotNull { s ->
                    val prize = TreasureCatalog.find(s.prizeId) ?: return@mapNotNull null
                    if (prize.kind == TreasureCatalog.PIRATE_SHOP || prize.kind == TreasureCatalog.GOLD_OUTFIT) {
                        Triple(m, s, prize)
                    } else null
                }
            }
        }
        CollapsibleSection(
            title = "${L.t(lang, "Ils l'ont eu dans tes parties", "They got it in your matches", "Lo consiguieron en tus partidas")} · ${seenRows.size}",
            initiallyOpen = false,
            badge = L.t(lang, "expérimental", "experimental", "experimental"),
            infoText = L.t(
                lang,
                "Un coffre doré est envoyé à tous les joueurs à environ 150 m, avec le trésor et le joueur qui l'a ouvert.",
                "A golden chest is sent to every player within about 150 m, with the treasure and the player who opened it.",
                "Un cofre dorado se envía a todos los jugadores a unos 150 m, con el tesoro y el jugador que lo abrió.",
            ),
            lang = lang,
        ) {
            ParchmentCard {
                if (seenRows.isEmpty()) {
                    Text(
                        L.t(
                            lang,
                            "Aucun pour l'instant : quand un joueur à moins de 150 m de toi décroche l'accès à la boutique pirate ou une tenue en or, il apparaît ici.",
                            "None yet: when a player within 150 m of you gets the Pirate Shop Access or a gold outfit, they show up here.",
                            "Ninguno por ahora: cuando un jugador a menos de 150 m de ti consigue el acceso a la tienda pirata o un traje de oro, aparece aquí.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                } else {
                    seenRows.take(30).forEachIndexed { idx, (m, s, prize) ->
                        if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 6.dp))
                        val whenTxt = (L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc))?.let {
                            L.dateTimeShort(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                        } ?: ""
                        val name = if (prize.kind == TreasureCatalog.PIRATE_SHOP) TreasureCatalog.kindName(prize.kind, lang) else prize.name
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("★★ $name", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = AccentGoldText)
                                Text("${s.player} · $whenTxt", style = MaterialTheme.typography.bodySmall, color = CardMuted)
                            }
                            Text(Pretty.clock(s.timeSeconds), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Your Collection
        val allCollectionPrizes = remember {
            listOf(TreasureCatalog.data.pirateShopAccess) + TreasureCatalog.data.weeks.flatMap { it.prizes }
        }
        val obtainedCount = allCollectionPrizes.count { it.id in obtainedSet }
        CollapsibleSection(
            title = "${L.t(lang, "Ta collection", "Your collection", "Tu colección")} · $obtainedCount/${allCollectionPrizes.size}",
            initiallyOpen = false,
            infoText = L.t(
                lang,
                "Coche ce que tu as déjà obtenu. Un trésor sorti d'un coffre doré est coché tout seul quand le compteur le reconnaît.",
                "Tick what you already obtained. A treasure from a golden chest is ticked by itself when the counter recognizes it.",
                "Marca lo que ya has conseguido. Un tesoro de un cofre dorado se marca solo cuando el contador lo reconoce.",
            ),
            lang = lang,
        ) {
            ParchmentCard {
                Text(
                    L.t(lang, "Toute la saison", "All season", "Toda la temporada"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                CollectionPrizeGrid(
                    prizes = listOf(TreasureCatalog.data.pirateShopAccess),
                    obtainedSet = obtainedSet,
                    autoIds = autoObtainedIds,
                    lang = lang,
                    onToggle = onToggleObtained,
                )
                TreasureCatalog.data.weeks.forEach { week ->
                    HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 12.dp))
                    val current = !state.nowUtc.isBefore(week.from) && state.nowUtc.isBefore(week.to)
                    val dates = "${L.shortWhen(week.from, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)} – " +
                        L.shortWhen(week.to, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                    Text(
                        text = "${L.t(lang, "Semaine", "Week", "Semana")} ${week.number} · ${week.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (current) AccentGoldText else CardInk,
                    )
                    Text(
                        text = dates + if (current) " · ${L.t(lang, "cette semaine", "this week", "esta semana")}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    CollectionPrizeGrid(
                        prizes = week.prizes,
                        obtainedSet = obtainedSet,
                        autoIds = autoObtainedIds,
                        lang = lang,
                        onToggle = onToggleObtained,
                    )
                }
            }
        }

        // Treasures Found (Paginated)
        if (treasureRows.isNotEmpty() || teamRows.isNotEmpty()) {
            var treasureLimit by remember { mutableIntStateOf(10) }
            val visibleRows = remember(treasureRows, teamRows, state.config.showTeamTreasuresInReport) {
                if (state.config.showTeamTreasuresInReport) treasureRows.map { Triple(it.first, it.second, false) } +
                    teamRows.map { Triple(it.first, it.second, true) }
                else treasureRows.map { Triple(it.first, it.second, false) }
            }.sortedByDescending { L.parseUtc(it.first.matchStartUtc) ?: L.parseUtc(it.first.analyzedAtUtc) ?: Instant.EPOCH }

            CollapsibleSection(
                title = "${L.t(lang, "Trésors trouvés", "Treasures found", "Tesoros encontrados")} · $treasures",
                initiallyOpen = false,
                lang = lang,
            ) {
                ParchmentCard {
                    if (teamRows.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                L.t(lang, "Afficher aussi l'équipe (+${teamRows.size})", "Also show team (+${teamRows.size})", "Mostrar equipo (+${teamRows.size})"),
                                style = MaterialTheme.typography.bodySmall,
                                color = CardMuted,
                            )
                            Switch(
                                checked = state.config.showTeamTreasuresInReport,
                                onCheckedChange = onToggleShowTeamTreasures,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealAccent),
                            )
                        }
                    }
                    visibleRows.take(treasureLimit).forEachIndexed { idx, (m, f, isTeam) ->
                        if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                        val whenTxt = (L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc))?.let {
                            L.dateTimeShort(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                        } ?: ""
                        val stars = if (f.fromGoldenChest) "★★" else "★"
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$stars ${f.displayName(lang)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (f.fromGoldenChest) AccentGoldText else PurpleStar,
                                )
                                val who = if (!isTeam) L.t(lang, "toi", "you", "tú") else f.teammate.orEmpty().ifBlank { L.t(lang, "coéquipier", "teammate", "compañero") }
                                Text("$whenTxt · ${f.sourceLabel(lang)} · $who", style = MaterialTheme.typography.bodySmall, color = CardMuted)
                            }
                            Text(
                                text = f.obtainedAtSeconds?.let { Pretty.clock(it) } ?: "?",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (visibleRows.size > 10) {
                        PagerControls(
                            shown = minOf(treasureLimit, visibleRows.size),
                            total = visibleRows.size,
                            lang = lang,
                            onShowMore = { treasureLimit += 20 },
                            onShowAll = { treasureLimit = visibleRows.size },
                            onShowLess = { treasureLimit = 10 },
                        )
                    }
                }
            }
        }

        // Latest Matches (Paginated)
        var matchLimit by remember { mutableIntStateOf(10) }
        CollapsibleSection(
            title = "${L.t(lang, "Dernières parties", "Latest matches", "Últimas partidas")} · ${L.n(lang, matches.size)}",
            initiallyOpen = matches.isNotEmpty(),
            lang = lang,
        ) {
            ParchmentCard {
                if (matches.isEmpty()) {
                    Text(
                        L.t(lang, "Aucune partie analysée pour l'instant.", "No matches analyzed yet.", "Todavía no hay partidas analizadas."),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                } else {
                    matches.take(matchLimit).forEachIndexed { idx, m ->
                        if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                        val whenTxt = (L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc))?.let {
                            L.dateTimeShort(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                        } ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedMatch = m }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val numBadge = buildString {
                                    if (m.matchNumber != null) append("${L.t(lang, "Partie", "Match", "Partida")} ${m.matchNumber}")
                                    if (m.sessionNumber != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append("${L.t(lang, "Session", "Session", "Sesión")} ${m.sessionNumber}")
                                    }
                                }
                                val title = if (numBadge.isNotEmpty()) {
                                    "$numBadge · ${Pretty.playlist(m.playlist, lang)}"
                                } else {
                                    Pretty.playlist(m.playlist, lang)
                                }
                                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "$whenTxt · ${Pretty.placement(m, lang)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (m.placement == 1) AccentGoldText else CardMuted,
                                )
                                if (m.treasureFinds.isNotEmpty()) {
                                    Text(
                                        m.treasureFinds.joinToString(", ") { (if (it.fromGoldenChest) "★★ " else "★ ") + it.displayName(lang) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PurpleStar,
                                    )
                                } else {
                                    Text(
                                        L.t(lang, "Toucher pour choisir les trésors", "Tap to pick treasures", "Toca para elegir tesoros"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CardMuted,
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${L.n(lang, m.chestsOpened)}${if (m.vehicleChestsMissing) " ⚠" else ""}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (m.vehicleChestsMissing) WarnCrimson else CardInk,
                                )
                                if (state.config.showSquadInReport && m.teamChestsOpened > 0) {
                                    Text("+${L.n(lang, m.teamChestsOpened)}", style = MaterialTheme.typography.bodySmall, color = TealAccent)
                                }
                            }
                        }
                    }
                    if (matches.size > 10) {
                        PagerControls(
                            shown = minOf(matchLimit, matches.size),
                            total = matches.size,
                            lang = lang,
                            onShowMore = { matchLimit += 20 },
                            onShowAll = { matchLimit = matches.size },
                            onShowLess = { matchLimit = 10 },
                        )
                    }
                }
            }
        }

        // Export Actions
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LogbookSecondaryButton(
                text = L.t(lang, "Exporter matches.csv", "Export matches.csv", "Exportar matches.csv"),
                onClick = onExportCsv,
                modifier = Modifier.testTag("export_csv_button"),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Exporter data.json", "Export data.json", "Exportar data.json"),
                onClick = onExportDataJson,
                modifier = Modifier.testTag("export_data_json_button"),
            )
        }
    }

    selectedMatch?.let { match ->
        MatchDetailDialog(
            match = match,
            state = state,
            onDismiss = { selectedMatch = null },
            onSave = { chests, treasuresList, matchNum, sessionNum ->
                onUpdateMatchDetails(match.sessionId, chests, treasuresList, matchNum, sessionNum)
                selectedMatch = null
            },
            onDeleteMatch = {
                onDeleteMatch(match.sessionId)
                selectedMatch = null
            },
        )
    }
}

@Composable
private fun ChestsPerDayChart(
    matches: List<MatchRecordDto>,
    nowUtc: Instant,
    timeZone: String,
    lang: String,
) {
    val zone = L.zoneId(timeZone)
    val today = nowUtc.atZone(zone).toLocalDate()
    val days = remember(today) { (0 until 30).map { today.minusDays((29 - it).toLong()) } }
    val perDay = remember(days, matches, zone) {
        val grouped = matches.groupBy { m ->
            val inst = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH
            inst.atZone(zone).toLocalDate()
        }
        days.map { d -> grouped[d].orEmpty().sumOf { it.chestsOpened } }
    }
    val maxVal = maxOf(1, perDay.maxOrNull() ?: 1)
    val topIdx = perDay.indices.maxByOrNull { perDay[it] } ?: 0
    var selectedIdx by remember { mutableIntStateOf(topIdx) }

    SectionHeading(L.t(lang, "Coffres par jour", "Chests per day", "Cofres por día"))
    ParchmentCard {
        val tipDate = L.dayAxisLabel(days[selectedIdx], lang, includeDayOfWeek = true)
        val tipVal = L.count(lang, perDay[selectedIdx], "coffre", "chest", "cofre")
        Text(
            text = "$tipDate : $tipVal",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = CardInk,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(days) {
                    detectTapGestures { offset ->
                        val colW = size.width.toFloat() / days.size
                        val idx = (offset.x / colW).toInt().coerceIn(0, days.lastIndex)
                        selectedIdx = idx
                    }
                }
        ) {
            val count = days.size
            val gap = 3.dp.toPx()
            val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
            val maxH = size.height
            perDay.forEachIndexed { idx, v ->
                val x = idx * (barW + gap)
                val barH = if (v == 0) 3.dp.toPx() else (maxH * (v.toFloat() / maxVal)).coerceAtLeast(4.dp.toPx())
                val col = when {
                    v == 0 -> CardLine
                    v == maxVal -> WarnRedBright
                    else -> AccentGold
                }
                drawRoundRect(
                    color = col,
                    topLeft = Offset(x, maxH - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
        }
        HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(L.dayAxisLabel(days.first(), lang), style = MaterialTheme.typography.bodySmall, color = CardMuted)
            if (perDay[topIdx] > 0) {
                Text(
                    "${L.t(lang, "meilleur jour", "best day", "mejor día")}: ${L.dayAxisLabel(days[topIdx], lang)} · ${L.n(lang, perDay[topIdx])}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            }
            Text(L.t(lang, "aujourd'hui", "today", "hoy"), style = MaterialTheme.typography.bodySmall, color = CardMuted)
        }
    }
}

@Composable
internal fun CollectionPrizeGrid(
    prizes: List<TreasurePrize>,
    obtainedSet: Set<String>,
    autoIds: Set<String>,
    lang: String,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        prizes.chunked(2).forEach { rowPrizes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPrizes.forEach { prize ->
                    val auto = prize.id in autoIds
                    val got = prize.id in obtainedSet
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !auto) { onToggle(prize.id, !got) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ParchmentCardLight, contentColor = CardInk),
                        border = BorderStroke(if (got) 2.dp else 1.dp, if (got) TealAccent else CardLine),
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                TreasureThumbnail(
                                    imageUrl = prize.image,
                                    obtained = got,
                                    size = 96.dp,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                Checkbox(
                                    checked = got,
                                    onCheckedChange = if (auto) null else { checked -> onToggle(prize.id, checked) },
                                    colors = CheckboxDefaults.colors(checkedColor = TealAccent),
                                    modifier = Modifier.align(Alignment.TopStart).size(32.dp),
                                )
                            }
                            Text(prize.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 2)
                            Text(
                                text = TreasureCatalog.kindName(prize.kind, lang) +
                                    if (auto) " · ${L.t(lang, "vu", "seen", "visto")}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = CardMuted,
                            )
                        }
                    }
                }
                if (rowPrizes.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun PagerControls(
    shown: Int,
    total: Int,
    lang: String,
    onShowMore: () -> Unit,
    onShowAll: () -> Unit,
    onShowLess: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        HorizontalDivider(color = CardLine)
        Spacer(Modifier.height(6.dp))
        Text(
            L.t(lang, "$shown sur $total", "$shown of $total", "$shown de $total"),
            style = MaterialTheme.typography.bodySmall,
            color = CardMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown < total) {
                TextButton(onClick = onShowMore) {
                    Text(L.t(lang, "Afficher 20 de plus", "Show 20 more", "Mostrar 20 más"), color = HeroChestBg)
                }
                TextButton(onClick = onShowAll) {
                    Text(L.t(lang, "Tout afficher", "Show all", "Mostrar todo"), color = HeroChestBg)
                }
            }
            if (shown > 10) {
                TextButton(onClick = onShowLess) {
                    Text(L.t(lang, "Réduire", "Show less", "Reducir"), color = HeroChestBg)
                }
            }
        }
    }
}

