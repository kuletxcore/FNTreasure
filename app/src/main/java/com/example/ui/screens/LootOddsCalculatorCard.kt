package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.IslandLootItem
import com.example.domain.IslandLootOdds
import com.example.domain.L
import com.example.domain.LootOddsCalculation
import com.example.domain.TreasureCatalog
import com.example.domain.VerdictTier
import com.example.ui.MainUiState
import com.example.ui.components.CollapsibleSection
import com.example.ui.components.ParchmentCard
import com.example.ui.components.TagPill
import com.example.ui.components.TreasureThumbnail
import com.example.ui.components.VerdictChip
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldBright
import com.example.ui.theme.AccentGoldText
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.CardTrack
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.HeroChestInk
import com.example.ui.theme.HeroChestValue
import com.example.ui.theme.LiveBoostChipBg
import com.example.ui.theme.LiveBoostOrange
import com.example.ui.theme.LiveBoostText
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.TealAccent
import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun LootOddsCalculatorSection(
    state: MainUiState,
    initiallyOpen: Boolean = true,
) {
    val lang = state.config.language
    CollapsibleSection(
        title = L.t(
            lang,
            "Calculateur de chances par objet",
            "Island loot odds calculator",
            "Calculadora de probabilidades de botín",
        ),
        initiallyOpen = initiallyOpen,
        infoText = L.t(
            lang,
            "Calcule la probabilité cumulée 1 − (1 − p)^N de trouver un objet précis selon les taux officiels de l'île (content.json) ou un taux personnalisé.",
            "Calculates the cumulative probability 1 − (1 − p)^N of finding a specific item from chests using current island drop rate data (content.json) or a custom rate.",
            "Calcula la probabilidad acumulada 1 − (1 − p)^N de encontrar un objeto específico en cofres según los datos de la isla (content.json) o una tasa personalizada.",
        ),
        lang = lang,
    ) {
        LootOddsCalculatorContent(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LootOddsCalculatorContent(
    state: MainUiState,
    modifier: Modifier = Modifier,
) {
    val lang = state.config.language
    val now = state.nowUtc
    val catalog = TreasureCatalog.data
    val matches = state.view.matches

    val currentWeekNumber = remember(now, catalog.revision) {
        TreasureCatalog.shownWeek(now)?.number ?: catalog.weeks.lastOrNull()?.number ?: 1
    }

    var selectedWeekNumber by remember(currentWeekNumber) { mutableIntStateOf(currentWeekNumber) }
    var customMode by remember { mutableStateOf(false) }
    var customPercentText by remember { mutableStateOf("1.0") }

    val items = remember(now, selectedWeekNumber, lang, catalog.revision) {
        IslandLootOdds.catalogItems(now, selectedWeekNumber, lang)
    }

    var selectedItemId by remember(items) {
        mutableStateOf(
            items.firstOrNull { it.kind == TreasureCatalog.GOLD_OUTFIT }?.id
                ?: items.firstOrNull()?.id
                ?: ""
        )
    }
    val selectedItem: IslandLootItem = remember(items, selectedItemId) {
        items.firstOrNull { it.id == selectedItemId } ?: items.first()
    }

    val playerTotalChests = state.view.totalChests.coerceAtLeast(0)
    val playerAvgChests = if (matches.isNotEmpty()) {
        state.view.countedChests.toDouble() / matches.size
    } else null
    val week = TreasureCatalog.shownWeek(now)
    val weekChests = remember(matches, state.view.manualEntries, week) {
        if (week == null) 0
        else {
            val fromMatches = matches.flatMap { m ->
                val start = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH
                m.chests.map { start.plusMillis((it.timeSeconds * 1000).toLong()) }
            }.count { !it.isBefore(week.from) && it.isBefore(week.to) }
            val fromManual = state.view.manualEntries
                .filter { L.parseUtc(it.weekFromUtc) == week.from }
                .sumOf { it.chests }
                .coerceAtLeast(0)
            fromMatches + fromManual
        }
    }

    var chestsText by remember {
        mutableStateOf(
            when {
                weekChests > 0 -> weekChests.toString()
                playerTotalChests > 0 -> playerTotalChests.toString()
                else -> "100"
            }
        )
    }
    val chestsCount = (chestsText.toLongOrNull() ?: 0L).coerceIn(0L, 10_000_000L)

    val defaultApplyBoost = selectedItem.isBoostLive
    var applyBoost by remember(selectedItem.id) { mutableStateOf(defaultApplyBoost) }

    val calculation: LootOddsCalculation = remember(
        customMode,
        customPercentText,
        selectedItem,
        chestsCount,
        applyBoost,
        playerAvgChests,
        lang,
    ) {
        if (customMode) {
            val pct = customPercentText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.000001, 99.9999) ?: 1.0
            IslandLootOdds.calculateCustom(
                perChestPercent = pct,
                chests = chestsCount,
                avgChestsPerMatch = playerAvgChests,
                lang = lang,
            )
        } else {
            IslandLootOdds.calculate(
                item = selectedItem,
                chests = chestsCount,
                applyBoost = applyBoost,
                avgChestsPerMatch = playerAvgChests,
                lang = lang,
            )
        }
    }

    val allComparison = remember(items, chestsCount, applyBoost, playerAvgChests, lang) {
        items.map { item ->
            IslandLootOdds.calculate(
                item = item,
                chests = chestsCount,
                applyBoost = applyBoost && item.liveOrNextBoost != null,
                avgChestsPerMatch = playerAvgChests,
                lang = lang,
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ParchmentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = HeroChestBg,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L.t(
                            lang,
                            "Taux de l'île (experience_br · rév. ${catalog.revision})",
                            "Current island drop data (experience_br · rev. ${catalog.revision})",
                            "Datos actuales de la isla (experience_br · rev. ${catalog.revision})",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = CardInk,
                    )
                    Text(
                        text = L.t(
                            lang,
                            "Sélectionne un objet ou entre un taux personnalisé pour simuler N coffres.",
                            "Pick an island loot item or enter a custom drop rate to simulate N chests.",
                            "Elige un objeto de la isla o introduce una tasa para simular N cofres.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Week / Custom selector strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                catalog.weeks.forEach { w ->
                    val selected = !customMode && selectedWeekNumber == w.number
                    FilterChip(
                        selected = selected,
                        onClick = {
                            customMode = false
                            selectedWeekNumber = w.number
                        },
                        label = {
                            Text(
                                text = "${L.t(lang, "Sem.", "Wk", "Sem.")} ${w.number}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HeroChestBg,
                            selectedLabelColor = HeroChestInk,
                            containerColor = ParchmentCardLight,
                            labelColor = CardInk,
                        ),
                        modifier = Modifier.testTag("loot_calc_week_${w.number}"),
                    )
                }
                FilterChip(
                    selected = customMode,
                    onClick = { customMode = true },
                    label = {
                        Text(
                            text = L.t(lang, "Taux perso %", "Custom %", "Tasa %"),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealAccent,
                        selectedLabelColor = Color.White,
                        containerColor = ParchmentCardLight,
                        labelColor = CardInk,
                    ),
                    modifier = Modifier.testTag("loot_calc_custom_chip"),
                )
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(visible = !customMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var itemExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = itemExpanded,
                        onExpandedChange = { itemExpanded = !itemExpanded },
                    ) {
                        OutlinedTextField(
                            value = "${selectedItem.name} · ${TreasureCatalog.percentText(1.0 / selectedItem.baseOneIn, lang)}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(L.t(lang, "Objet du coffre", "Loot item from chest", "Objeto del cofre")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ParchmentCardLight,
                                unfocusedContainerColor = ParchmentCardLight,
                                focusedTextColor = CardInk,
                                unfocusedTextColor = CardInk,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = CardLine,
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("loot_calc_item_dropdown"),
                        )
                        ExposedDropdownMenu(
                            expanded = itemExpanded,
                            onDismissRequest = { itemExpanded = false },
                        ) {
                            items.forEach { item ->
                                val basePct = TreasureCatalog.percentText(1.0 / item.baseOneIn, lang)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            if (item.imageUrl != null) {
                                                TreasureThumbnail(imageUrl = item.imageUrl, obtained = true, size = 28.dp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    "${TreasureCatalog.kindName(item.kind, lang)} · $basePct (${L.t(lang, "1 sur", "1 in", "1 entre")} ${L.n(lang, item.baseOneIn)})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = CardMuted,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedItemId = item.id
                                        itemExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Treasure Hour multiplier chip when applicable
                    val boost = selectedItem.liveOrNextBoost
                    if (boost != null && boost > 1.0) {
                        val boostTxt = L.f(lang, boost, if (boost % 1.0 == 0.0) 0 else 1)
                        val boostLabel = if (selectedItem.isBoostLive) {
                            L.t(
                                lang,
                                "Appliquer Heure du trésor EN DIRECT (×$boostTxt)",
                                "Apply LIVE Treasure Hour boost (×$boostTxt)",
                                "Aplicar Hora del tesoro EN DIRECTO (×$boostTxt)",
                            )
                        } else {
                            L.t(
                                lang,
                                "Simuler Heure du trésor (×$boostTxt)",
                                "Simulate Treasure Hour boost (×$boostTxt)",
                                "Simular Hora del tesoro (×$boostTxt)",
                            )
                        }
                        FilterChip(
                            selected = applyBoost,
                            onClick = { applyBoost = !applyBoost },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(boostLabel, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LiveBoostChipBg,
                                selectedLabelColor = LiveBoostText,
                                selectedLeadingIconColor = LiveBoostOrange,
                                containerColor = ParchmentCardLight,
                                labelColor = CardInk,
                            ),
                            modifier = Modifier.testTag("loot_calc_boost_chip"),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = customMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customPercentText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                            customPercentText = filtered.take(10)
                        },
                        label = {
                            Text(L.t(lang, "Taux de drop par coffre (%)", "Drop rate per chest (%)", "Tasa por cofre (%)"))
                        },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = CardLine,
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("loot_calc_custom_rate_input"),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("0.05", "0.5", "1.0", "5.0", "15.0", "25.0").forEach { preset ->
                            Surface(
                                onClick = { customPercentText = preset },
                                shape = RoundedCornerShape(6.dp),
                                color = ParchmentCardLight,
                                border = BorderStroke(1.dp, CardLine),
                            ) {
                                Text(
                                    "$preset%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CardInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Number of chests input + logarithmic slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = chestsText,
                    onValueChange = { raw ->
                        chestsText = raw.filter { it.isDigit() }.take(7)
                    },
                    label = { Text(L.t(lang, "Coffres à ouvrir", "Chests to open", "Cofres a abrir")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ParchmentCardLight,
                        unfocusedContainerColor = ParchmentCardLight,
                        focusedTextColor = CardInk,
                        unfocusedTextColor = CardInk,
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = CardLine,
                    ),
                    modifier = Modifier.weight(1f).testTag("loot_calc_chests_input"),
                )
            }

            // Logarithmic slider 1 .. 5000 chests
            val maxLog = ln(5000.0).toFloat()
            val sliderPos = if (chestsCount <= 1L) 0f else (ln(chestsCount.coerceAtMost(5000L).toDouble()) / maxLog).toFloat()
            Slider(
                value = sliderPos.coerceIn(0f, 1f),
                onValueChange = { frac ->
                    val valInt = if (frac <= 0.01f) 1L else exp(frac * maxLog.toDouble()).roundToLong().coerceIn(1L, 5000L)
                    chestsText = valInt.toString()
                },
                colors = SliderDefaults.colors(
                    thumbColor = HeroChestBg,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = CardLine,
                ),
                modifier = Modifier.fillMaxWidth().testTag("loot_calc_chests_slider"),
            )

            // Quick chest presets
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = buildList {
                    add(1L to "1")
                    playerAvgChests?.roundToInt()?.takeIf { it > 1 }?.let { avg ->
                        add(avg.toLong() to "${L.t(lang, "Moy.", "Avg", "Med.")} ($avg)")
                    }
                    if (weekChests > 0) {
                        add(weekChests.toLong() to "${L.t(lang, "Sem.", "Week", "Sem.")} (${L.n(lang, weekChests)})")
                    }
                    if (playerTotalChests > 0 && playerTotalChests != weekChests) {
                        add(playerTotalChests.toLong() to "${L.t(lang, "Total", "Total", "Total")} (${L.n(lang, playerTotalChests)})")
                    }
                    add(100L to "100")
                    add(500L to "500")
                    add(1000L to "1 000")
                }
                presets.forEach { (value, label) ->
                    val active = chestsCount == value
                    Surface(
                        onClick = { chestsText = value.toString() },
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) HeroChestBg else ParchmentCardLight,
                        border = BorderStroke(1.dp, if (active) AccentGoldBright else CardLine),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (active) HeroChestInk else CardInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // Focused Result Hero Card
        val calc = calculation
        Card(
            modifier = Modifier.fillMaxWidth().testTag("loot_calc_result_card"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = HeroChestBg, contentColor = HeroChestInk),
            border = BorderStroke(2.dp, AccentGoldBright),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (calc.item.imageUrl != null) {
                        TreasureThumbnail(imageUrl = calc.item.imageUrl, obtained = true, size = 46.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = calc.item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = HeroChestValue,
                        )
                        val kindLabel = if (calc.item.kind == "custom") {
                            L.t(lang, "Taux personnalisé", "Custom drop rate", "Tasa personalizada")
                        } else {
                            TreasureCatalog.kindName(calc.item.kind, lang)
                        }
                        Text(
                            text = "$kindLabel · ${L.nLong(lang, calc.chests)} ${L.t(lang, "coffres", "chests", "cofres")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HeroChestInk.copy(alpha = 0.85f),
                        )
                    }
                    if (!customMode) {
                        TagPill(
                            text = if (calc.item.weekly) L.t(lang, "Hebdo", "Weekly", "Semanal") else L.t(lang, "Saison", "Season", "Temporada"),
                            weekly = calc.item.weekly,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = TreasureCatalog.percentText(calc.cumulativeP, lang),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp, lineHeight = 44.sp),
                        color = HeroChestValue,
                        modifier = Modifier.testTag("loot_calc_cumulative_pct"),
                    )
                    VerdictChip(calc.verdictTier, calc.verdictLabel)
                }

                val perChestPct = TreasureCatalog.percentText(calc.effectivePerChestP, lang)
                val oneInFormatted = L.nLong(lang, calc.effectiveOneIn.roundToLong().coerceAtLeast(1L))
                val boostSuffix = if (calc.appliedBoost > 1.0) {
                    " (⚡ ×${L.f(lang, calc.appliedBoost, if (calc.appliedBoost % 1.0 == 0.0) 0 else 1)})"
                } else ""
                Text(
                    text = L.t(
                        lang,
                        "Par coffre : $perChestPct · 1 sur $oneInFormatted$boostSuffix",
                        "Per chest: $perChestPct · 1 in $oneInFormatted$boostSuffix",
                        "Por cofre: $perChestPct · 1 entre $oneInFormatted$boostSuffix",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroChestInk,
                )

                val invText = if (calc.cumulativeP > 0.0 && calc.cumulativeP < 0.5) {
                    " · " + L.t(lang, "environ 1 sur", "about 1 in", "aprox. 1 entre") + " ${TreasureCatalog.oneInHuge(ln(calc.cumulativeP), lang)}"
                } else ""
                Text(
                    text = "${L.t(lang, "Quantité attendue", "Expected drops", "Apariciones esperadas")} : ${TreasureCatalog.expectedText(calc.expectedDrops, lang)}$invText",
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroChestInk.copy(alpha = 0.9f),
                )

                HorizontalDivider(color = AccentGold.copy(alpha = 0.45f))

                Text(
                    text = L.t(
                        lang,
                        "Coffres nécessaires pour atteindre :",
                        "Chests needed to reach target odds:",
                        "Cofres necesarios para alcanzar:",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF3CF7A),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    calc.milestones.forEach { m ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF5D3715),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "${(m.targetChance * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HeroChestValue,
                                )
                                Text(
                                    text = L.nLong(lang, m.chestsNeeded),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                if (m.matchesNeeded != null) {
                                    Text(
                                        text = "~${L.nLong(lang, m.matchesNeeded)} ${L.t(lang, "p.", "m.", "p.")}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = HeroChestInk.copy(alpha = 0.78f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Island Loot Table Comparison at N chests
        ParchmentCard {
            Text(
                text = L.t(
                    lang,
                    "Tous les objets de l'île sur ${L.nLong(lang, chestsCount)} coffres",
                    "All island loot odds over ${L.nLong(lang, chestsCount)} chests",
                    "Todas las probabilidades de la isla en ${L.nLong(lang, chestsCount)} cofres",
                ),
                style = MaterialTheme.typography.titleMedium,
                color = CardInk,
            )
            Text(
                text = L.t(
                    lang,
                    "Touche un objet pour l'analyser en détail.",
                    "Tap any item to inspect milestones and targets.",
                    "Toca un objeto para ver objetivos y detalle.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted,
            )
            Spacer(Modifier.height(8.dp))

            allComparison.forEachIndexed { idx, rowCalc ->
                if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                val isSelected = !customMode && rowCalc.item.id == selectedItem.id
                val barCol = when (rowCalc.verdictTier) {
                    VerdictTier.VUNLIKELY, VerdictTier.UNLIKELY -> AccentGold
                    VerdictTier.POSSIBLE -> Color(0xFFD68A1C)
                    VerdictTier.LIKELY, VerdictTier.VLIKELY -> Color(0xFF5F9C4A)
                    VerdictTier.CERTAIN -> Color(0xFF2F7D4F)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentGold.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable {
                            customMode = false
                            selectedItemId = rowCalc.item.id
                        }
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (rowCalc.item.imageUrl != null) {
                            TreasureThumbnail(imageUrl = rowCalc.item.imageUrl, obtained = true, size = 34.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rowCalc.item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CardInk,
                            )
                            val perPct = TreasureCatalog.percentText(rowCalc.effectivePerChestP, lang)
                            val boostTag = if (rowCalc.appliedBoost > 1.0) " · ⚡×${L.f(lang, rowCalc.appliedBoost, if (rowCalc.appliedBoost % 1.0 == 0.0) 0 else 1)}" else ""
                            Text(
                                text = "$perPct / ${L.t(lang, "coffre", "chest", "cofre")}$boostTag",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (rowCalc.appliedBoost > 1.0) LiveBoostText else CardMuted,
                            )
                        }
                        Text(
                            text = TreasureCatalog.percentText(rowCalc.cumulativeP, lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardInk,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerdictChip(rowCalc.verdictTier, rowCalc.verdictLabel)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CardTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(rowCalc.cumulativeP.toFloat().coerceIn(0.01f, 1f))
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barCol)
                            )
                        }
                    }
                }
            }
        }
    }
}
