package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OverlayModelTest {

    private fun sampleMatch(
        sessionId: String,
        chests: Int,
        sessionNum: Int? = 1,
        matchNum: Int? = 1,
        startUtc: String = "2026-11-21T12:00:00Z",
        treasures: List<TreasureFindDto> = emptyList(),
    ): MatchRecordDto = MatchRecordDto(
        sessionId = sessionId,
        replayFile = "Match #$matchNum",
        analyzedAtUtc = startUtc,
        matchStartUtc = startUtc,
        durationSeconds = 900,
        playlist = "experience_br",
        chestsOpened = chests,
        treasureFinds = treasures,
        sessionNumber = sessionNum,
        matchNumber = matchNum,
    )

    @Test
    fun `live match chests never overwrite match number or session number`() {
        val emptyView = TrackerView(
            matches = emptyList(),
            manualAdjust = 0,
            manualEntries = emptyList(),
            obtainedIds = emptyList(),
        )
        val live = LiveMatchState(
            active = true,
            chests = 9,
            treasures = listOf(
                TreasureFindDto(item = "No Trespassing", prizeId = "DelilahPrize:dpid_item3"),
                TreasureFindDto(item = "Gizmo"),
            ),
            startedUtc = Instant.now().toString(),
        )

        val ov = OverlayModel.buildLive(
            view = emptyView,
            liveMatch = live,
            sessionNumber = 1,
            sessionStartedEpochMs = 1L,
            lang = "en",
        )

        assertEquals(1, ov.matchNumber)
        assertEquals(1, ov.sessionNumber)
        assertEquals(9, ov.liveChests)
        assertEquals(2, ov.liveTreasures)
        assertEquals("9", ov.current)
        assertEquals("9", ov.session)
        assertEquals("9", ov.total)
        assertEquals("Match 1 · Session 1", ov.currentCaption)
        assertTrue(ov.currentLive)
        assertNull(ov.last)
    }

    @Test
    fun `completed match 1 advances to match 2 in same session and keeps last count`() {
        val m1 = sampleMatch(
            sessionId = "m1",
            chests = 15,
            sessionNum = 1,
            matchNum = 1,
            treasures = listOf(
                TreasureFindDto(item = "No Trespassing", prizeId = "DelilahPrize:dpid_item3"),
                TreasureFindDto(item = "Gizmo"),
            ),
        )
        val view = TrackerView(
            matches = listOf(m1),
            manualAdjust = 0,
            manualEntries = emptyList(),
            obtainedIds = emptyList(),
        )

        // Idle between Match 1 and Match 2
        val idle = OverlayModel.buildLive(
            view = view,
            liveMatch = LiveMatchState(active = false),
            sessionNumber = 1,
            sessionStartedEpochMs = 1L,
            lang = "en",
        )
        assertEquals(1, idle.lastMatchNumber)
        assertEquals("15", idle.last)
        assertEquals("15", idle.session)
        assertEquals("2", idle.treasures)
        assertFalse(idle.currentLive)

        // Active Match 2 with 4 chests
        val activeM2 = OverlayModel.buildLive(
            view = view,
            liveMatch = LiveMatchState(active = true, chests = 4),
            sessionNumber = 1,
            sessionStartedEpochMs = 1L,
            lang = "en",
        )
        assertEquals(2, activeM2.matchNumber)
        assertEquals(1, activeM2.sessionNumber)
        assertEquals(4, activeM2.liveChests)
        assertEquals("19", activeM2.session)
        assertEquals("19", activeM2.total)
        assertEquals("Match 2 · Session 1", activeM2.currentCaption)
    }

    @Test
    fun `starting session 2 resets session chests and match number while preserving total`() {
        val m1 = sampleMatch(sessionId = "m1", chests = 15, sessionNum = 1, matchNum = 1)
        val view = TrackerView(
            matches = listOf(m1),
            manualAdjust = 10,
            manualEntries = emptyList(),
            obtainedIds = emptyList(),
        )

        val session2Live = OverlayModel.buildLive(
            view = view,
            liveMatch = LiveMatchState(active = true, chests = 3),
            sessionNumber = 2,
            sessionStartedEpochMs = Instant.now().toEpochMilli(),
            lang = "en",
        )

        assertEquals(2, session2Live.sessionNumber)
        assertEquals(1, session2Live.matchNumber)
        assertEquals(0, session2Live.sessionMatchCount)
        assertEquals("3", session2Live.session)
        assertEquals("28", session2Live.total) // 15 + 10 + 3
        assertEquals("Match 1 · Session 2", session2Live.currentCaption)
    }

    @Test
    fun `nextMatchNumber handles sparse match numbers safely`() {
        val matches = listOf(
            sampleMatch("a", chests = 5, sessionNum = 1, matchNum = 1),
            sampleMatch("b", chests = 8, sessionNum = 1, matchNum = 4),
        )
        assertEquals(5, OverlayModel.nextMatchNumber(matches))
        assertEquals(1, OverlayModel.nextMatchNumber(emptyList()))
    }

    @Test
    fun `overlay appearance clamps size and opacity within bounds`() {
        assertEquals(70, OverlayAppearance.clampSize(10))
        assertEquals(175, OverlayAppearance.clampSize(300))
        assertEquals(30, OverlayAppearance.clampOpacity(5))
        assertEquals(100, OverlayAppearance.clampOpacity(150))
    }
}
