package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.domain.ChestKinds
import com.example.domain.L
import com.example.domain.MatchRecordDto
import com.example.domain.Pretty
import com.example.domain.TreasureCatalog
import com.example.domain.TreasureFindDto
import com.example.ui.MainUiState
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldText
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.PurpleStar
import com.example.ui.theme.TealAccent
import com.example.ui.theme.WarnCrimson
import java.time.Instant

data class TreasurePickOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val itemName: String,
    val prizeId: String?,
    val fromGoldenChest: Boolean,
    val imageUrl: String?,
)

object TreasureChoices {
    fun quickOverlayOptions(nowUtc: Instant, lang: String): List<TreasurePickOption> {
        val currentWeek = TreasureCatalog.shownWeek(nowUtc) ?: TreasureCatalog.data.weeks.firstOrNull()
        val list = mutableListOf<TreasurePickOption>()

        val jam = currentWeek?.prizes?.firstOrNull { it.kind == TreasureCatalog.JAM_TRACK }
        list += if (jam != null) {
            TreasurePickOption(
                id = jam.id,
                title = "${L.t(lang, "Piste", "Jam Track", "Pista")}: ${jam.name}",
                subtitle = TreasureCatalog.kindName(jam.kind, lang),
                itemName = jam.name,
                prizeId = jam.prizeIds.firstOrNull(),
                fromGoldenChest = true,
                imageUrl = jam.image,
            )
        } else {
            TreasurePickOption(
                id = "generic_jam",
                title = L.t(lang, "Piste musicale", "Jam Track", "Pista musical"),
                subtitle = L.t(lang, "Trésor", "Treasure", "Tesoro"),
                itemName = "Piste musicale",
                prizeId = null,
                fromGoldenChest = true,
                imageUrl = null,
            )
        }

        list += TreasurePickOption(
            id = "gizmo_any",
            title = L.t(lang, "Gizmo (n'importe)", "Gizmo (any)", "Artilugio"),
            subtitle = L.t(lang, "Gizmo", "Gizmo", "Artilugio"),
            itemName = "Gizmo",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        )
        list += gizmoOptions(lang).filter { it.id != "gizmo_any" }

        currentWeek?.prizes?.filter { it.kind != TreasureCatalog.JAM_TRACK }?.forEach { p ->
            list += TreasurePickOption(
                id = p.id,
                title = p.name,
                subtitle = TreasureCatalog.kindName(p.kind, lang),
                itemName = p.name,
                prizeId = p.prizeIds.firstOrNull(),
                fromGoldenChest = true,
                imageUrl = p.image,
            )
        }

        val pirate = TreasureCatalog.data.pirateShopAccess
        list += TreasurePickOption(
            id = pirate.id,
            title = TreasureCatalog.kindName(pirate.kind, lang),
            subtitle = L.t(lang, "Saison entière", "All season", "Toda la temporada"),
            itemName = pirate.name,
            prizeId = pirate.prizeIds.firstOrNull(),
            fromGoldenChest = true,
            imageUrl = pirate.image,
        )
        return list
    }

    fun gizmoOptions(lang: String): List<TreasurePickOption> = listOf(
        TreasurePickOption(
            id = "gizmo_any",
            title = L.t(lang, "Gizmo (n'importe lequel)", "Gizmo (any)", "Artilugio (cualquiera)"),
            subtitle = L.t(lang, "Gizmo général", "Any gizmo", "Artilugio general"),
            itemName = "Gizmo",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
        TreasurePickOption(
            id = "gizmo_firework",
            title = L.name("Feu d'artifice doré", lang),
            subtitle = "Gizmo",
            itemName = "Feu d'artifice doré",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
        TreasurePickOption(
            id = "gizmo_extractor",
            title = L.name("Extracteur portable", lang),
            subtitle = "Gizmo",
            itemName = "Extracteur portable",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
        TreasurePickOption(
            id = "gizmo_sprite",
            title = L.name("Localisateur d'esprit", lang),
            subtitle = "Gizmo",
            itemName = "Localisateur d'esprit",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
        TreasurePickOption(
            id = "gizmo_lucky",
            title = L.name("Localisateur chanceux", lang),
            subtitle = "Gizmo",
            itemName = "Localisateur chanceux",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
        TreasurePickOption(
            id = "gizmo_accel",
            title = L.name("Accélérateur d'extraction", lang),
            subtitle = "Gizmo",
            itemName = "Accélérateur d'extraction",
            prizeId = null,
            fromGoldenChest = false,
            imageUrl = null,
        ),
    )

    fun catalogOptions(
        nowUtc: Instant,
        lang: String,
        includeAllWeeks: Boolean = false,
    ): List<TreasurePickOption> {
        val out = mutableListOf<TreasurePickOption>()
        val shownWeek = TreasureCatalog.shownWeek(nowUtc)
        val weeks = if (includeAllWeeks) {
            TreasureCatalog.data.weeks
        } else {
            listOfNotNull(shownWeek)
        }
        weeks.forEach { week ->
            week.prizes.forEach { p ->
                out += TreasurePickOption(
                    id = p.id,
                    title = p.name,
                    subtitle = "${TreasureCatalog.kindName(p.kind, lang)} · W${week.number}",
                    itemName = p.name,
                    prizeId = p.prizeIds.firstOrNull(),
                    fromGoldenChest = true,
                    imageUrl = p.image,
                )
            }
        }
        val pirate = TreasureCatalog.data.pirateShopAccess
        out += TreasurePickOption(
            id = pirate.id,
            title = TreasureCatalog.kindName(pirate.kind, lang),
            subtitle = L.t(lang, "Toute la saison", "All season", "Toda la temporada"),
            itemName = pirate.name,
            prizeId = pirate.prizeIds.firstOrNull(),
            fromGoldenChest = true,
            imageUrl = pirate.image,
        )
        out += gizmoOptions(lang)
        return out
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreasurePickerDialog(
    nowUtc: Instant,
    lang: String,
    onDismiss: () -> Unit,
    onPick: (TreasurePickOption) -> Unit,
) {
    var showAllWeeks by remember { mutableStateOf(false) }
    val options = remember(nowUtc, lang, showAllWeeks) {
        TreasureChoices.catalogOptions(nowUtc, lang, showAllWeeks)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ParchmentCard,
        titleContentColor = HeroChestBg,
        textContentColor = CardInk,
        title = {
            Text(
                text = L.t(lang, "Choisir un trésor obtenu", "Pick a treasure you got", "Elige un tesoro conseguido"),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = L.t(lang, "Toutes les semaines", "All weeks", "Todas las semanas"),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                    Switch(
                        checked = showAllWeeks,
                        onCheckedChange = { showAllWeeks = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealAccent),
                    )
                }

                options.forEach { opt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(opt) }
                            .testTag("pick_treasure_${opt.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ParchmentCardLight, contentColor = CardInk),
                        border = BorderStroke(1.dp, if (opt.fromGoldenChest) AccentGold else CardLine),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (opt.imageUrl != null) {
                                TreasureThumbnail(imageUrl = opt.imageUrl, obtained = true, size = 38.dp)
                            } else {
                                Surface(
                                    color = PurpleStar.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = PurpleStar,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = CardInk,
                                )
                                Text(
                                    text = opt.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CardMuted,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = L.t(lang, "Ajouter", "Add", "Añadir"),
                                tint = HeroChestBg,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L.t(lang, "Fermer", "Close", "Cerrar"), color = HeroChestBg)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchDetailDialog(
    match: MatchRecordDto,
    state: MainUiState,
    onDismiss: () -> Unit,
    onSave: (chestsOpened: Int, treasures: List<TreasureFindDto>, matchNumber: Int?, sessionNumber: Int?) -> Unit,
    onDeleteMatch: () -> Unit,
) {
    val lang = state.config.language
    var chestText by remember(match.sessionId) { mutableStateOf(match.chestsOpened.toString()) }
    var matchNumText by remember(match.sessionId) { mutableStateOf(match.matchNumber?.toString() ?: "") }
    var sessionNumText by remember(match.sessionId) { mutableStateOf(match.sessionNumber?.toString() ?: "") }
    val draftTreasures = remember(match.sessionId) {
        mutableStateListOf<TreasureFindDto>().apply { addAll(match.treasureFinds) }
    }
    var showAllWeeks by remember { mutableStateOf(false) }
    val pickOptions = remember(state.nowUtc, lang, showAllWeeks) {
        TreasureChoices.catalogOptions(state.nowUtc, lang, showAllWeeks)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ParchmentCard,
        titleContentColor = HeroChestBg,
        textContentColor = CardInk,
        title = {
            val header = buildString {
                if (match.matchNumber != null || match.sessionNumber != null) {
                    val m = match.matchNumber?.let { "${L.t(lang, "Partie", "Match", "Partida")} $it" }
                    val s = match.sessionNumber?.let { "${L.t(lang, "Session", "Session", "Sesión")} $it" }
                    append(listOfNotNull(m, s).joinToString(" · "))
                    append(" — ")
                }
                append(Pretty.playlist(match.playlist, lang))
            }
            Text(header, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${L.t(lang, "Source", "Source", "Origen")}: ${match.replayFile}", style = MaterialTheme.typography.bodySmall, color = CardMuted)
                if (match.placement != null) {
                    Text("${L.t(lang, "Place", "Placement", "Puesto")}: ${Pretty.placement(match, lang)}", style = MaterialTheme.typography.bodySmall)
                }

                // Chests stepper + field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = chestText,
                        onValueChange = { chestText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(L.t(lang, "Coffres", "Chests", "Cofres")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                        ),
                        modifier = Modifier.weight(1f).testTag("dialog_chests_input"),
                    )
                    Surface(
                        onClick = {
                            val cur = chestText.toIntOrNull() ?: 0
                            chestText = maxOf(0, cur - 1).toString()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = ParchmentCardLight,
                        border = BorderStroke(1.dp, AccentGold),
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Text("-1", style = MaterialTheme.typography.titleMedium, color = CardInk)
                        }
                    }
                    Surface(
                        onClick = {
                            val cur = chestText.toIntOrNull() ?: 0
                            chestText = (cur + 1).toString()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = HeroChestBg,
                        border = BorderStroke(1.dp, AccentGold),
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Text("+1", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }

                // Match # & Session # explicit fields
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = matchNumText,
                        onValueChange = { matchNumText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(L.t(lang, "N° partie", "Match #", "N.º partida")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = sessionNumText,
                        onValueChange = { sessionNumText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(L.t(lang, "N° session", "Session #", "N.º sesión")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = CardLine)

                Text(
                    text = L.t(
                        lang,
                        "Trésors de cette partie (${draftTreasures.size})",
                        "Treasures in this match (${draftTreasures.size})",
                        "Tesoros de esta partida (${draftTreasures.size})",
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PurpleStar,
                )

                if (draftTreasures.isEmpty()) {
                    Text(
                        text = L.t(
                            lang,
                            "Aucun trésor sélectionné. Touche un trésor ci-dessous pour l'ajouter.",
                            "No treasures picked yet. Tap any item below to add it.",
                            "Ningún tesoro seleccionado. Toca un objeto abajo para añadirlo.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        draftTreasures.forEachIndexed { index, find ->
                            Surface(
                                onClick = { draftTreasures.removeAt(index) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = L.t(lang, "Ajouter un trésor :", "Pick treasures to add:", "Añadir tesoro:"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CardInk,
                    )
                    TextButton(onClick = { showAllWeeks = !showAllWeeks }) {
                        Text(
                            text = if (showAllWeeks) L.t(lang, "Semaine act.", "This week", "Esta semana")
                            else L.t(lang, "Toutes sem.", "All weeks", "Todas"),
                            color = HeroChestBg,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pickOptions.forEach { opt ->
                        Surface(
                            onClick = {
                                draftTreasures.add(
                                    TreasureFindDto(
                                        item = opt.itemName,
                                        rawItem = opt.prizeId ?: opt.itemName,
                                        prizeId = opt.prizeId,
                                        fromGoldenChest = opt.fromGoldenChest,
                                        sourceKind = ChestKinds.NORMAL,
                                        sourceIsChest = true,
                                        sourceCertain = true,
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = ParchmentCardLight,
                            border = BorderStroke(1.dp, if (opt.fromGoldenChest) AccentGold else CardLine),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("dialog_add_treasure_${opt.id}"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "+ ${opt.title}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = CardInk,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            LogbookPrimaryButton(
                text = L.t(lang, "Enregistrer", "Save", "Guardar"),
                onClick = {
                    onSave(
                        chestText.toIntOrNull() ?: match.chestsOpened,
                        draftTreasures.toList(),
                        matchNumText.toIntOrNull() ?: match.matchNumber,
                        sessionNumText.toIntOrNull() ?: match.sessionNumber,
                    )
                },
                modifier = Modifier.testTag("dialog_save_match_button"),
            )
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDeleteMatch) {
                    Text(L.t(lang, "Supprimer", "Delete", "Eliminar"), color = WarnCrimson)
                }
                TextButton(onClick = onDismiss) {
                    Text(L.t(lang, "Fermer", "Close", "Cerrar"), color = CardInk)
                }
            }
        },
    )
}
