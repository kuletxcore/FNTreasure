package com.example.domain

import java.time.Instant
import kotlin.math.roundToInt

data class OverlayRow(
    val label: String,
    val value: String,
    val detail: String? = null,
    val treasure: Boolean = false,
)

data class OverlayView(
    val total: String,
    val current: String?,
    val currentCaption: String,
    val currentLive: Boolean,
    val badge: String?,
    val rows: List<OverlayRow>,
    val treasures: String,
    val session: String,
    val last: String?,
    val squad: String?,
    val sessionNumber: Int = 1,
    val matchNumber: Int = 1,
    val lastMatchNumber: Int? = null,
    val liveChests: Int = 0,
    val liveTreasures: Int = 0,
    val sessionMatchCount: Int = 0,
)

object OverlayAppearance {
    val sizes = listOf(70, 80, 90, 100, 115, 130, 150, 175)
    val opacities = listOf(100, 90, 80, 70, 60, 50, 40, 30)

    fun clampSize(percent: Int): Int = percent.coerceIn(sizes.first(), sizes.last())
    fun clampOpacity(percent: Int): Int = percent.coerceIn(opacities.last(), 100)
}

object OverlayModel {
    val sessionStartUtc: Instant = Instant.now()

    fun isThisSession(m: MatchRecordDto): Boolean {
        val start = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: return false
        val end = start.plusSeconds((m.durationSeconds ?: 0).toLong())
        return !end.isBefore(sessionStartUtc.minusSeconds(120))
    }

    fun isInSession(
        m: MatchRecordDto,
        sessionNumber: Int,
        sessionStartedEpochMs: Long,
    ): Boolean {
        if (m.sessionNumber != null) {
            return m.sessionNumber == sessionNumber
        }
        if (sessionStartedEpochMs > 0L) {
            val start = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: return false
            val end = start.plusSeconds((m.durationSeconds ?: 0).toLong())
            return !end.isBefore(Instant.ofEpochMilli(sessionStartedEpochMs).minusSeconds(120))
        }
        return isThisSession(m)
    }

    fun nextMatchNumber(
        sessionMatches: List<MatchRecordDto>,
    ): Int {
        val highestRecorded = sessionMatches.maxOfOrNull { it.matchNumber ?: 0 } ?: 0
        return maxOf(sessionMatches.size, highestRecorded) + 1
    }

    fun squadLabel(lang: String): String = L.t(lang, "Équipe", "Squad", "Equipo")
    fun lastMatchCaption(lang: String): String = L.t(lang, "dernière partie", "last match", "última partida")

    fun build(
        view: TrackerView,
        live: MatchRecordDto?,
        inProgress: Boolean,
        showSquad: Boolean,
        lang: String,
        sessionNumber: Int = 1,
        sessionStartedEpochMs: Long = 0L,
    ): OverlayView {
        val liveState = if (live != null) {
            LiveMatchState(
                active = true,
                chests = live.chestsOpened,
                treasures = live.treasureFinds,
                startedUtc = live.matchStartUtc,
            )
        } else {
            LiveMatchState(active = inProgress)
        }
        return buildLive(
            view = view,
            liveMatch = liveState,
            inProgressHint = inProgress,
            sessionNumber = sessionNumber,
            sessionStartedEpochMs = sessionStartedEpochMs,
            showSquad = showSquad,
            lang = lang,
            teamChestsDelta = live?.teamChestsOpened ?: 0,
        )
    }

    fun buildLive(
        view: TrackerView,
        liveMatch: LiveMatchState,
        inProgressHint: Boolean = liveMatch.active,
        sessionNumber: Int = 1,
        sessionStartedEpochMs: Long = 0L,
        showSquad: Boolean = false,
        lang: String = "en",
        teamChestsDelta: Int = 0,
    ): OverlayView {
        val liveChests = if (liveMatch.active) liveMatch.chests else 0
        val liveTreasures = if (liveMatch.active) liveMatch.treasures.size else 0
        val total = view.totalChests + liveChests
        val team = view.teamChests + teamChestsDelta
        val squad = if (showSquad && team > 0) L.n(lang, total + team) else null

        val sessionMatches = view.matches.filter { isInSession(it, sessionNumber, sessionStartedEpochMs) }
        val nextMatchNum = nextMatchNumber(sessionMatches)
        val last = view.matches.maxByOrNull { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
        val lastMatchNum = last?.matchNumber ?: if (sessionMatches.isNotEmpty()) sessionMatches.size else null

        val sessionDetail = if (sessionMatches.isNotEmpty()) {
            val avg = sessionMatches.sumOf { it.chestsOpened }.toDouble() / sessionMatches.size
            val avgText = avg.roundToInt().toString()
            "${L.count(lang, sessionMatches.size, "partie", "parties", "match", "matches", "partida", "partidas")} · ${L.t(lang, "moy.", "avg.", "media")} $avgText"
        } else null

        val sessionChests = sessionMatches.sumOf { it.chestsOpened } + liveChests
        val rows = mutableListOf<OverlayRow>()
        rows.add(
            OverlayRow(
                label = "${L.t(lang, "Session", "Session", "Sesión")} $sessionNumber",
                value = L.count(lang, sessionChests, "coffre", "chest", "cofre"),
                detail = sessionDetail,
            )
        )

        if (last != null) {
            val mLabel = lastMatchNum?.let { " #$it" } ?: ""
            rows.add(
                OverlayRow(
                    label = "${L.t(lang, "Dernière", "Last", "Última")}$mLabel",
                    value = L.count(lang, last.chestsOpened, "coffre", "chest", "cofre"),
                )
            )
        }

        val treasures = view.matches.sumOf { it.treasureFinds.size } + liveTreasures
        if (squad != null) {
            rows.add(
                OverlayRow(
                    label = squadLabel(lang),
                    value = L.count(lang, total + team, "coffre", "chest", "cofre"),
                )
            )
        }
        rows.add(
            OverlayRow(
                label = L.t(lang, "Trésors", "Treasures", "Tesoros"),
                value = "★ ${L.n(lang, treasures)}",
                treasure = true,
            )
        )

        val displayMatchNum = if (liveMatch.active) nextMatchNum else (lastMatchNum ?: nextMatchNum)

        return when {
            liveMatch.active -> OverlayView(
                total = L.n(lang, total),
                current = L.n(lang, liveMatch.chests),
                currentCaption = L.t(
                    lang,
                    "Partie $nextMatchNum · Session $sessionNumber",
                    "Match $nextMatchNum · Session $sessionNumber",
                    "Partida $nextMatchNum · Sesión $sessionNumber",
                ),
                currentLive = true,
                badge = L.t(lang, "● EN DIRECT · P$nextMatchNum", "● LIVE · M$nextMatchNum", "● EN DIRECTO · P$nextMatchNum"),
                rows = rows,
                treasures = L.n(lang, treasures),
                session = L.n(lang, sessionChests),
                last = last?.let { L.n(lang, it.chestsOpened) },
                squad = squad,
                sessionNumber = sessionNumber,
                matchNumber = displayMatchNum,
                lastMatchNumber = lastMatchNum,
                liveChests = liveMatch.chests,
                liveTreasures = liveTreasures,
                sessionMatchCount = sessionMatches.size,
            )
            inProgressHint -> OverlayView(
                total = L.n(lang, total),
                current = "…",
                currentCaption = L.t(
                    lang,
                    "Partie $nextMatchNum · Session $sessionNumber",
                    "Match $nextMatchNum · Session $sessionNumber",
                    "Partida $nextMatchNum · Sesión $sessionNumber",
                ),
                currentLive = true,
                badge = L.t(lang, "● PARTIE EN COURS", "● MATCH IN PROGRESS", "● PARTIDA EN CURSO"),
                rows = rows,
                treasures = L.n(lang, treasures),
                session = L.n(lang, sessionChests),
                last = last?.let { L.n(lang, it.chestsOpened) },
                squad = squad,
                sessionNumber = sessionNumber,
                matchNumber = nextMatchNum,
                lastMatchNumber = lastMatchNum,
                liveChests = 0,
                liveTreasures = 0,
                sessionMatchCount = sessionMatches.size,
            )
            else -> OverlayView(
                total = L.n(lang, total),
                current = last?.let { L.n(lang, it.chestsOpened) },
                currentCaption = lastMatchCaption(lang),
                currentLive = false,
                badge = null,
                rows = rows,
                treasures = L.n(lang, treasures),
                session = L.n(lang, sessionChests),
                last = last?.let { L.n(lang, it.chestsOpened) },
                squad = squad,
                sessionNumber = sessionNumber,
                matchNumber = displayMatchNum,
                lastMatchNumber = lastMatchNum,
                liveChests = 0,
                liveTreasures = 0,
                sessionMatchCount = sessionMatches.size,
            )
        }
    }
}
