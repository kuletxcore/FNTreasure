package com.kuletx.forttreasure.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.Pretty
import com.kuletx.forttreasure.domain.TreasureCatalog
import com.kuletx.forttreasure.ui.MainUiState
import com.kuletx.forttreasure.ui.components.CollapsibleSection
import com.kuletx.forttreasure.ui.components.LiveMatchSessionCard
import com.kuletx.forttreasure.ui.components.LogbookPrimaryButton
import com.kuletx.forttreasure.ui.components.LogbookSecondaryButton
import com.kuletx.forttreasure.ui.components.ParchmentCard
import com.kuletx.forttreasure.ui.components.SectionHeading
import com.kuletx.forttreasure.ui.components.StatTile
import com.kuletx.forttreasure.ui.theme.AccentGold
import com.kuletx.forttreasure.ui.theme.AccentGoldText
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.CardLine
import com.kuletx.forttreasure.ui.theme.CardMuted
import com.kuletx.forttreasure.ui.theme.LiveBoostText
import com.kuletx.forttreasure.ui.theme.PurpleStar
import com.kuletx.forttreasure.ui.theme.TealAccent
import com.kuletx.forttreasure.ui.theme.WarnBannerBg
import com.kuletx.forttreasure.ui.theme.WarnCrimson
import com.kuletx.forttreasure.ui.theme.WarnRedBright
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LootTrackerScreen(
    state: MainUiState,
    onEditChests: () -> Unit,
    onOpenFullReport: () -> Unit,
    onToggleShowTeamTreasures: (Boolean) -> Unit,
    onToggleObtained: (String, Boolean) -> Unit,
    onIncrementLiveChest: (Int) -> Unit,
    onAddLiveTreasure: (itemName: String, prizeId: String?, fromGoldenChest: Boolean) -> Unit,
    onRemoveLiveTreasure: (Int) -> Unit,
    onFinishLiveMatch: () -> Unit,
    onResetLiveMatch: () -> Unit,
    onStartNewSession: () -> Unit,
) {
    val lang = state.config.language
    val view = state.view
    val matches = remember(view.matches) {
        view.matches.sortedByDescending { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
    }
    val liveChests = if (state.liveMatch.active) state.liveMatch.chests else 0
    val liveTreasures = if (state.liveMatch.active) state.liveMatch.treasures.size else 0
    val totalChests = view.countedChests + liveChests
    val treasures = matches.sumOf { it.treasureFinds.size } + liveTreasures
    val teamTreasures = matches.sumOf { it.teamTreasureFinds.size }
    val every = if (treasures > 0) (totalChests.toDouble() / treasures).roundToInt() else 0

    val seenPrizeIds = remember(matches, state.liveMatch.treasures) {
        (matches.flatMap { it.treasureFinds + it.teamTreasureFinds } + state.liveMatch.treasures)
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
    val allCollectionPrizes = remember {
        listOf(TreasureCatalog.data.pirateShopAccess) + TreasureCatalog.data.weeks.flatMap { it.prizes }
    }
    val obtainedCount = allCollectionPrizes.count { it.id in obtainedSet }

    val treasureRows = remember(matches) { matches.flatMap { m -> m.treasureFinds.map { m to it } } }
    val teamRows = remember(matches) { matches.flatMap { m -> m.teamTreasureFinds.map { m to it } } }

    Column(
        modifier = Modifier.testTag("loot_tracker_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(L.t(lang, "Suivi de butin", "Loot Tracker", "Rastreador de botín"))

        LiveMatchSessionCard(
            state = state,
            onIncrementChest = onIncrementLiveChest,
            onAddTreasure = onAddLiveTreasure,
            onRemoveTreasure = onRemoveLiveTreasure,
            onFinishMatch = onFinishLiveMatch,
            onResetLiveMatch = onResetLiveMatch,
            onStartNewSession = onStartNewSession,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val treasureSub = buildList {
                if (every > 0) add(L.t(lang, "1 tous les ${L.n(lang, every)} coffres", "1 every ${L.n(lang, every)} chests", "1 cada ${L.n(lang, every)} cofres"))
                if (teamTreasures > 0) add(L.t(lang, "+$teamTreasures par l'équipe", "+$teamTreasures by team", "+$teamTreasures del equipo"))
            }.joinToString("\n").ifEmpty { null }

            StatTile(
                value = "★ ${L.n(lang, treasures)}",
                label = L.t(lang, "trésors trouvés", "treasures found", "tesoros encontrados"),
                sub = treasureSub,
                valueColor = PurpleStar,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = "$obtainedCount/${allCollectionPrizes.size}",
                label = L.t(lang, "collection obtenue", "collection obtained", "colección obtenida"),
                sub = "${L.n(lang, view.totalChests + liveChests)} ${L.t(lang, "coffres au total", "chests total", "cofres en total")}",
                valueColor = TealAccent,
                modifier = Modifier.weight(1f),
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LogbookPrimaryButton(
                text = L.t(lang, "Modifier les coffres", "Edit chests", "Editar cofres"),
                onClick = onEditChests,
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Rapport complet", "Full report", "Informe completo"),
                onClick = onOpenFullReport,
            )
        }

        // Where your treasures come from
        SectionHeading(L.t(lang, "Sources de tes trésors", "Treasure sources", "Origen de tus tesoros"))
        ParchmentCard {
            if (treasureRows.isEmpty()) {
                Text(
                    L.t(lang, "Pas encore de trésor enregistré.", "No treasure recorded yet.", "Todavía ningún tesoro registrado."),
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

        // Collection (initially open in Loot Tracker!)
        CollapsibleSection(
            title = "${L.t(lang, "Ta collection", "Your collection", "Tu colección")} · $obtainedCount/${allCollectionPrizes.size}",
            initiallyOpen = true,
            infoText = L.t(
                lang,
                "Coche ce que tu as déjà obtenu. Un trésor sorti d'un coffre doré est coché tout seul.",
                "Tick what you already obtained. Golden chest treasures tick themselves automatically.",
                "Marca lo que ya has conseguido. Los tesoros de cofre dorado se marcan solos.",
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
                    Text(
                        text = "${L.t(lang, "Semaine", "Week", "Semana")} ${week.number} · ${week.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (current) AccentGoldText else CardInk,
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

        // Treasures Found
        if (treasureRows.isNotEmpty() || teamRows.isNotEmpty()) {
            var treasureLimit by remember { mutableIntStateOf(10) }
            val visibleRows = remember(treasureRows, teamRows, state.config.showTeamTreasuresInReport) {
                if (state.config.showTeamTreasuresInReport) treasureRows.map { Triple(it.first, it.second, false) } +
                    teamRows.map { Triple(it.first, it.second, true) }
                else treasureRows.map { Triple(it.first, it.second, false) }
            }.sortedByDescending { L.parseUtc(it.first.matchStartUtc) ?: L.parseUtc(it.first.analyzedAtUtc) ?: Instant.EPOCH }

            CollapsibleSection(
                title = "${L.t(lang, "Trésors trouvés", "Treasures found", "Tesoros encontrados")} · $treasures",
                initiallyOpen = true,
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
    }
}

@Composable
fun ChestOddsCalculatorScreen(
    state: MainUiState,
    onOpenFullReport: () -> Unit,
) {
    val lang = state.config.language
    val now = state.nowUtc
    val catalog = TreasureCatalog.data
    val curHour = TreasureCatalog.currentTreasureHour(now)
    val nextHour = if (curHour == null) TreasureCatalog.nextTreasureHour(now) else null

    Column(
        modifier = Modifier.testTag("chest_odds_calculator_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeading(
                text = L.t(lang, "Calculateur de cotes", "Chest Odds Calculator", "Calculadora de probabilidades"),
                modifier = Modifier.weight(1f),
            )
            LogbookSecondaryButton(
                text = L.t(lang, "Rapport", "Report", "Informe"),
                onClick = onOpenFullReport,
            )
        }

        if (curHour != null) {
            val endClock = L.clock(curHour.endUtc, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
            val (cdText, _) = TreasureCatalog.countdownText(curHour.endUtc, now, lang)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = WarnBannerBg, contentColor = CardInk),
                border = BorderStroke(1.5.dp, WarnRedBright),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "⚡ " + L.t(
                            lang,
                            "Heure du trésor en cours, jusqu'à $endClock · se termine dans $cdText",
                            "Treasure Hour live now, until $endClock · ends in $cdText",
                            "Hora del tesoro en curso, hasta las $endClock · termina en $cdText",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = CardInk,
                    )
                    curHour.boosts.entries.forEach { (kind, boost) ->
                        val odds = catalog.odds.firstOrNull { it.kind == kind }
                        val per = odds?.let { L.n(lang, (it.oneIn / boost).roundToInt()) }
                        Text(
                            text = "${TreasureCatalog.kindName(kind, lang)} ×${L.f(lang, boost, if (boost % 1.0 == 0.0) 0 else 1)}" +
                                (per?.let { " · " + L.t(lang, "1 sur $it par coffre", "1 in $it per chest", "1 entre $it por cofre") } ?: ""),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LiveBoostText,
                        )
                    }
                }
            }
        } else if (nextHour != null) {
            val startTxt = L.dayTime(nextHour.startUtc, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
            val (cdText, _) = TreasureCatalog.countdownText(nextHour.startUtc, now, lang)
            ParchmentCard(borderColor = AccentGold) {
                Text(
                    text = L.t(
                        lang,
                        "Prochaine heure du trésor : $startTxt · dans $cdText",
                        "Next Treasure Hour: $startTxt · in $cdText",
                        "Próxima hora del tesoro: $startTxt · en $cdText",
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        LootOddsCalculatorContent(state = state)
    }
}
