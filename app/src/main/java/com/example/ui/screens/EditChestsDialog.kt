package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.ChestEstimate
import com.example.domain.L
import com.example.domain.ManualEntryDto
import com.example.domain.TreasureCatalog
import com.example.ui.MainUiState
import com.example.ui.components.LogbookPrimaryButton
import com.example.ui.components.LogbookSecondaryButton
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.TealAccent
import com.example.ui.theme.WarnCrimson

@Composable
fun EditChestsDialog(
    state: MainUiState,
    onDismiss: () -> Unit,
    onAddManual: (Int, Boolean) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onComputeEstimate: (Int) -> Unit,
    onApplyEstimate: () -> Unit,
) {
    val lang = state.config.language
    var manualInput by remember { mutableStateOf("") }
    var thisWeek by remember { mutableStateOf(false) }
    var careerInput by remember { mutableStateOf("") }
    val currentWeek = remember(state.nowUtc) { TreasureCatalog.shownWeek(state.nowUtc) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ParchmentCard,
            contentColor = CardInk,
            border = BorderStroke(2.dp, AccentGold),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = L.t(lang, "Modifier mon nombre de coffres", "Edit my number of chests", "Modificar mi número de cofres"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = HeroChestBg,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("close_edit_chests_dialog"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = L.t(lang, "Fermer", "Close", "Cerrar"),
                            tint = CardInk,
                        )
                    }
                }

                Text(
                    text = L.t(
                        lang,
                        "Les coffres ouverts avant d'utiliser le compteur. Chaque nombre s'ajoute à ce que tu as déjà ajouté (un nombre négatif en retire) ; la croix annule un ajout.",
                        "The chests opened before using the counter. Each number is added to what you already added (a negative number removes some); the cross undoes an addition.",
                        "Los cofres abiertos antes de usar el contador. Cada número se suma a lo que ya añadiste (un número negativo quita cofres); la cruz anula un añadido.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
                Spacer(Modifier.height(10.dp))

                val manualSign = if (state.view.manualAdjust > 0) "+" else ""
                Text(
                    text = L.t(lang, "Ajoutés à la main", "Added by hand", "Añadidos a mano") +
                        ": $manualSign${L.n(lang, state.view.manualAdjust)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CardInk,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("0") },
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
                        modifier = Modifier.weight(1f).testTag("manual_chests_input"),
                    )
                    LogbookPrimaryButton(
                        text = L.t(lang, "Ajouter", "Add", "Añadir"),
                        onClick = {
                            val n = ChestEstimate.parseNumber(manualInput)
                            if (n != null && n != 0) {
                                onAddManual(n, thisWeek)
                                manualInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_manual_chests_button"),
                    )
                }

                if (currentWeek != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Checkbox(
                            checked = thisWeek,
                            onCheckedChange = { thisWeek = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealAccent),
                            modifier = Modifier.testTag("opened_this_week_checkbox"),
                        )
                        Column {
                            Text(
                                text = L.t(lang, "Ouverts cette semaine", "Opened this week", "Abiertos esta semana"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = L.t(
                                    lang,
                                    "comptent aussi pour les trésors hebdo de la semaine ${currentWeek.number}",
                                    "also count for the weekly treasures of week ${currentWeek.number}",
                                    "cuentan también para los tesoros semanales de la semana ${currentWeek.number}",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = CardMuted,
                            )
                        }
                    }
                }

                if (state.view.manualEntries.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        for (entry in state.view.manualEntries) {
                            HorizontalDivider(color = CardLine)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val sign = if (entry.chests > 0) "+" else ""
                                Text(
                                    text = "$sign${L.n(lang, entry.chests)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                                val what = when (entry.kind) {
                                    ManualEntryDto.ESTIMATE -> L.t(
                                        lang,
                                        "estimation (${entry.careerMatches ?: 0} parties)",
                                        "estimate (${entry.careerMatches ?: 0} matches)",
                                        "estimación (${entry.careerMatches ?: 0} partidas)",
                                    )
                                    ManualEntryDto.EARLIER -> L.t(lang, "ajouté plus tôt", "added earlier", "añadido antes")
                                    else -> L.t(lang, "à la main", "by hand", "a mano")
                                }
                                val whenTxt = L.parseUtc(entry.addedUtc)?.let {
                                    L.dateTimeShort(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                                }
                                Text(
                                    text = listOfNotNull(what, whenTxt).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CardMuted,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { onRemoveEntry(entry.id) },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = L.t(lang, "Annuler cet ajout", "Undo this addition", "Anular este añadido"),
                                        tint = WarnCrimson,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CardLine)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = L.t(lang, "Ou estimer à partir de tes parties", "Or estimate from your matches", "O estimar a partir de tus partidas"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = L.t(
                        lang,
                        "Nombre de parties Battle Royale de la saison, dans Fortnite : Carrière → Statistiques.",
                        "Number of Battle Royale matches this season, in Fortnite: Career → Stats.",
                        "Número de partidas de Battle Royale de la temporada, en Fortnite: Carrera → Estadísticas.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = careerInput,
                        onValueChange = { careerInput = it },
                        placeholder = { Text(L.t(lang, "Parties jouées", "Matches played", "Partidas jugadas")) },
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
                        modifier = Modifier.weight(1f).testTag("career_matches_input"),
                    )
                    LogbookSecondaryButton(
                        text = L.t(lang, "Calculer", "Compute", "Calcular"),
                        onClick = {
                            val n = ChestEstimate.parseNumber(careerInput) ?: 0
                            onComputeEstimate(n)
                        },
                        modifier = Modifier.testTag("compute_estimate_button"),
                    )
                }

                state.estimateMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    state.estimateResult?.let { res ->
                        Spacer(Modifier.height(8.dp))
                        LogbookPrimaryButton(
                            text = L.t(
                                lang,
                                "Ajouter ${L.n(lang, res.chests)} coffres",
                                "Add ${L.n(lang, res.chests)} chests",
                                "Añadir ${L.n(lang, res.chests)} cofres",
                            ),
                            onClick = onApplyEstimate,
                            modifier = Modifier.fillMaxWidth().testTag("apply_estimate_button"),
                        )
                    }
                }
            }
        }
    }
}
