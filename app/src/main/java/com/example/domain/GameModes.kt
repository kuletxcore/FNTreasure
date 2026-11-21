package com.example.domain

import java.util.Locale

object GameModes {
    val seasonMaps = listOf("/Delilah/Maps/Delilah_Terrain")

    // Supports regular Battle Royale playlists plus Fortnite Discover experience_br (play.fn.gg/island/experience_br)
    private val battleRoyalePlaylist = Regex(
        """^(Playlist_(Default(Solo|Duo|Duos|Trio|Trios|Squad|Squads)|Trios|NoBuildBR_(Solo|Duo|Duos|Trio|Trios|Squad|Squads))|(Playlist_)?experience_br)$""",
        RegexOption.IGNORE_CASE,
    )

    fun isSeasonMap(map: String?): Boolean =
        map != null && seasonMaps.any { map.equals(it, ignoreCase = true) }

    fun isBattleRoyalePlaylist(playlist: String?): Boolean =
        playlist != null && battleRoyalePlaylist.matches(playlist.trim())

    fun counts(m: MatchRecordDto): Boolean =
        !m.isCustomMatch && isBattleRoyalePlaylist(m.playlist) && (m.map == null || isSeasonMap(m.map))

    fun whyNot(m: MatchRecordDto, lang: String): String? {
        if (counts(m)) return null
        val mode = if (m.playlist.isNullOrEmpty()) L.t(lang, "mode inconnu", "unknown mode", "modo desconocido") else m.playlist
        if (m.isCustomMatch) {
            return L.t(lang, "partie personnalisée ($mode)", "custom match ($mode)", "partida personalizada ($mode)")
        }
        if (m.map != null && !isSeasonMap(m.map)) {
            return L.t(lang, "pas sur la carte de la saison ($mode, carte ${m.map})", "not on the season's map ($mode, map ${m.map})", "no es el mapa de la temporada ($mode, mapa ${m.map})")
        }
        return L.t(lang, "pas le Battle Royale classique ($mode)", "not the regular Battle Royale ($mode)", "no es el Battle Royale normal ($mode)")
    }
}

object Pretty {
    fun playlist(playlist: String?, lang: String): String {
        if (playlist.isNullOrEmpty()) {
            return L.t(lang, "mode inconnu", "unknown mode", "modo desconocido")
        }
        val p = playlist.lowercase(Locale.ROOT)
        if (p == "experience_br" || p == "playlist_experience_br") {
            return "Battle Royale (experience_br)"
        }
        val parts = mutableListOf<String>()
        if ("habanero" in p || "ranked" in p) parts.add(L.t(lang, "Classée", "Ranked", "Clasificatoria"))
        if ("nobuild" in p) parts.add(L.t(lang, "Zéro construction", "Zero Build", "Cero construcción"))
        when {
            "solo" in p -> parts.add(L.t(lang, "Solo", "Solo", "Solitario"))
            "duo" in p -> parts.add(L.t(lang, "Duo", "Duos", "Dúos"))
            "trio" in p -> parts.add(L.t(lang, "Trio", "Trios", "Tríos"))
            "squad" in p -> parts.add(L.t(lang, "Section", "Squads", "Escuadrones"))
        }
        return if (parts.isNotEmpty()) parts.joinToString(" · ") else playlist.removePrefix("Playlist_")
    }

    fun placement(m: MatchRecordDto, lang: String): String {
        val place = m.placement
        if (place == null || place <= 0) {
            return L.t(lang, "place inconnue", "unknown placement", "puesto desconocido")
        }
        if (place == 1) {
            return L.t(lang, "Top 1 !", "Victory Royale!", "¡Victoria magistral!")
        }
        val total = m.totalPlayers
        return if (total != null && total > 0) "${L.ordinal(lang, place)} / $total" else L.ordinal(lang, place)
    }

    fun clock(seconds: Float): String {
        val s = seconds.toInt().coerceAtLeast(0)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    fun itemName(item: String): String {
        var name = item
        for (prefix in listOf("WID_", "Athena", "AmmoData")) {
            name = name.replace(prefix, "")
        }
        return name.replace("_", " ").trim()
    }
}
