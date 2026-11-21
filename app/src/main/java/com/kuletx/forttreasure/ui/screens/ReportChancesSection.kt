package com.kuletx.forttreasure.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuletx.forttreasure.domain.ChanceResult
import com.kuletx.forttreasure.domain.L
import com.kuletx.forttreasure.domain.MatchRecordDto
import com.kuletx.forttreasure.domain.TreasureCatalog
import com.kuletx.forttreasure.domain.TreasureOdds
import com.kuletx.forttreasure.domain.TreasurePrize
import com.kuletx.forttreasure.domain.VerdictTier
import com.kuletx.forttreasure.ui.MainUiState
import com.kuletx.forttreasure.ui.components.CollapsibleSection
import com.kuletx.forttreasure.ui.components.GotBadge
import com.kuletx.forttreasure.ui.components.ParchmentCard
import com.kuletx.forttreasure.ui.components.TagPill
import com.kuletx.forttreasure.ui.components.TreasureThumbnail
import com.kuletx.forttreasure.ui.components.VerdictChip
import com.kuletx.forttreasure.ui.theme.AccentGold
import com.kuletx.forttreasure.ui.theme.AccentGoldText
import com.kuletx.forttreasure.ui.theme.CardInk
import com.kuletx.forttreasure.ui.theme.CardLine
import com.kuletx.forttreasure.ui.theme.CardMuted
import com.kuletx.forttreasure.ui.theme.CardTrack
import com.kuletx.forttreasure.ui.theme.HeroChestBg
import com.kuletx.forttreasure.ui.theme.LiveBoostChipBg
import com.kuletx.forttreasure.ui.theme.LiveBoostOrange
import com.kuletx.forttreasure.ui.theme.LiveBoostText
import com.kuletx.forttreasure.ui.theme.NightMuted
import com.kuletx.forttreasure.ui.theme.ParchmentCard
import com.kuletx.forttreasure.ui.theme.ParchmentInkBright
import com.kuletx.forttreasure.ui.theme.TealAccent
import com.kuletx.forttreasure.ui.theme.TitleGold
import com.kuletx.forttreasure.ui.theme.WarnBannerBg
import com.kuletx.forttreasure.ui.theme.WarnRedBright
import java.lang.Math.log1p
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.expm1
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
fun ReportChancesSection(
    state: MainUiState,
    obtainedSet: Set<String>,
    onOpenUrl: (String) -> Unit,
) {
    val lang = state.config.language
    val now = state.nowUtc
    val catalog = TreasureCatalog.data
    if (catalog.weeks.isEmpty() || !now.isBefore(TreasureCatalog.huntEnd.plusSeconds(14 * 86400L))) {
        return
    }

    val view = state.view
    val matches = view.matches
    val chestTimes = matches.flatMap { m ->
        val start = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH
        m.chests.map { start.plusMillis((it.timeSeconds * 1000).toLong()) }
    }
    val huntStart = TreasureCatalog.huntStart
    val huntEnd = TreasureCatalog.huntEnd
    val huntTimes = chestTimes.filter { !it.isBefore(huntStart) }.sorted()

    val manual = view.totalChests - chestTimes.size
    val seasonChests = maxOf(0, huntTimes.size + manual)
    val week = TreasureCatalog.shownWeek(now)
    val weekTimes = if (week == null) emptyList() else huntTimes.filter { !it.isBefore(week.from) && it.isBefore(week.to) }
    val weekManual = if (week == null) 0 else maxOf(
        0,
        view.manualEntries.filter { L.parseUtc(it.weekFromUtc) == week.from }.sumOf { it.chests }
    )
    val weekChests = weekTimes.size + weekManual
    val boostedCount = huntTimes.count { TreasureCatalog.boost(TreasureCatalog.GOLD_OUTFIT, it) > 1.0 }

    fun seasonChance(odds: TreasureOdds): ChanceResult =
        if (manual >= 0) ChanceResult.of(odds, huntTimes, manual.toLong())
        else ChanceResult.of(odds, emptyList(), seasonChests.toLong())

    fun weekChance(odds: TreasureOdds): ChanceResult =
        ChanceResult.of(odds, weekTimes, weekManual.toLong())

    fun shownChance(odds: TreasureOdds): ChanceResult =
        if (odds.weekly) weekChance(odds) else seasonChance(odds)

    fun perDay(count: Int, from: Instant, to: Instant): Double {
        val days = Duration.between(from, to).toMillis() / 86_400_000.0
        return if (days >= 1.0) count / days else 0.0
    }

    val seasonPace = if (huntTimes.isNotEmpty()) perDay(huntTimes.size, huntTimes.first(), now) else 0.0
    var weekPace = if (week != null) perDay(weekChests, week.from, now) else 0.0
    if (weekPace <= 0.0) weekPace = seasonPace

    fun projectionText(odds: TreasureOdds, c: ChanceResult, weekly: Boolean): String? {
        val end = if (weekly) week?.to ?: now else huntEnd
        val pace = if (weekly) weekPace else seasonPace
        if (pace <= 0.0 || !now.isBefore(end) || c.p >= 0.75) return null
        val remDays = Duration.between(now, end).toMillis() / 86_400_000.0
        val log = c.logNever + pace * remDays * log1p(-1.0 / odds.oneIn)
        val pText = TreasureCatalog.percentText(-expm1(log), lang)
        return if (weekly) {
            L.t(lang, "≈ $pText d'ici la fin de la semaine à ton rythme", "≈ $pText by the end of the week at your pace", "≈ $pText al final de la semana a tu ritmo")
        } else {
            L.t(lang, "≈ $pText d'ici la fin de la chasse à ton rythme", "≈ $pText by the end of the hunt at your pace", "≈ $pText al final de la búsqueda a tu ritmo")
        }
    }

    val gizmoTimes = matches.flatMap { m ->
        val start = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH
        m.treasureFinds.filter { !it.fromGoldenChest }
            .map { start.plusMillis(((it.obtainedAtSeconds ?: 0f) * 1000).toLong()) }
    }.filter { !it.isBefore(huntStart) }.sorted()

    val curHour = TreasureCatalog.currentTreasureHour(now)
    val nextHour = if (curHour == null) TreasureCatalog.nextTreasureHour(now) else null

    val info = L.t(
        lang,
        "La chance que chaque trésor soit déjà tombé pour toi au moins une fois. Détails : « Comment c'est calculé ? » en bas.",
        "The chance each treasure has already dropped for you at least once. Details: \"How is this calculated?\" at the bottom.",
        "La probabilidad de que cada tesoro ya te haya tocado al menos una vez. Detalles: «¿Cómo se calcula?» abajo.",
    )

    CollapsibleSection(
        title = L.t(lang, "Tes chances", "Your chances", "Tus probabilidades"),
        initiallyOpen = true,
        infoText = info,
        lang = lang,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Treasure Hour Banner
            if (curHour != null) {
                val endClock = L.clock(curHour.endUtc, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                val (cdText, _) = TreasureCatalog.countdownText(curHour.endUtc, now, lang)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = WarnBannerBg, contentColor = CardInk),
                    border = BorderStroke(1.5.dp, WarnRedBright),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "⚡ " + L.t(
                                lang,
                                "Heure du trésor en cours, jusqu'à $endClock · se termine dans $cdText",
                                "Treasure Hour live now, until $endClock · ends in $cdText",
                                "Hora del tesoro en curso, hasta las $endClock · termina en $cdText",
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = CardInk,
                        )
                        curHour.boosts.entries.forEach { (kind, boost) ->
                            val odds = catalog.odds.firstOrNull { it.kind == kind }
                            val per = odds?.let { L.n(lang, (it.oneIn / boost).roundToInt()) }
                            Text(
                                text = "${TreasureCatalog.kindName(kind, lang)} ×${L.f(lang, boost, if (boost % 1.0 == 0.0) 0 else 1)}" +
                                    (per?.let { " · " + L.t(lang, "1 sur $it par coffre", "1 in $it per chest", "1 entre $it por cofre") } ?: ""),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LiveBoostText,
                            )
                        }
                    }
                }
            } else if (nextHour != null) {
                val startTxt = L.dayTime(nextHour.startUtc, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                val (cdText, _) = TreasureCatalog.countdownText(nextHour.startUtc, now, lang)
                val expTxt = if (!nextHour.confirmed) " (${L.t(lang, "prévue, pas encore annoncée", "expected, not announced yet", "prevista, aún no anunciada")})" else ""
                ParchmentCard(borderColor = AccentGold) {
                    Text(
                        text = L.t(
                            lang,
                            "Prochaine heure du trésor : $startTxt · dans $cdText",
                            "Next Treasure Hour: $startTxt · in $cdText",
                            "Próxima hora del tesoro: $startTxt · en $cdText",
                        ) + expTxt,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = nextHour.boosts.entries.joinToString("   ") { (k, b) ->
                            "${TreasureCatalog.kindName(k, lang)} ×${L.f(lang, b, if (b % 1.0 == 0.0) 0 else 1)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                }
            }

            Text(
                text = L.t(
                    lang,
                    "Chaque coffre est un nouveau tirage : tes chances ne s'accumulent pas. Ces chiffres montrent la chance qu'un trésor soit déjà tombé pour toi au moins une fois, avec les coffres que tu as ouverts.",
                    "Every chest is a fresh roll: your odds don't build up. These numbers show the chance a treasure has already dropped for you at least once, from the chests you've opened.",
                    "Cada cofre es una tirada nueva: tus probabilidades no se acumulan. Estas cifras muestran la probabilidad de que un tesoro ya te haya tocado al menos una vez, con los cofres que has abierto.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = NightMuted,
            )

            // Featured Hero Cards: Gold Outfit + Pirate Shop Access
            listOf(TreasureCatalog.GOLD_OUTFIT, TreasureCatalog.PIRATE_SHOP).forEach { kind ->
                val odds = catalog.odds.first { it.kind == kind }
                val weekly = odds.weekly
                val c = shownChance(odds)
                val prize: TreasurePrize? = if (weekly) week?.prizes?.firstOrNull { it.kind == kind } else catalog.pirateShopAccess
                val got = prize != null && prize.id in obtainedSet
                val activeBoost = curHour?.boosts?.get(kind)?.takeIf { it > 1.0 }
                val borderCol = when {
                    got -> TealAccent
                    activeBoost != null -> LiveBoostOrange
                    else -> CardLine
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = ParchmentCard, contentColor = CardInk),
                    border = BorderStroke(if (got || activeBoost != null) 2.dp else 1.dp, borderCol),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TreasureThumbnail(imageUrl = prize?.image, obtained = true, size = 46.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = TreasureCatalog.kindName(kind, lang).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CardMuted,
                                    )
                                    if (activeBoost != null) {
                                        Surface(color = LiveBoostChipBg, shape = RoundedCornerShape(999.dp)) {
                                            Text(
                                                text = "⚡ ×${L.f(lang, activeBoost, if (activeBoost % 1.0 == 0.0) 0 else 1)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LiveBoostText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (weekly) (prize?.name ?: TreasureCatalog.kindName(kind, lang))
                                    else L.t(lang, "Toute la saison", "Lasts all season", "Toda la temporada"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CardInk,
                                )
                            }
                            TagPill(
                                text = if (weekly) L.t(lang, "Hebdo", "Weekly", "Semanal") else L.t(lang, "Saison", "Season", "Temporada"),
                                weekly = weekly,
                            )
                            if (got) GotBadge(lang)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = TreasureCatalog.percentText(c.p, lang),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                                color = CardInk,
                            )
                            val (tier, vLabel) = TreasureCatalog.verdict(c.p, lang)
                            VerdictChip(tier, vLabel)
                        }

                        val about = if (c.p > 0.0 && c.p < 0.5) {
                            val inv = (1.0 / c.p).roundToInt()
                            " · " + L.t(lang, "environ 1 sur ${L.n(lang, inv)}", "about 1 in ${L.n(lang, inv)}", "aproximadamente 1 entre ${L.n(lang, inv)}")
                        } else ""
                        Text(
                            text = (if (weekly) L.t(lang, "chance qu'il soit tombé pour toi cette semaine", "chance it dropped for you this week", "probabilidad de que te haya tocado esta semana")
                            else L.t(lang, "chance qu'il soit tombé pour toi cette saison", "chance it dropped for you this season", "probabilidad de que te haya tocado esta temporada")) + about,
                            style = MaterialTheme.typography.bodySmall,
                            color = CardInk,
                        )

                        if (activeBoost != null) {
                            val boostedOne = L.n(lang, (odds.oneIn / activeBoost).roundToInt())
                            val extraMsg = if (c.p < 0.5) {
                                val more = ceil(ln(0.5) / log1p(-1.0 / (odds.oneIn / activeBoost))).toInt()
                                L.t(lang, " · une chance sur deux dans ${L.n(lang, more)} coffres de plus à ce rythme", " · one chance in two within ${L.n(lang, more)} more chests at this rate", " · una de cada dos en ${L.n(lang, more)} cofres más a este ritmo")
                            } else ""
                            Text(
                                text = L.t(lang, "1 sur $boostedOne à chaque coffre pendant l'heure du trésor", "1 in $boostedOne per chest during the Treasure Hour", "1 entre $boostedOne por cofre durante la hora del tesoro") + extraMsg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = LiveBoostText,
                            )
                        } else {
                            Text(
                                text = L.t(
                                    lang,
                                    "Chaque coffre : 1 sur ${L.n(lang, odds.oneIn)} · ${L.n(lang, c.chests)} coffres ${if (weekly) "cette semaine" else "cette saison"}",
                                    "Each chest: 1 in ${L.n(lang, odds.oneIn)} · ${L.n(lang, c.chests)} chests ${if (weekly) "this week" else "this season"}",
                                    "Cada cofre: 1 entre ${L.n(lang, odds.oneIn)} · ${L.n(lang, c.chests)} cofres ${if (weekly) "esta semana" else "esta temporada"}",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = CardMuted,
                            )
                        }

                        if (weekly) {
                            val s = seasonChance(odds)
                            HorizontalDivider(color = CardLine)
                            Text(
                                text = L.t(
                                    lang,
                                    "Toutes les semaines réunies : ${TreasureCatalog.percentText(s.p, lang)} sur ${L.n(lang, s.chests)} coffres",
                                    "All weeks combined: ${TreasureCatalog.percentText(s.p, lang)} over ${L.n(lang, s.chests)} chests",
                                    "Todas las semanas juntas: ${TreasureCatalog.percentText(s.p, lang)} en ${L.n(lang, s.chests)} cofres",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = CardMuted,
                            )
                            if (week != null && now.isBefore(week.to)) {
                                val (cd, _) = TreasureCatalog.countdownText(week.to, now, lang)
                                Text(
                                    text = "${L.t(lang, "Cette tenue part dans", "This outfit leaves in", "Este traje se va en")} $cd",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = CardInk,
                                )
                            }
                        } else {
                            val half = TreasureCatalog.chestsFor(odds.oneIn, 0.5)
                            val frac = (c.chests.toFloat() / half.toFloat()).coerceIn(0.01f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        L.t(lang, "Vers une chance sur deux", "Halfway to a 50/50 chance", "Hacia una probabilidad del 50 %"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CardMuted,
                                    )
                                    Text(
                                        "${L.n(lang, c.chests)} / ${L.nLong(lang, half)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = CardMuted,
                                    )
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardTrack)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AccentGoldText)
                                    )
                                }
                            }
                        }

                        projectionText(odds, c, weekly)?.let { proj ->
                            HorizontalDivider(color = CardLine)
                            Text(proj, style = MaterialTheme.typography.bodySmall, color = CardMuted)
                        }
                    }
                }
            }

            // Weekly Odds Table Card
            if (week != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagPill(L.t(lang, "Hebdo", "Weekly", "Semanal"), weekly = true)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${L.t(lang, "Semaine", "Week", "Semana")} ${week.number} · ${week.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ParchmentInkBright,
                        )
                        val manualWeekTxt = if (weekManual > 0) L.t(lang, ", dont ${L.n(lang, weekManual)} ajoutés à la main", ", including ${L.n(lang, weekManual)} added by hand", ", incluidos ${L.n(lang, weekManual)} añadidos a mano") else ""
                        Text(
                            text = "${L.count(lang, weekChests, "coffre", "coffres", "chest", "chests", "cofre", "cofres")} ${L.t(lang, "cette semaine", "this week", "esta semana")}$manualWeekTxt",
                            style = MaterialTheme.typography.bodySmall,
                            color = NightMuted,
                        )
                    }
                    if (now.isBefore(week.to)) {
                        val (cd, soon) = TreasureCatalog.countdownText(week.to, now, lang)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                L.t(lang, "PART DANS", "LEAVES IN", "SE VA EN"),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NightMuted,
                            )
                            Text(
                                text = cd,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (soon) Color(0xFFF0A04B) else ParchmentInkBright,
                            )
                        }
                    }
                }

                ParchmentCard {
                    val weeklyOdds = catalog.odds.filter { it.weekly && it.kind != TreasureCatalog.GOLD_OUTFIT }
                    weeklyOdds.forEachIndexed { idx, odds ->
                        if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                        val prize = week.prizes.firstOrNull { it.kind == odds.kind }
                        val c = shownChance(odds)
                        val got = prize != null && prize.id in obtainedSet
                        OddsRowItem(
                            odds = odds,
                            c = c,
                            prize = prize,
                            got = got,
                            boost = curHour?.boosts?.get(odds.kind)?.takeIf { it > 1.0 },
                            note = projectionText(odds, c, weekly = true),
                            lang = lang,
                        )
                    }
                }
            }

            // All-Season Odds Table Card
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagPill(L.t(lang, "Saison", "Season", "Temporada"), weekly = false)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L.t(lang, "Toute la saison", "All season", "Toda la temporada"),
                        style = MaterialTheme.typography.titleMedium,
                        color = ParchmentInkBright,
                    )
                    val dateStart = L.dateOnly(huntStart, lang, state.config.timeZone)
                    val byHand = if (manual > 0) L.t(lang, ", dont ${L.n(lang, manual)} ajoutés à la main", ", including ${L.n(lang, manual)} added by hand", ", incluidos ${L.n(lang, manual)} añadidos a mano") else ""
                    Text(
                        text = "${L.count(lang, seasonChests, "coffre", "coffres", "chest", "chests", "cofre", "cofres")} ${L.t(lang, "depuis le début de la chasse", "since the hunt started", "desde el inicio de la búsqueda")} ($dateStart)$byHand",
                        style = MaterialTheme.typography.bodySmall,
                        color = NightMuted,
                    )
                }
            }

            ParchmentCard {
                val seasonOdds = catalog.odds.filter { !it.weekly && it.kind != TreasureCatalog.PIRATE_SHOP }
                seasonOdds.forEachIndexed { idx, odds ->
                    if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 8.dp))
                    val c = shownChance(odds)
                    val note = if (odds.kind == TreasureCatalog.GIZMO && gizmoTimes.isNotEmpty()) {
                        val since = huntTimes.count { it.isAfter(gizmoTimes.last()) }
                        L.t(
                            lang,
                            "Tu en as trouvé ${L.n(lang, gizmoTimes.size)} · le dernier il y a ${L.count(lang, since, "coffre", "coffres", "chest", "chests", "cofre", "cofres")}",
                            "You found ${L.n(lang, gizmoTimes.size)} · last one ${L.count(lang, since, "coffre", "coffres", "chest", "chests", "cofre", "cofres")} ago",
                            "Has encontrado ${L.n(lang, gizmoTimes.size)} · el último hace ${L.count(lang, since, "coffre", "coffres", "chest", "chests", "cofre", "cofres")}",
                        )
                    } else null
                    OddsRowItem(
                        odds = odds,
                        c = c,
                        prize = null,
                        got = false,
                        boost = curHour?.boosts?.get(odds.kind)?.takeIf { it > 1.0 },
                        note = note,
                        lang = lang,
                    )
                }
            }

            // Folded: Weekly treasures all weeks combined
            CollapsibleSection(
                title = L.t(lang, "Trésors hebdo, toutes les semaines réunies", "Weekly treasures, all weeks combined", "Tesoros semanales, todas las semanas juntas"),
                initiallyOpen = false,
                lang = lang,
            ) {
                ParchmentCard {
                    Text(
                        text = L.t(
                            lang,
                            "La chance d'en avoir eu au moins un, de n'importe quelle semaine, sur les ${L.n(lang, seasonChests)} coffres de la chasse.",
                            "The chance you've had at least one of them from any week, over all ${L.n(lang, seasonChests)} chests of the hunt.",
                            "La probabilidad de haber conseguido al menos uno, de cualquier semana, en los ${L.n(lang, seasonChests)} cofres de la búsqueda.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    catalog.odds.filter { it.weekly }.forEachIndexed { idx, odds ->
                        if (idx > 0) HorizontalDivider(color = CardLine, modifier = Modifier.padding(vertical = 6.dp))
                        val s = seasonChance(odds)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(TreasureCatalog.kindName(odds.kind, lang), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(TreasureCatalog.percentText(s.p, lang), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
                            Text(TreasureCatalog.expectedText(s.average, lang), style = MaterialTheme.typography.bodySmall, color = CardMuted)
                        }
                    }
                }
            }

            // Folded: How is this calculated?
            CollapsibleSection(
                title = L.t(lang, "Comment c'est calculé ?", "How is this calculated?", "¿Cómo se calcula?"),
                initiallyOpen = false,
                lang = lang,
            ) {
                val hours = TreasureCatalog.nextRegularHours(now)
                val hoursFmt = hours.joinToString(L.t(lang, " et ", " and ", " y ")) {
                    L.weekdayTime(it, lang, state.config.timeZone, state.config.clockFormat, state.isSystem24Hour)
                }
                val loadingOneIn = catalog.odds.firstOrNull { it.kind == TreasureCatalog.LOADING_SCREEN }?.oneIn ?: 272
                ParchmentCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = L.t(
                                lang,
                                "• Chaque coffre est indépendant. Un écran de chargement, c'est 1 sur ${L.n(lang, loadingOneIn)} à chaque coffre.\n" +
                                    "• Chance de l'avoir eu = 1 − (1 − 1/X)^coffres.\n" +
                                    "• Hebdo : ces trésors changent chaque dimanche, seuls les coffres de la semaine comptent.\n" +
                                    "• Saison : boutique pirate et gizmos comptent tous les coffres de la chasse, y compris manuels.\n" +
                                    "• Heures du trésor : certaines chances sont multipliées ($hoursFmt pendant 2 h). ${L.n(lang, boostedCount)} de tes coffres ouverts pendant une heure boostée.",
                                "• Each chest is independent. A loading screen is 1 in ${L.n(lang, loadingOneIn)} on every chest.\n" +
                                    "• Chance you've had it = 1 − (1 − 1/X)^chests.\n" +
                                    "• Weekly treasures change every Sunday, so only this week's chests count.\n" +
                                    "• Season treasures count every chest since the hunt started, including manual ones.\n" +
                                    "• Treasure Hours boost some odds ($hoursFmt for 2 hours). ${L.n(lang, boostedCount)} of your chests count with boosted odds.",
                                "• Cada cofre es independiente. Una pantalla de carga es 1 entre ${L.n(lang, loadingOneIn)} en cada cofre.\n" +
                                    "• Probabilidad = 1 − (1 − 1/X)^cofres.\n" +
                                    "• Semanal: estos tesoros cambian cada domingo, solo cuentan los cofres de la semana.\n" +
                                    "• Temporada: cuentan todos los cofres desde el inicio de la búsqueda.\n" +
                                    "• Horas del tesoro ($hoursFmt, 2 horas). ${L.n(lang, boostedCount)} de tus cofres cuentan con probabilidad aumentada.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = CardInk,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenUrl(catalog.oddsSource) }) {
                                Text("Epic Games (Fortnite Status)", color = HeroChestBg, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { onOpenUrl(catalog.hoursSource) }) {
                                Text("fortnite.bot", color = HeroChestBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OddsRowItem(
    odds: TreasureOdds,
    c: ChanceResult,
    prize: TreasurePrize?,
    got: Boolean,
    boost: Double?,
    note: String?,
    lang: String,
) {
    val (tier, vLabel) = TreasureCatalog.verdict(c.p, lang)
    val barColor = when (tier) {
        VerdictTier.VUNLIKELY, VerdictTier.UNLIKELY -> AccentGold
        VerdictTier.POSSIBLE -> Color(0xFFD68A1C)
        VerdictTier.LIKELY, VerdictTier.VLIKELY -> Color(0xFF5F9C4A)
        VerdictTier.CERTAIN -> Color(0xFF2F7D4F)
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (prize != null) {
                TreasureThumbnail(imageUrl = prize.image, obtained = true, size = 38.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = TreasureCatalog.kindName(odds.kind, lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (got) TealAccent else CardInk,
                    )
                    if (boost != null) {
                        Surface(color = LiveBoostChipBg, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                text = "⚡ ×${L.f(lang, boost, if (boost % 1.0 == 0.0) 0 else 1)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiveBoostText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    if (got) GotBadge(lang)
                }
                if (prize != null) {
                    Text(prize.name, style = MaterialTheme.typography.bodySmall, color = CardMuted)
                }
                val oneInText = if (boost != null) {
                    L.t(lang, "1 sur ${L.n(lang, (odds.oneIn / boost).roundToInt())} par coffre", "1 in ${L.n(lang, (odds.oneIn / boost).roundToInt())} each chest", "1 entre ${L.n(lang, (odds.oneIn / boost).roundToInt())} por cofre")
                } else {
                    L.t(lang, "1 sur ${L.n(lang, odds.oneIn)} par coffre", "1 in ${L.n(lang, odds.oneIn)} each chest", "1 entre ${L.n(lang, odds.oneIn)} por cofre")
                }
                Text(oneInText, style = MaterialTheme.typography.bodySmall, color = if (boost != null) LiveBoostText else CardMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TreasureCatalog.percentText(c.p, lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${L.t(lang, "Attendu", "Exp.", "Esp.")}: ${TreasureCatalog.expectedText(c.average, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VerdictChip(tier, vLabel)
            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardTrack)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(c.p.toFloat().coerceIn(0.01f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }
        if (!note.isNullOrBlank()) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = CardMuted)
        }
    }
}
