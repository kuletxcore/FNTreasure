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

    fun squadLabel(lang: String): String = L.t(lang, "Équipe", "Squad", "Equipo")
    fun lastMatchCaption(lang: String): String = L.t(lang, "dernière partie", "last match", "última partida")

    fun build(
        view: TrackerView,
        live: MatchRecordDto?,
        inProgress: Boolean,
        showSquad: Boolean,
        lang: String,
    ): OverlayView {
        val liveChests = live?.chestsOpened ?: 0
        val liveTreasures = live?.treasureFinds?.size ?: 0
        val total = view.totalChests + liveChests
        val team = view.teamChests + (live?.teamChestsOpened ?: 0)
        val squad = if (showSquad && team > 0) L.n(lang, total + team) else null

        val sessionMatches = view.matches.filter(::isThisSession)
        val sessionDetail = if (sessionMatches.isNotEmpty()) {
            val avg = sessionMatches.sumOf { it.chestsOpened }.toDouble() / sessionMatches.size
            val avgText = avg.roundToInt().toString()
            "${L.count(lang, sessionMatches.size, "partie", "parties", "match", "matches", "partida", "partidas")} · ${L.t(lang, "moy.", "avg.", "media")} $avgText"
        } else null

        val sessionChests = sessionMatches.sumOf { it.chestsOpened } + liveChests
        val rows = mutableListOf<OverlayRow>()
        rows.add(
            OverlayRow(
                label = L.t(lang, "Session", "Session", "Sesión"),
                value = L.count(lang, sessionChests, "coffre", "chest", "cofre"),
                detail = sessionDetail,
            )
        )

        val last = view.matches.maxByOrNull { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
        if (last != null) {
            rows.add(
                OverlayRow(
                    label = L.t(lang, "Dernière", "Last", "Última"),
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

        return when {
            live != null -> OverlayView(
                total = L.n(lang, total),
                current = L.n(lang, live.chestsOpened),
                currentCaption = L.t(lang, "partie en cours", "current match", "partida actual"),
                currentLive = true,
                badge = L.t(lang, "● EN DIRECT", "● LIVE", "● EN DIRECTO"),
                rows = rows,
                treasures = L.n(lang, treasures),
                session = L.n(lang, sessionChests),
                last = last?.let { L.n(lang, it.chestsOpened) },
                squad = squad,
            )
            inProgress -> OverlayView(
                total = L.n(lang, total),
                current = "…",
                currentCaption = L.t(lang, "en cours", "in progress", "en curso"),
                currentLive = true,
                badge = L.t(lang, "● PARTIE EN COURS", "● MATCH IN PROGRESS", "● PARTIDA EN CURSO"),
                rows = rows,
                treasures = L.n(lang, treasures),
                session = L.n(lang, sessionChests),
                last = last?.let { L.n(lang, it.chestsOpened) },
                squad = squad,
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
            )
        }
    }
}
