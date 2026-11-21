package com.kuletx.forttreasure.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModesAndReportTest {

    @Test
    fun `GameModes counts BR playlists and experience_br while excluding customs and bad maps`() {
        val expBr = MatchRecordDto(
            sessionId = "1",
            playlist = "experience_br",
            map = "/Delilah/Maps/Delilah_Terrain",
        )
        assertTrue(GameModes.counts(expBr))
        assertNull(GameModes.whyNot(expBr, "en"))

        val defaultSolo = expBr.copy(playlist = "Playlist_DefaultSolo", map = null)
        assertTrue(GameModes.counts(defaultSolo))

        val zeroBuild = expBr.copy(playlist = "Playlist_NoBuildBR_Squads")
        assertTrue(GameModes.counts(zeroBuild))

        val custom = expBr.copy(isCustomMatch = true)
        assertFalse(GameModes.counts(custom))
        assertTrue(GameModes.whyNot(custom, "en")!!.contains("custom"))

        val wrongMap = expBr.copy(map = "/Game/Athena/Maps/Creative")
        assertFalse(GameModes.counts(wrongMap))
        assertTrue(GameModes.whyNot(wrongMap, "en")!!.contains("map"))
    }

    @Test
    fun `Pretty formats playlists and placements cleanly`() {
        assertEquals("Battle Royale (experience_br)", Pretty.playlist("experience_br", "en"))
        assertEquals("Zero Build · Squads", Pretty.playlist("Playlist_NoBuildBR_Squads", "en"))
        assertEquals("Victory Royale!", Pretty.placement(MatchRecordDto(placement = 1), "en"))
        assertEquals("2nd / 100", Pretty.placement(MatchRecordDto(placement = 2, totalPlayers = 100), "en"))
    }

    @Test
    fun `L localizes strings and counts in French English and Spanish`() {
        assertEquals("0 chests", L.count("en", 0, "coffre", "chest", "cofre"))
        assertEquals("1 chest", L.count("en", 1, "coffre", "chest", "cofre"))
        assertEquals("1 coffre", L.count("fr", 1, "coffre", "chest", "cofre"))
        assertEquals("2 coffres", L.count("fr", 2, "coffre", "chest", "cofre"))
        assertEquals("2 cofres", L.count("es", 2, "coffre", "chest", "cofre"))
    }

    @Test
    fun `ReportExporter generates escaped HUD and Classic HTML and CSV rows`() {
        val m = MatchRecordDto(
            sessionId = "s1",
            replayFile = "Match #1 (Session #1)",
            analyzedAtUtc = "2026-11-21T18:00:00Z",
            matchStartUtc = "2026-11-21T17:45:00Z",
            playlist = "experience_br",
            playerName = "Kulet<X>",
            placement = 1,
            totalPlayers = 100,
            chestsOpened = 15,
            treasureFinds = listOf(TreasureFindDto(item = "Gizmo")),
            sessionNumber = 1,
            matchNumber = 1,
        )
        val view = TrackerView(listOf(m), 0, emptyList(), emptyList())
        val ov = OverlayModel.buildLive(view, LiveMatchState(active = true, chests = 3), sessionNumber = 1, lang = "en")

        val hud = ReportExporter.overlayHtml(ov, style = "hud", lang = "en")
        assertTrue(hud.contains("18"))
        assertTrue(hud.contains("class=\"dot\""))

        val classic = ReportExporter.overlayHtml(ov, style = "classic", lang = "en")
        assertTrue(classic.contains("Match 2 · Session 1"))

        val csv = ReportExporter.matchesCsv(listOf(m), lang = "en", timeZone = "utc", clockFormat = "24", isSystem24 = true)
        assertTrue(csv.contains("date;mode;player;placement;players;chests"))
        assertTrue(csv.contains("Battle Royale (experience_br);Kulet<X>;1;100;15;0;0;Gizmo;Match #1 (Session #1)"))
    }

    @Test
    fun `Profiles groups matches by epicId or playerName and builds combined view`() {
        val m1 = MatchRecordDto(
            sessionId = "1",
            playerName = "PlayerA",
            epicId = "id_a",
            playlist = "experience_br",
            matchStartUtc = "2026-11-21T10:00:00Z",
            chestsOpened = 12,
        )
        val m2 = MatchRecordDto(
            sessionId = "2",
            playerName = "PlayerB",
            epicId = "id_b",
            playlist = "experience_br",
            matchStartUtc = "2026-11-21T11:00:00Z",
            chestsOpened = 7,
        )
        val profiles = Profiles.list(listOf(m1, m2), "en")
        assertEquals(2, profiles.size)
        assertEquals("id_b", profiles.first().key) // most recent first

        val view = Profiles.buildView(
            matches = listOf(m1, m2),
            manualEntriesByProfile = mapOf("id_a" to listOf(ManualEntryDto(id = "e1", chests = 5))),
            obtainedByProfile = mapOf("id_a" to listOf("w1-jam")),
            selectedProfile = "id_a",
            lang = "en",
        )
        assertEquals(1, view.matches.size)
        assertEquals(12, view.countedChests)
        assertEquals(17, view.totalChests)
        assertNotNull(view.obtainedIds.firstOrNull { it == "w1-jam" })
    }
}
