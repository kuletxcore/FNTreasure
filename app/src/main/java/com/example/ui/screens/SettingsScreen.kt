package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FortniteLauncher
import com.example.domain.L
import com.example.ui.MainUiState
import com.example.ui.components.LogbookPrimaryButton
import com.example.ui.components.LogbookSecondaryButton
import com.example.ui.components.ParchmentCard
import com.example.ui.components.SectionHeading
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    onLanguageChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
    onClockFormatChange: (String) -> Unit,
    onProfileChange: (String) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onPickDemosFolder: () -> Unit,
    onRescanDemosFolder: () -> Unit,
    onPickReplays: () -> Unit,
    onImportDataJson: () -> Unit,
) {
    val lang = state.config.language
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ParchmentCard {
            // Language
            var langOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = langOpen, onExpandedChange = { langOpen = !langOpen }) {
                OutlinedTextField(
                    value = L.nativeName(lang),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(L.t(lang, "Langue", "Language", "Idioma")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langOpen) },
                    colors = dropdownFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("settings_language_dropdown"),
                )
                ExposedDropdownMenu(expanded = langOpen, onDismissRequest = { langOpen = false }) {
                    L.codes.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(L.nativeName(code)) },
                            onClick = { onLanguageChange(code); langOpen = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // Time zone
            var tzOpen by remember { mutableStateOf(false) }
            val tzLabel = if (state.config.timeZone == "utc") {
                L.t(lang, "UTC (heure du jeu)", "UTC (game time)", "UTC (hora del juego)")
            } else {
                L.t(lang, "Heure locale (${L.localOffsetLabel()})", "Local time (${L.localOffsetLabel()})", "Hora local (${L.localOffsetLabel()})")
            }
            ExposedDropdownMenuBox(expanded = tzOpen, onExpandedChange = { tzOpen = !tzOpen }) {
                OutlinedTextField(
                    value = tzLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(L.t(lang, "Heures affichées", "Times shown in", "Horas mostradas en")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzOpen) },
                    colors = dropdownFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = tzOpen, onDismissRequest = { tzOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "Heure locale (${L.localOffsetLabel()})", "Local time (${L.localOffsetLabel()})", "Hora local (${L.localOffsetLabel()})")) },
                        onClick = { onTimeZoneChange("local"); tzOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "UTC (heure du jeu)", "UTC (game time)", "UTC (hora del juego)")) },
                        onClick = { onTimeZoneChange("utc"); tzOpen = false },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            // Clock format
            var clockOpen by remember { mutableStateOf(false) }
            val sysClk = if (state.isSystem24Hour) L.t(lang, "24 h", "24-hour", "24 h") else L.t(lang, "12 h", "12-hour", "12 h")
            val clockLabel = when (state.config.clockFormat) {
                "12" -> L.t(lang, "12 heures (10:54 PM)", "12-hour (10:54 PM)", "12 horas (10:54 PM)")
                "24" -> L.t(lang, "24 heures (22:54)", "24-hour (22:54)", "24 horas (22:54)")
                else -> L.t(lang, "Comme Android ($sysClk)", "Same as Android ($sysClk)", "Como Android ($sysClk)")
            }
            ExposedDropdownMenuBox(expanded = clockOpen, onExpandedChange = { clockOpen = !clockOpen }) {
                OutlinedTextField(
                    value = clockLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(L.t(lang, "Horloge", "Clock", "Reloj")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clockOpen) },
                    colors = dropdownFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = clockOpen, onDismissRequest = { clockOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "Comme Android ($sysClk)", "Same as Android ($sysClk)", "Como Android ($sysClk)")) },
                        onClick = { onClockFormatChange("auto"); clockOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "12 heures (10:54 PM)", "12-hour (10:54 PM)", "12 horas (10:54 PM)")) },
                        onClick = { onClockFormatChange("12"); clockOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "24 heures (22:54)", "24-hour (22:54)", "24 horas (22:54)")) },
                        onClick = { onClockFormatChange("24"); clockOpen = false },
                    )
                }
            }

            if (state.profiles.size > 1) {
                Spacer(Modifier.height(12.dp))
                var profOpen by remember { mutableStateOf(false) }
                val cur = state.currentProfile
                ExposedDropdownMenuBox(expanded = profOpen, onExpandedChange = { profOpen = !profOpen }) {
                    OutlinedTextField(
                        value = cur?.let { "${it.name} (${it.matches} · ${it.chests})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(L.t(lang, "Compte (profil)", "Account (profile)", "Cuenta (perfil)")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profOpen) },
                        colors = dropdownFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = profOpen, onDismissRequest = { profOpen = false }) {
                        state.profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} (${p.matches} · ${p.chests})") },
                                onClick = { onProfileChange(p.key); profOpen = false },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        L.t(lang, "Actualiser le catalogue au lancement", "Refresh treasure catalog on startup", "Actualizar catálogo al iniciar"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        L.t(
                            lang,
                            "Révision actuelle : ${state.catalogRevision} (content.json sur GitHub)",
                            "Current revision: ${state.catalogRevision} (content.json on GitHub)",
                            "Revisión actual: ${state.catalogRevision} (content.json en GitHub)",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
                Switch(
                    checked = state.config.autoRefreshCatalog,
                    onCheckedChange = onAutoRefreshChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealAccent),
                    modifier = Modifier.testTag("auto_refresh_catalog_switch"),
                )
            }
        }

        SectionHeading(L.t(lang, "Dossier des replays", "Replays folder", "Carpeta de repeticiones"))
        ParchmentCard {
            Text(
                text = L.t(
                    lang,
                    "Sur Android, Fortnite enregistre généralement dans : ${FortniteLauncher.ANDROID_DEMOS_HINT}",
                    "On Android, Fortnite stores replays under: ${FortniteLauncher.ANDROID_DEMOS_HINT}",
                    "En Android, Fortnite guarda las repeticiones en: ${FortniteLauncher.ANDROID_DEMOS_HINT}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.config.demosTreeUri ?: L.t(lang, "Aucun dossier sélectionné", "No folder selected", "Ninguna carpeta seleccionada"),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = CardInk,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LogbookPrimaryButton(
                    text = L.t(lang, "Choisir un dossier", "Choose folder", "Elegir carpeta"),
                    onClick = onPickDemosFolder,
                )
                if (state.config.demosTreeUri != null) {
                    LogbookSecondaryButton(
                        text = L.t(lang, "Analyser le dossier", "Scan folder", "Escanear carpeta"),
                        onClick = onRescanDemosFolder,
                    )
                }
                LogbookSecondaryButton(
                    text = L.t(lang, "Choisir .replay", "Pick .replay files", "Elegir .replay"),
                    onClick = onPickReplays,
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Importer data.json", "Import data.json", "Importar data.json"),
                    onClick = onImportDataJson,
                )
            }
        }
    }
}

@Composable
private fun dropdownFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ParchmentCardLight,
    unfocusedContainerColor = ParchmentCardLight,
    focusedTextColor = CardInk,
    unfocusedTextColor = CardInk,
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = CardLine,
)
