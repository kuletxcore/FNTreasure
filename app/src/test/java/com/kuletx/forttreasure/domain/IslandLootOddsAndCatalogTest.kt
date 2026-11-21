package com.kuletx.forttreasure.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IslandLootOddsAndCatalogTest {

    private val sampleCatalogJson = """
        {
          "schema": 1,
          "revision": 3,
          "updated": "2026-11-15",
          "odds": [
            { "kind": "outfit", "oneIn": 1562500, "weekly": true },
            { "kind": "pirate-shop", "oneIn": 137500, "weekly": false },
            { "kind": "car", "oneIn": 7813, "weekly": true },
            { "kind": "pickaxe", "oneIn": 568, "weekly": true },
            { "kind": "jam", "oneIn": 391, "weekly": true },
            { "kind": "loading", "oneIn": 272, "weekly": true },
            { "kind": "gizmo", "oneIn": 127, "weekly": false }
          ],
          "pirateShop": {
            "id": "pirate-shop",
            "kind": "pirate-shop",
            "name": "Pirate Shop Access",
            "image": "https://cdn.fortnite.bot/s43treasure/token.webp",
            "prizeIds": ["9f73f94d-2e61-4523-97d7-d61235d3365e"]
          },
          "weeks": [
            {
              "number": 1,
              "start": "2026-11-01T14:00:00Z",
              "end": "2026-11-08T14:00:00Z",
              "title": "Week 1",
              "prizes": [
                {
                  "id": "w1-jam",
                  "kind": "jam",
                  "name": "No Trespassing",
                  "image": "https://cdn.fortnite.bot/s43treasure/w1-jam.webp",
                  "prizeIds": ["DelilahPrize:dpid_item3"]
                }
              ]
            }
          ],
          "treasureHours": [],
          "notices": []
        }
    """.trimIndent()

    @Test
    fun `parse validates schema and rejects malformed catalogs`() {
        val parsed = TreasureCatalog.parse(sampleCatalogJson)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.revision)
        assertEquals(7, parsed.odds.size)
        assertEquals(1, parsed.weeks.size)

        assertNull(TreasureCatalog.parse(sampleCatalogJson.replace("\"schema\": 1", "\"schema\": 2")))
        assertNull(TreasureCatalog.parse("{not json"))
    }

    @Test
    fun `calculate computes cumulative odds boost and milestones accurately`() {
        val jamItem = IslandLootItem(
            id = "w1-jam",
            kind = TreasureCatalog.JAM_TRACK,
            name = "No Trespassing",
            imageUrl = null,
            baseOneIn = 391,
            weekly = true,
            weekNumber = 1,
            liveOrNextBoost = 2.0,
            isBoostLive = true,
        )

        val zero = IslandLootOdds.calculate(jamItem, chests = 0L, applyBoost = false, lang = "en")
        assertEquals(0.0, zero.cumulativeP, 1e-12)
        assertEquals(0.0, zero.expectedDrops, 1e-12)
        assertEquals(VerdictTier.VUNLIKELY, zero.verdictTier)

        val boosted = IslandLootOdds.calculate(
            item = jamItem,
            chests = 200L,
            applyBoost = true,
            avgChestsPerMatch = 10.0,
            lang = "en",
        )
        assertEquals(2.0, boosted.appliedBoost, 1e-9)
        assertEquals(2.0 / 391.0, boosted.effectivePerChestP, 1e-12)
        assertTrue(boosted.cumulativeP in 0.63..0.65)
        assertEquals(4, boosted.milestones.size)
        // At 10 chests per match, matchesNeeded == ceil(chestsNeeded / 10)
        boosted.milestones.forEach { m ->
            assertNotNull(m.matchesNeeded)
            assertTrue(m.chestsNeeded >= 1L)
        }
    }

    @Test
    fun `calculateCustom handles custom percentage drop rate`() {
        val res = IslandLootOdds.calculateCustom(
            perChestPercent = 10.0,
            chests = 10L,
            avgChestsPerMatch = 5.0,
            lang = "en",
        )
        // 1 - 0.9^10 ≈ 0.65132
        assertEquals(0.10, res.effectivePerChestP, 1e-9)
        assertEquals(0.6513215599, res.cumulativeP, 1e-6)
        assertEquals(1.0, res.expectedDrops, 1e-9)
        assertEquals(50.0 / 100.0, res.milestones[1].targetChance, 1e-9)
        // For 50% at 10% per chest: ceil(ln(0.5)/ln(0.9)) = 7 chests -> ceil(7/5) = 2 matches
        assertEquals(7L, res.milestones[1].chestsNeeded)
        assertEquals(2L, res.milestones[1].matchesNeeded)
    }

    @Test
    fun `percentText never displays 100 percent`() {
        val textEn = TreasureCatalog.percentText(0.99999, "en")
        assertFalse(textEn.contains("100"))
        assertEquals(">99.9%", textEn)

        val textFr = TreasureCatalog.percentText(0.99999, "fr")
        assertFalse(textFr.contains("100"))
        assertTrue(textFr.startsWith("> 99"))
    }

    @Test
    fun `localDrawableFor resolves token and weekly CDN asset names`() {
        assertNotNull(TreasureCatalog.localDrawableFor("https://cdn.fortnite.bot/s43treasure/token.webp"))
        assertNotNull(TreasureCatalog.localDrawableFor("https://cdn.fortnite.bot/s43treasure/w3-jamtrack.webp?2"))
        assertNull(TreasureCatalog.localDrawableFor("https://cdn.fortnite.bot/s43treasure/unknown.webp"))
        assertNull(TreasureCatalog.localDrawableFor(null))
    }

    @Test
    fun `countdownText returns now when target already passed`() {
        val now = Instant.parse("2026-11-21T12:00:00Z")
        val past = now.minusSeconds(10)
        val (text, soon) = TreasureCatalog.countdownText(past, now, "en")
        assertEquals("now", text)
        assertFalse(soon)

        val inTwoHours = now.plusSeconds(7230)
        val (futureText, isSoon) = TreasureCatalog.countdownText(inTwoHours, now, "en")
        assertEquals("2h 0m 30s", futureText)
        assertTrue(isSoon)
    }
}
