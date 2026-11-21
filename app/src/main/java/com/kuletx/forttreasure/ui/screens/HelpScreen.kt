package com.kuletx.forttreasure.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuletx.forttreasure.domain.AppInfo
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.ui.MainUiState
import com.kuletx.forttreasure.ui.components.LogbookPrimaryButton
import com.kuletx.forttreasure.ui.components.LogbookSecondaryButton
import com.kuletx.forttreasure.ui.components.ParchmentCard
import com.kuletx.forttreasure.ui.components.SectionHeading
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.CardMuted
import com.kuletx.forttreasure.ui.theme.HeroChestBg
import com.kuletx.forttreasure.ui.theme.ParchmentCard
import com.kuletx.forttreasure.ui.theme.WarnCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(
    state: MainUiState,
    onLaunchExperienceBr: () -> Unit,
    onExportCsv: () -> Unit,
    onExportDataJson: () -> Unit,
    onClearAllData: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val lang = state.config.language
    var confirmClear by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ParchmentCard {
            Text(
                text = L.t(
                    lang,
                    "Suis tes coffres et trésors sur Fortnite Android (experience_br) grâce au compteur de partie en direct et à l'overlay flottant. Aucune donnée n'est envoyée : pas de connexion à ton compte Epic, aucune télémétrie.",
                    "Track your chests and treasures on Fortnite Android (experience_br) using the live match counter and floating overlay. No data is sent: no Epic account login, no telemetry.",
                    "Sigue tus cofres y tesoros en Fortnite Android (experience_br) con el contador en vivo y el overlay flotante. No se envían datos: sin inicio de sesión de Epic ni telemetría.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = CardInk,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = L.t(
                    lang,
                    "Fortnite sur Android ne prend pas en charge l'enregistrement ou la lecture des rediffusions (.replay). Tu peux toutefois importer ou exporter ton fichier data.json pour partager ton historique avec la version PC.",
                    "Fortnite on Android does not support recording or playing replays (.replay). You can still import or export data.json to share your history with the desktop app.",
                    "Fortnite en Android no permite grabar ni reproducir repeticiones (.replay). Aun así puedes importar o exportar data.json para compartir tu historial con la app de PC.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted,
            )
            Spacer(Modifier.height(10.dp))
            LogbookPrimaryButton(
                text = L.t(lang, "Ouvrir experience_br (com.epicgames.fortnite)", "Open experience_br (com.epicgames.fortnite)", "Abrir experience_br (com.epicgames.fortnite)"),
                onClick = onLaunchExperienceBr,
                modifier = Modifier.testTag("help_launch_experience_br"),
            )
        }

        SectionHeading(L.t(lang, "Sauvegarde et projet", "Backup & project", "Copia y proyecto"))
        ParchmentCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LogbookSecondaryButton(
                    text = L.t(lang, "Exporter matches.csv", "Export matches.csv", "Exportar matches.csv"),
                    onClick = onExportCsv,
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Exporter data.json", "Export data.json", "Exportar data.json"),
                    onClick = onExportDataJson,
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Page du projet (GitHub)", "Project page (GitHub)", "Página del proyecto (GitHub)"),
                    onClick = { onOpenUrl(AppInfo.REPO_URL) },
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Signaler un problème (GitHub)", "Report a problem (GitHub)", "Informar de un problema (GitHub)"),
                    onClick = { onOpenUrl(AppInfo.ISSUES_URL) },
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Tout supprimer", "Delete everything", "Borrarlo todo"),
                    onClick = { confirmClear = true },
                    danger = true,
                    modifier = Modifier.testTag("clear_all_data_button"),
                )
            }
        }

        SectionHeading(L.t(lang, "Journal d'erreurs", "Error log", "Registro de errores"))
        ParchmentCard {
            val txt = state.errorLogText.ifBlank {
                L.t(lang, "Aucune erreur enregistrée.", "No errors recorded.", "Sin errores registrados.")
            }
            Text(
                text = txt,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = if (state.errorLogText.isBlank()) CardMuted else WarnCrimson,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = ParchmentCard,
            titleContentColor = WarnCrimson,
            textContentColor = CardInk,
            title = { Text(L.t(lang, "Tout supprimer ?", "Delete everything?", "¿Borrarlo todo?")) },
            text = {
                Text(
                    L.t(
                        lang,
                        "Effacer toutes les parties enregistrées, les coffres ajoutés à la main et la collection ?",
                        "Erase all saved matches, manual chest entries, and collection ticks?",
                        "¿Borrar todas las partidas guardadas, los cofres manuales y la colección?",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearAllData()
                    }
                ) {
                    Text(L.t(lang, "Tout supprimer", "Delete everything", "Borrarlo todo"), color = WarnCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(L.t(lang, "Annuler", "Cancel", "Cancelar"), color = CardInk)
                }
            },
        )
    }
}
