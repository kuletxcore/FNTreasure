package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.L
import com.example.domain.MatchRecordDto
import com.example.domain.OverlayAppearance
import com.example.domain.OverlayModel
import com.example.domain.OverlayPreviewState
import com.example.domain.OverlayView
import com.example.domain.TreasureFindDto
import com.example.ui.MainUiState
import com.example.ui.components.ChestVectorIcon
import com.example.ui.components.LogbookPrimaryButton
import com.example.ui.components.LogbookSecondaryButton
import com.example.ui.components.ParchmentCard
import com.example.ui.components.SectionHeading
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.ClassicBoxBg
import com.example.ui.theme.ClassicBoxBorder
import com.example.ui.theme.ClassicLiveGreen
import com.example.ui.theme.HudLiveRed
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.ParchmentCardLight
import com.example.ui.theme.PurpleStarLight
import com.example.ui.theme.TealAccent
import com.example.ui.theme.WarnBadgeBg
import com.example.ui.theme.WarnCrimson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OverlayScreen(
    state: MainUiState,
    onStyleChange: (String) -> Unit,
    onSizeChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onShowSquadChange: (Boolean) -> Unit,
    onPreviewStateChange: (OverlayPreviewState) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onStartFloatingOverlay: (OverlayView) -> Unit,
    onStopFloatingOverlay: () -> Unit,
    onCopyObsHtml: (OverlayView) -> Unit,
    onSaveObsHtml: (OverlayView) -> Unit,
) {
    val lang = state.config.language
    val previewMatch = remember(state.overlayPreviewState) {
        if (state.overlayPreviewState == OverlayPreviewState.LIVE) {
            MatchRecordDto(
                chestsOpened = 12,
                treasureFinds = listOf(TreasureFindDto(item = "Trésor")),
            )
        } else null
    }
    val overlayView = remember(
        state.view,
        state.liveMatch,
        previewMatch,
        state.overlayPreviewState,
        state.config.showSquadTotal,
        state.config.sessionNumber,
        state.config.sessionStartedEpochMs,
        lang,
    ) {
        if (previewMatch != null) {
            OverlayModel.build(
                view = state.view,
                live = previewMatch,
                inProgress = true,
                showSquad = state.config.showSquadTotal,
                lang = lang,
                sessionNumber = state.config.sessionNumber,
                sessionStartedEpochMs = state.config.sessionStartedEpochMs,
            )
        } else {
            OverlayModel.buildLive(
                view = state.view,
                liveMatch = state.liveMatch,
                inProgressHint = state.overlayPreviewState != OverlayPreviewState.IDLE || state.liveMatch.active,
                sessionNumber = state.config.sessionNumber,
                sessionStartedEpochMs = state.config.sessionStartedEpochMs,
                showSquad = state.config.showSquadTotal,
                lang = lang,
            )
        }
    }

    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        onStartFloatingOverlay(overlayView)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Floating Overlay (Display over other apps)
        SectionHeading(L.t(lang, "Overlay flottant Android", "Android Floating Overlay", "Overlay flotante Android"))
        ParchmentCard(borderColor = AccentGold) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L.t(
                            lang,
                            "Afficher par-dessus d'autres applications",
                            "Display over other apps",
                            "Mostrar sobre otras aplicaciones",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = CardInk,
                    )
                    Text(
                        text = L.t(
                            lang,
                            "Permet d'afficher le compteur par-dessus Fortnite Mobile.",
                            "Allows showing the chest counter floating above Fortnite Mobile.",
                            "Permite mostrar el contador flotando sobre Fortnite Mobile.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
                Surface(
                    color = if (state.canDrawOverlays) LiveGreen.copy(alpha = 0.18f) else WarnBadgeBg,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (state.canDrawOverlays) {
                            L.t(lang, "Autorisé", "Granted", "Permitido")
                        } else {
                            L.t(lang, "Requis", "Required", "Requerido")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.canDrawOverlays) LiveGreen else WarnCrimson,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!state.canDrawOverlays) {
                    LogbookPrimaryButton(
                        text = L.t(
                            lang,
                            "Autoriser la superposition",
                            "Grant overlay permission",
                            "Permitir superposición",
                        ),
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier.testTag("grant_overlay_permission_button"),
                    )
                } else {
                    LogbookPrimaryButton(
                        text = if (state.floatingOverlayActive) {
                            L.t(lang, "Masquer l'overlay flottant", "Hide floating overlay", "Ocultar overlay flotante")
                        } else {
                            L.t(lang, "Afficher l'overlay flottant", "Show floating overlay", "Mostrar overlay flotante")
                        },
                        onClick = {
                            if (state.floatingOverlayActive) {
                                onStopFloatingOverlay()
                            } else if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onStartFloatingOverlay(overlayView)
                            }
                        },
                        modifier = Modifier.testTag("toggle_floating_overlay_button"),
                    )
                    LogbookSecondaryButton(
                        text = L.t(lang, "Paramètres système", "Overlay settings", "Ajustes del sistema"),
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier.testTag("overlay_settings_button"),
                    )
                }
            }
        }

        // Live Preview Surface
        SectionHeading(L.t(lang, "Aperçu", "Preview", "Vista previa"))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF182A38),
            border = BorderStroke(1.dp, AccentGold),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        OverlayPreviewState.IDLE to L.t(lang, "Inactif", "Idle", "Inactivo"),
                        OverlayPreviewState.IN_PROGRESS to L.t(lang, "En cours", "In progress", "En curso"),
                        OverlayPreviewState.LIVE to L.t(lang, "En direct (+12)", "Live (+12)", "En directo (+12)"),
                    ).forEach { (st, label) ->
                        val active = state.overlayPreviewState == st
                        Surface(
                            onClick = { onPreviewStateChange(st) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) AccentGold else Color.Transparent,
                            border = BorderStroke(1.dp, AccentGold),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (active) CardInk else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                val scale = (state.config.overlaySize / 100f).coerceIn(0.7f, 1.3f)
                val alpha = (state.config.overlayOpacity / 100f).coerceIn(0.3f, 1f)
                Box(
                    modifier = Modifier.fillMaxWidth().alpha(alpha),
                    contentAlignment = if (state.config.overlayStyle == "hud") Alignment.CenterEnd else Alignment.Center,
                ) {
                    if (state.config.overlayStyle == "classic") {
                        ClassicOverlayPreview(overlayView, scale, lang)
                    } else {
                        HudOverlayPreview(overlayView, scale, lang)
                    }
                }
            }
        }

        // Overlay Settings Card
        ParchmentCard {
            Text(L.t(lang, "Overlay en jeu & OBS", "In-game & OBS overlay", "Overlay y OBS"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            var styleOpen by remember { mutableStateOf(false) }
            val styleLabel = if (state.config.overlayStyle == "classic") {
                L.t(lang, "Classique (carte sombre)", "Classic (dark card)", "Clásico (tarjeta oscura)")
            } else {
                L.t(lang, "HUD (intégré au jeu)", "HUD (blends into the game)", "HUD (integrado en el juego)")
            }
            ExposedDropdownMenuBox(expanded = styleOpen, onExpandedChange = { styleOpen = !styleOpen }) {
                OutlinedTextField(
                    value = styleLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(L.t(lang, "Style", "Style", "Estilo")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleOpen) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ParchmentCardLight,
                        unfocusedContainerColor = ParchmentCardLight,
                        focusedTextColor = CardInk,
                        unfocusedTextColor = CardInk,
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = CardLine,
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("overlay_style_dropdown"),
                )
                ExposedDropdownMenu(expanded = styleOpen, onDismissRequest = { styleOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "HUD (intégré au jeu)", "HUD (blends into the game)", "HUD (integrado en el juego)")) },
                        onClick = { onStyleChange("hud"); styleOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text(L.t(lang, "Classique (carte sombre)", "Classic (dark card)", "Clásico (tarjeta oscura)")) },
                        onClick = { onStyleChange("classic"); styleOpen = false },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                var sizeOpen by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = sizeOpen, onExpandedChange = { sizeOpen = !sizeOpen }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = "${state.config.overlaySize} %",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(L.t(lang, "Taille", "Size", "Tamaño")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeOpen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = sizeOpen, onDismissRequest = { sizeOpen = false }) {
                        OverlayAppearance.sizes.forEach { sz ->
                            DropdownMenuItem(
                                text = { Text("$sz %") },
                                onClick = { onSizeChange(sz); sizeOpen = false },
                            )
                        }
                    }
                }

                var opOpen by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = opOpen, onExpandedChange = { opOpen = !opOpen }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = "${state.config.overlayOpacity} %",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(L.t(lang, "Opacité", "Opacity", "Opacidad")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = opOpen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ParchmentCardLight,
                            unfocusedContainerColor = ParchmentCardLight,
                            focusedTextColor = CardInk,
                            unfocusedTextColor = CardInk,
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = opOpen, onDismissRequest = { opOpen = false }) {
                        OverlayAppearance.opacities.forEach { op ->
                            DropdownMenuItem(
                                text = { Text("$op %") },
                                onClick = { onOpacityChange(op); opOpen = false },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(L.t(lang, "Afficher le total d'équipe", "Show the squad total", "Mostrar el total del equipo"), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        L.t(lang, "Tes coffres + ceux de tous tes coéquipiers.", "Your chests + those of all your teammates.", "Tus cofres + los de todos tus compañeros."),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
                Switch(
                    checked = state.config.showSquadTotal,
                    onCheckedChange = onShowSquadChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealAccent),
                    modifier = Modifier.testTag("overlay_squad_switch"),
                )
            }
        }

        // OBS Export Card
        SectionHeading(L.t(lang, "Overlay pour OBS (stream)", "Overlay for OBS (stream)", "Overlay para OBS (directo)"))
        ParchmentCard {
            Text(
                text = L.t(
                    lang,
                    "Dans OBS : Sources → + → Navigateur, coche « Fichier local », choisis overlay.html. Largeur 360, hauteur 220 (450 × 300 en classique).",
                    "In OBS: Sources → + → Browser, tick \"Local file\", pick overlay.html. Width 360, height 220 (450 × 300 for classic).",
                    "En OBS: Fuentes → + → Navegador, marca «Archivo local», elige overlay.html. Ancho 360, alto 220 (450 × 300 en clásico).",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LogbookPrimaryButton(
                    text = L.t(lang, "Enregistrer overlay.html", "Save overlay.html", "Guardar overlay.html"),
                    onClick = { onSaveObsHtml(overlayView) },
                    modifier = Modifier.testTag("save_overlay_html_button"),
                )
                LogbookSecondaryButton(
                    text = L.t(lang, "Copier HTML", "Copy HTML", "Copiar HTML"),
                    onClick = { onCopyObsHtml(overlayView) },
                    modifier = Modifier.testTag("copy_overlay_html_button"),
                )
            }
        }
    }
}

@Composable
private fun HudOverlayPreview(v: OverlayView, scale: Float, lang: String) {
    val shadow = Shadow(color = Color(0xFF0B0F18), blurRadius = 6f)
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (v.currentLive) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(HudLiveRed))
            }
            ChestVectorIcon(size = (32 * scale).dp)
            Text(
                text = v.total,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = (40 * scale).sp,
                    fontStyle = FontStyle.Italic,
                    shadow = shadow,
                ),
                color = if (v.currentLive && v.current != null && v.current != "…") Color(0xFFFFD23F) else Color.White,
            )
        }
        val rowStyle = MaterialTheme.typography.titleMedium.copy(
            fontSize = (18 * scale).sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            shadow = shadow,
        )
        Text("${L.t(lang, "Dernière", "Last", "Última")} • ${v.last ?: "—"}", style = rowStyle, color = Color.White)
        Text("${L.t(lang, "Session", "Session", "Sesión")} • ${v.session}", style = rowStyle, color = Color.White)
        v.squad?.let {
            Text("${OverlayModel.squadLabel(lang)} • $it", style = rowStyle, color = Color.White)
        }
        Text("★ ${v.treasures}", style = rowStyle, color = PurpleStarLight)
    }
}

@Composable
private fun ClassicOverlayPreview(v: OverlayView, scale: Float, lang: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ClassicBoxBg,
        border = BorderStroke(1.dp, ClassicBoxBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    L.t(lang, "COFFRES", "CHESTS", "COFRES"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9696A2),
                )
                v.badge?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFF60D680))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column {
                    Text(
                        v.total,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = (36 * scale).sp, lineHeight = (40 * scale).sp),
                        color = Color(0xFFF2B84B),
                    )
                    Text("total", style = MaterialTheme.typography.bodySmall, color = Color(0xFF82828E))
                }
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(Color(0xFF2C2C34)))
                Column {
                    Text(
                        v.current ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = (36 * scale).sp, lineHeight = (40 * scale).sp),
                        color = when {
                            v.current == null -> Color(0xFF50505C)
                            v.currentLive -> ClassicLiveGreen
                            else -> Color(0xFFE2E2E8)
                        },
                    )
                    Text(v.currentCaption, style = MaterialTheme.typography.bodySmall, color = Color(0xFF82828E))
                }
            }
            HorizontalDivider(color = Color(0xFF2C2C34), modifier = Modifier.padding(vertical = 8.dp))
            v.rows.forEach { r ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(r.label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF82828E))
                    Text(
                        r.value + (r.detail?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (r.treasure) Color(0xFFC4A0FF) else Color(0xFFE2E2E8),
                    )
                }
            }
        }
    }
}
