package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LogbookColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = CardInk,
    primaryContainer = HeroChestBg,
    onPrimaryContainer = HeroChestInk,
    secondary = TealAccent,
    onSecondary = VerdictCertainFg,
    tertiary = PurpleStar,
    onTertiary = VerdictCertainFg,
    background = NightSeaBg,
    onBackground = ParchmentInk,
    surface = ParchmentCard,
    onSurface = CardInk,
    surfaceVariant = NightSeaSurface,
    onSurfaceVariant = NightMuted,
    outline = AccentGold,
    outlineVariant = CardLine,
    error = WarnRedBright,
    onError = VerdictCertainFg,
)

@Composable
fun ChestCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LogbookColorScheme,
        typography = ChestCounterTypography,
        content = content,
    )
}
