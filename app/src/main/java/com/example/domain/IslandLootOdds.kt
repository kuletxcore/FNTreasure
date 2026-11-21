package com.example.domain

import java.lang.Math.log1p
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.expm1
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToLong

data class IslandLootItem(
    val id: String,
    val kind: String,
    val name: String,
    val imageUrl: String?,
    val baseOneIn: Int,
    val weekly: Boolean,
    val weekNumber: Int?,
    val liveOrNextBoost: Double?,
    val isBoostLive: Boolean,
)

data class LootMilestone(
    val targetChance: Double,
    val chestsNeeded: Long,
    val matchesNeeded: Long?,
)

data class LootOddsCalculation(
    val item: IslandLootItem,
    val chests: Long,
    val appliedBoost: Double,
    val effectivePerChestP: Double,
    val effectiveOneIn: Double,
    val cumulativeP: Double,
    val logNever: Double,
    val expectedDrops: Double,
    val verdictTier: VerdictTier,
    val verdictLabel: String,
    val milestones: List<LootMilestone>,
)

object IslandLootOdds {
    val milestoneTargets = listOf(0.25, 0.50, 0.75, 0.90)

    fun catalogItems(
        now: Instant,
        weekNumberOverride: Int? = null,
        lang: String,
    ): List<IslandLootItem> {
        val catalog = TreasureCatalog.data
        val activeWeek = if (weekNumberOverride != null) {
            catalog.weeks.firstOrNull { it.number == weekNumberOverride }
        } else {
            TreasureCatalog.shownWeek(now) ?: catalog.weeks.lastOrNull()
        }
        val curHour = TreasureCatalog.currentTreasureHour(now)
        val nextHour = if (curHour == null) TreasureCatalog.nextTreasureHour(now) else null

        fun boostInfo(kind: String): Pair<Double?, Boolean> {
            val live = curHour?.boosts?.get(kind)?.takeIf { it > 1.0 }
            if (live != null) return live to true
            val upcoming = nextHour?.boosts?.get(kind)?.takeIf { it > 1.0 }
            return upcoming to false
        }

        val result = mutableListOf<IslandLootItem>()

        // Weekly items from selected / current week in catalog order of rarity or odds
        val oddsByKind = catalog.odds.associateBy { it.kind }
        if (activeWeek != null) {
            for (prize in activeWeek.prizes) {
                val odds = oddsByKind[prize.kind] ?: continue
                val (boost, isLive) = boostInfo(prize.kind)
                result.add(
                    IslandLootItem(
                        id = prize.id,
                        kind = prize.kind,
                        name = prize.name,
                        imageUrl = prize.image,
                        baseOneIn = odds.oneIn,
                        weekly = odds.weekly,
                        weekNumber = activeWeek.number,
                        liveOrNextBoost = boost,
                        isBoostLive = isLive,
                    )
                )
            }
        }

        // All-season Pirate Shop Access
        oddsByKind[TreasureCatalog.PIRATE_SHOP]?.let { odds ->
            val (boost, isLive) = boostInfo(TreasureCatalog.PIRATE_SHOP)
            result.add(
                IslandLootItem(
                    id = catalog.pirateShopAccess.id,
                    kind = TreasureCatalog.PIRATE_SHOP,
                    name = TreasureCatalog.kindName(TreasureCatalog.PIRATE_SHOP, lang),
                    imageUrl = catalog.pirateShopAccess.image,
                    baseOneIn = odds.oneIn,
                    weekly = odds.weekly,
                    weekNumber = null,
                    liveOrNextBoost = boost,
                    isBoostLive = isLive,
                )
            )
        }

        // All-season Gizmo (any)
        oddsByKind[TreasureCatalog.GIZMO]?.let { odds ->
            val (boost, isLive) = boostInfo(TreasureCatalog.GIZMO)
            result.add(
                IslandLootItem(
                    id = "gizmo-any",
                    kind = TreasureCatalog.GIZMO,
                    name = TreasureCatalog.kindName(TreasureCatalog.GIZMO, lang),
                    imageUrl = null,
                    baseOneIn = odds.oneIn,
                    weekly = odds.weekly,
                    weekNumber = null,
                    liveOrNextBoost = boost,
                    isBoostLive = isLive,
                )
            )
        }

        // Sort rarest -> most common or most common -> rarest (odds.json order: rarest first, matching Report)
        val kindOrder = catalog.odds.mapIndexed { index, o -> o.kind to index }.toMap()
        return result.sortedBy { kindOrder[it.kind] ?: Int.MAX_VALUE }
    }

    fun calculate(
        item: IslandLootItem,
        chests: Long,
        applyBoost: Boolean,
        avgChestsPerMatch: Double? = null,
        lang: String,
    ): LootOddsCalculation {
        val safeChests = chests.coerceAtLeast(0L)
        val boost = if (applyBoost && item.liveOrNextBoost != null && item.liveOrNextBoost > 1.0) {
            item.liveOrNextBoost
        } else {
            1.0
        }
        val baseP = 1.0 / item.baseOneIn.coerceAtLeast(1)
        val effectiveP = min(0.999999, baseP * boost)
        val effectiveOneIn = 1.0 / effectiveP

        val logNever = if (safeChests == 0L) 0.0 else safeChests.toDouble() * log1p(-effectiveP)
        val cumulativeP = if (safeChests == 0L) 0.0 else -expm1(logNever)
        val expected = safeChests.toDouble() * effectiveP
        val (tier, verdictLabel) = TreasureCatalog.verdict(cumulativeP, lang)

        val validAvg = avgChestsPerMatch?.takeIf { it > 0.0 }
        val milestones = milestoneTargets.map { target ->
            val needed = chestsForChance(effectiveP, target)
            val matches = validAvg?.let { ceil(needed / it).toLong().coerceAtLeast(1L) }
            LootMilestone(target, needed, matches)
        }

        return LootOddsCalculation(
            item = item,
            chests = safeChests,
            appliedBoost = boost,
            effectivePerChestP = effectiveP,
            effectiveOneIn = effectiveOneIn,
            cumulativeP = cumulativeP,
            logNever = logNever,
            expectedDrops = expected,
            verdictTier = tier,
            verdictLabel = verdictLabel,
            milestones = milestones,
        )
    }

    fun calculateCustom(
        perChestPercent: Double,
        chests: Long,
        avgChestsPerMatch: Double? = null,
        lang: String,
    ): LootOddsCalculation {
        val safeChests = chests.coerceAtLeast(0L)
        val clampedP = (perChestPercent / 100.0).coerceIn(1e-9, 0.999999)
        val oneIn = (1.0 / clampedP).roundToLong().coerceIn(1L, 1_000_000_000L).toInt()
        val customItem = IslandLootItem(
            id = "custom",
            kind = "custom",
            name = L.t(lang, "Objet personnalisé", "Custom loot item", "Objeto personalizado"),
            imageUrl = null,
            baseOneIn = oneIn,
            weekly = false,
            weekNumber = null,
            liveOrNextBoost = null,
            isBoostLive = false,
        )
        val logNever = if (safeChests == 0L) 0.0 else safeChests.toDouble() * log1p(-clampedP)
        val cumulativeP = if (safeChests == 0L) 0.0 else -expm1(logNever)
        val expected = safeChests.toDouble() * clampedP
        val (tier, verdictLabel) = TreasureCatalog.verdict(cumulativeP, lang)

        val validAvg = avgChestsPerMatch?.takeIf { it > 0.0 }
        val milestones = milestoneTargets.map { target ->
            val needed = chestsForChance(clampedP, target)
            val matches = validAvg?.let { ceil(needed / it).toLong().coerceAtLeast(1L) }
            LootMilestone(target, needed, matches)
        }

        return LootOddsCalculation(
            item = customItem,
            chests = safeChests,
            appliedBoost = 1.0,
            effectivePerChestP = clampedP,
            effectiveOneIn = 1.0 / clampedP,
            cumulativeP = cumulativeP,
            logNever = logNever,
            expectedDrops = expected,
            verdictTier = tier,
            verdictLabel = verdictLabel,
            milestones = milestones,
        )
    }

    fun chestsForChance(perChestP: Double, targetChance: Double): Long {
        if (perChestP >= 1.0) return 1L
        if (perChestP <= 0.0) return Long.MAX_VALUE
        if (perChestP >= targetChance) return 1L
        return ceil(ln(1.0 - targetChance) / log1p(-perChestP)).toLong().coerceAtLeast(1L)
    }
}
