package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.L
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldBright
import com.example.ui.theme.CardInk
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.HeroChestInk
import com.example.ui.theme.HeroChestValue
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.TealAccent

enum class QuickViewMode {
    LOGBOOK,
    LOOT_TRACKER,
    CHEST_ODDS_CALCULATOR,
}

private data class FabMenuOption(
    val mode: QuickViewMode,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

@Composable
fun QuickSwitchFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    activeMode: QuickViewMode?,
    lang: String,
    onSelectMode: (QuickViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = expanded) {
        onExpandedChange(false)
    }

    val options = listOf(
        FabMenuOption(
            mode = QuickViewMode.LOOT_TRACKER,
            label = L.t(lang, "Suivi de butin", "Loot Tracker", "Rastreador de botín"),
            icon = Icons.Default.AutoAwesome,
            testTag = "fab_option_loot_tracker",
        ),
        FabMenuOption(
            mode = QuickViewMode.CHEST_ODDS_CALCULATOR,
            label = L.t(lang, "Calculateur de cotes", "Chest Odds Calculator", "Calculadora de probabilidades"),
            icon = Icons.Default.Calculate,
            testTag = "fab_option_chest_odds",
        ),
        FabMenuOption(
            mode = QuickViewMode.LOGBOOK,
            label = L.t(lang, "Journal de bord", "Logbook", "Cuaderno de bitácora"),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            testTag = "fab_option_logbook",
        ),
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fab_icon_rotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                expandVertically(spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(initialScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) +
                scaleOut(targetScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                options.forEach { opt ->
                    val isActive = activeMode == opt.mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            onClick = {
                                onSelectMode(opt.mode)
                                onExpandedChange(false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) AccentGold else ParchmentCard,
                            contentColor = CardInk,
                            border = BorderStroke(1.5.dp, if (isActive) HeroChestBg else AccentGold),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag(opt.testTag),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CardInk,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                    ),
                                    color = CardInk,
                                )
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = {
                                onSelectMode(opt.mode)
                                onExpandedChange(false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = if (isActive) TealAccent else HeroChestBg,
                            contentColor = if (isActive) Color.White else HeroChestValue,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.label,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, AccentGoldBright),
            color = Color.Transparent,
            shadowElevation = 8.dp,
        ) {
            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(16.dp),
                containerColor = HeroChestBg,
                contentColor = HeroChestValue,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier.testTag("quick_nav_fab"),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Explore,
                    contentDescription = if (expanded) {
                        L.t(lang, "Fermer le menu rapide", "Close quick switch menu", "Cerrar menú rápido")
                    } else {
                        L.t(lang, "Changer de vue rapidement", "Quick switch views", "Cambiar de vista")
                    },
                    modifier = Modifier.rotate(if (expanded) 0f else iconRotation),
                )
            }
        }
    }
}
