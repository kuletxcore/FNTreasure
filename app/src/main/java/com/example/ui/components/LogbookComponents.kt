package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.L
import com.example.domain.Notice
import com.example.domain.TreasureCatalog
import com.example.domain.VerdictTier
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldBright
import com.example.ui.theme.CardInk
import com.example.ui.theme.CardLine
import com.example.ui.theme.CardMuted
import com.example.ui.theme.CardTrack
import com.example.ui.theme.HeroChestBg
import com.example.ui.theme.HeroChestInk
import com.example.ui.theme.HeroChestValue
import com.example.ui.theme.NightMuted
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentInkBright
import com.example.ui.theme.PurpleStar
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TitleGold
import com.example.ui.theme.VerdictCertainBg
import com.example.ui.theme.VerdictCertainFg
import com.example.ui.theme.VerdictLikelyBg
import com.example.ui.theme.VerdictLikelyFg
import com.example.ui.theme.VerdictPossibleBg
import com.example.ui.theme.VerdictPossibleFg
import com.example.ui.theme.VerdictUnlikelyBg
import com.example.ui.theme.VerdictUnlikelyFg
import com.example.ui.theme.VerdictVLikelyBg
import com.example.ui.theme.VerdictVLikelyFg
import com.example.ui.theme.VerdictVUnlikelyBg
import com.example.ui.theme.VerdictVUnlikelyFg
import com.example.ui.theme.WarnBannerBg
import com.example.ui.theme.WarnRedBright
import com.example.ui.theme.WeeklyBlue

@Composable
fun ChestVectorIcon(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.06f
        // Chest lid
        val lidPath = Path().apply {
            moveTo(w * 0.11f, h * 0.46f)
            quadraticTo(w * 0.50f, h * 0.08f, w * 0.89f, h * 0.46f)
            close()
        }
        drawPath(lidPath, Color(0xFF965D27))
        drawPath(lidPath, AccentGoldBright, style = Stroke(width = stroke))
        // Chest body
        drawRoundRect(
            color = HeroChestBg,
            topLeft = Offset(w * 0.11f, h * 0.36f),
            size = Size(w * 0.78f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        )
        drawRoundRect(
            color = AccentGoldBright,
            topLeft = Offset(w * 0.11f, h * 0.36f),
            size = Size(w * 0.78f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke),
        )
        // Band line
        drawLine(
            color = AccentGoldBright,
            start = Offset(w * 0.11f, h * 0.60f),
            end = Offset(w * 0.89f, h * 0.60f),
            strokeWidth = stroke * 0.75f,
        )
        // Lock clasp
        drawRoundRect(
            color = AccentGoldBright,
            topLeft = Offset(w * 0.43f, h * 0.43f),
            size = Size(w * 0.14f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        )
    }
}

@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ParchmentCard,
    borderColor: Color = CardLine,
    leftAccentColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = CardInk),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (leftAccentColor != null) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(1.dp) // filled below via intrinsic or wrapper
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (leftAccentColor != null) {
                            Modifier.border(
                                width = 0.dp,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                        } else Modifier
                    )
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
fun HeroChestCard(
    totalChests: String,
    title: String,
    subtitle: String,
    manualLine: String?,
    onEditClick: () -> Unit,
    editContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = HeroChestBg, contentColor = HeroChestInk),
        border = BorderStroke(2.dp, AccentGoldBright),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 48.dp)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF3CF7A),
                    letterSpacing = 1.4.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalChests,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 56.sp),
                    color = HeroChestValue,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroChestInk,
                )
                if (manualLine != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = manualLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = HeroChestInk.copy(alpha = 0.9f),
                    )
                }
            }
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .border(1.dp, AccentGoldBright, RoundedCornerShape(8.dp))
                    .testTag("edit_chests_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = editContentDescription,
                    tint = HeroChestValue,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    sub: String? = null,
    valueColor: Color = CardInk,
    badgeText: String? = null,
    infoText: String? = null,
    lang: String = "en",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ParchmentCard, contentColor = CardInk),
        border = BorderStroke(1.dp, CardLine),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
                color = valueColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
                if (badgeText != null) {
                    Surface(
                        color = com.example.ui.theme.WarnBadgeBg,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = com.example.ui.theme.WarnCrimson,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (infoText != null) {
                    InfoDialogButton(text = infoText, lang = lang, tint = CardMuted)
                }
            }
            if (!sub.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            }
        }
    }
}

@Composable
fun InfoDialogButton(
    text: String,
    lang: String,
    tint: Color = TitleGold,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = L.t(lang, "Plus d'informations", "More info", "Más información"),
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = ParchmentCard,
            titleContentColor = HeroChestBg,
            textContentColor = CardInk,
            title = {
                Text(
                    text = L.t(lang, "Information", "Details", "Información"),
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            text = { Text(text = text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text(L.t(lang, "OK", "OK", "OK"), color = HeroChestBg, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
    lang: String,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val isWarning = notice.level == "warning"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) WarnBannerBg else ParchmentCard,
            contentColor = CardInk,
        ),
        border = BorderStroke(1.5.dp, if (isWarning) WarnRedBright else AccentGold),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isWarning) WarnRedBright else AccentGold),
                contentAlignment = Alignment.Center,
            ) {
                if (isWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text("i", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                val title = notice.titleIn(lang)
                if (title.isNotEmpty()) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = CardInk)
                    Spacer(Modifier.height(2.dp))
                }
                Text(notice.textIn(lang), style = MaterialTheme.typography.bodyMedium, color = CardInk)
                val link = notice.link
                if (link != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenLink(link) },
                        border = BorderStroke(1.dp, if (isWarning) WarnRedBright else AccentGold),
                    ) {
                        Text(
                            L.t(lang, "En savoir plus", "More info", "Más información"),
                            color = CardInk,
                        )
                    }
                }
            }
            if (!isWarning) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = L.t(lang, "Fermer", "Close", "Cerrar"),
                        tint = CardMuted,
                    )
                }
            }
        }
    }
}

@Composable
fun VerdictChip(tier: VerdictTier, label: String) {
    val (bg, fg) = when (tier) {
        VerdictTier.VUNLIKELY -> VerdictVUnlikelyBg to VerdictVUnlikelyFg
        VerdictTier.UNLIKELY -> VerdictUnlikelyBg to VerdictUnlikelyFg
        VerdictTier.POSSIBLE -> VerdictPossibleBg to VerdictPossibleFg
        VerdictTier.LIKELY -> VerdictLikelyBg to VerdictLikelyFg
        VerdictTier.VLIKELY -> VerdictVLikelyBg to VerdictVLikelyFg
        VerdictTier.CERTAIN -> VerdictCertainBg to VerdictCertainFg
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun TagPill(text: String, weekly: Boolean) {
    Surface(
        color = if (weekly) WeeklyBlue else PurpleStar,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun GotBadge(lang: String) {
    Surface(
        color = TealAccent,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = "✓ ${L.t(lang, "Obtenu", "Got it", "Conseguido")}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    initiallyOpen: Boolean = false,
    badge: String? = null,
    infoText: String? = null,
    lang: String = "en",
    headerTrailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TitleGold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TitleGold,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Surface(
                    color = com.example.ui.theme.WarnBadgeBg,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = com.example.ui.theme.WarnCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            if (infoText != null) {
                InfoDialogButton(text = infoText, lang = lang, tint = TitleGold)
            }
            if (open && headerTrailing != null) {
                headerTrailing()
            }
        }
        AnimatedVisibility(visible = open) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
fun TreasureThumbnail(
    imageUrl: String?,
    obtained: Boolean = true,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val localRes = remember(imageUrl) { TreasureCatalog.localDrawableFor(imageUrl) }
    val colorFilter = remember(obtained) {
        if (obtained) null
        else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })
    }
    val alpha = if (obtained) 1f else 0.72f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(CardTrack),
        contentAlignment = Alignment.Center,
    ) {
        if (localRes != null) {
            Image(
                painter = painterResource(id = localRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                alpha = alpha,
                modifier = Modifier.matchParentSize(),
            )
        } else if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                alpha = alpha,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
fun LogbookPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HeroChestBg,
            contentColor = HeroChestInk,
        ),
        border = BorderStroke(1.dp, AccentGoldBright),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LogbookSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ParchmentCard,
            contentColor = if (danger) com.example.ui.theme.WarnCrimson else CardInk,
        ),
        border = BorderStroke(1.dp, if (danger) com.example.ui.theme.WarnCrimson else AccentGold),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = TitleGold,
        modifier = modifier,
    )
}

@Composable
fun NightCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = NightMuted,
        modifier = modifier,
    )
}
