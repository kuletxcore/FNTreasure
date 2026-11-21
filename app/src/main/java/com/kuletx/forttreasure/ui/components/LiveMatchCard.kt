package com.kuletx.forttreasure.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.OverlayModel
import com.kuletx.forttreasure.ui.MainUiState
import com.kuletx.forttreasure.ui.theme.AccentGold
import com.kuletx.forttreasure.ui.theme.AccentGoldText
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.CardLine
import com.kuletx.forttreasure.ui.theme.CardMuted
import com.kuletx.forttreasure.ui.theme.HeroChestBg
import com.kuletx.forttreasure.ui.theme.LiveGreen
import com.kuletx.forttreasure.ui.theme.ParchmentCard
import com.kuletx.forttreasure.ui.theme.ParchmentCardLight
import com.kuletx.forttreasure.ui.theme.PurpleStar
import com.kuletx.forttreasure.ui.theme.TealAccent
import com.kuletx.forttreasure.ui.theme.WarnCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveMatchSessionCard(
    state: MainUiState,
    onIncrementChest: (Int) -> Unit,
    onAddTreasure: (itemName: String, prizeId: String?, fromGoldenChest: Boolean) -> Unit,
    onRemoveTreasure: (Int) -> Unit,
    onFinishMatch: () -> Unit,
    onResetLiveMatch: () -> Unit,
    onStartNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lang = state.config.language
    val live = state.liveMatch
    val sessionNum = state.config.sessionNumber
    val sessionMatches = remember(state.view.matches, sessionNum, state.config.sessionStartedEpochMs) {
        state.view.matches.filter {
            OverlayModel.isInSession(it, sessionNum, state.config.sessionStartedEpochMs)
        }
    }
    val currentMatchNum = OverlayModel.nextMatchNumber(sessionMatches)
    val sessionChestsTotal = sessionMatches.sumOf { it.chestsOpened } + (if (live.active) live.chests else 0)
    var showTreasurePicker by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentCard, contentColor = CardInk),
        border = BorderStroke(1.5.dp, if (live.active) LiveGreen else AccentGold),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (live.active) LiveGreen else AccentGold)
                    )
                    Text(
                        text = "${L.t(lang, "Partie", "Match", "Partida")} $currentMatchNum · ${L.t(lang, "Session", "Session", "Sesión")} $sessionNum",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CardInk,
                    )
                }
                TextButton(
                    onClick = onStartNewSession,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("new_session_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = HeroChestBg,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = L.t(lang, "Nouvelle session", "New session", "Nueva sesión"),
                        color = HeroChestBg,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Chest counter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${L.n(lang, live.chests)} ${L.t(lang, "coffres dans cette partie", "chests in this match", "cofres en esta partida")}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = HeroChestBg,
                    )
                    Text(
                        text = "${L.t(lang, "Total session", "Session chests", "Cofres sesión")}: ${L.n(lang, sessionChestsTotal)} · ${L.count(lang, sessionMatches.size, "terminée", "terminées", "finished", "finished", "terminada", "terminadas")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }

                Surface(
                    onClick = { onIncrementChest(-1) },
                    enabled = live.chests > 0,
                    shape = RoundedCornerShape(8.dp),
                    color = ParchmentCardLight,
                    border = BorderStroke(1.dp, AccentGold),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).testTag("live_minus_chest_button"),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text("-1", style = MaterialTheme.typography.titleMedium, color = CardInk)
                    }
                }

                Surface(
                    onClick = { onIncrementChest(1) },
                    shape = RoundedCornerShape(8.dp),
                    color = AccentGold,
                    border = BorderStroke(1.dp, HeroChestBg),
                    modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 48.dp).testTag("live_plus_chest_button"),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                        Text("+1", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CardInk)
                    }
                }
            }

            // Chosen treasures in active match
            if (live.treasures.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    live.treasures.forEachIndexed { idx, find ->
                        Surface(
                            onClick = { onRemoveTreasure(idx) },
                            shape = RoundedCornerShape(999.dp),
                            color = PurpleStar.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, PurpleStar),
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "${if (find.fromGoldenChest) "★★ " else "★ "}${find.displayName(lang)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (find.fromGoldenChest) AccentGoldText else PurpleStar,
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = L.t(lang, "Retirer", "Remove", "Quitar"),
                                    tint = WarnCrimson,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Pick treasure & Finish Match buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { showTreasurePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleStar,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("live_pick_treasure_button"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = L.t(lang, "Choisir trésor", "Pick treasure", "Elegir tesoro"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }

                if (live.active || live.chests > 0 || live.treasures.isNotEmpty()) {
                    Surface(
                        onClick = onFinishMatch,
                        shape = RoundedCornerShape(8.dp),
                        color = TealAccent,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("live_finish_match_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = L.t(lang, "Terminer partie $currentMatchNum", "Finish Match $currentMatchNum", "Terminar partida $currentMatchNum"),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }

                    Surface(
                        onClick = onResetLiveMatch,
                        shape = RoundedCornerShape(8.dp),
                        color = ParchmentCardLight,
                        border = BorderStroke(1.dp, CardLine),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("live_reset_match_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = WarnCrimson, modifier = Modifier.size(16.dp))
                            Text(
                                text = L.t(lang, "Effacer", "Reset", "Reiniciar"),
                                style = MaterialTheme.typography.labelLarge,
                                color = WarnCrimson,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTreasurePicker) {
        TreasurePickerDialog(
            nowUtc = state.nowUtc,
            lang = lang,
            onDismiss = { showTreasurePicker = false },
            onPick = { opt ->
                onAddTreasure(opt.itemName, opt.prizeId, opt.fromGoldenChest)
                showTreasurePicker = false
            },
        )
    }
}
