package com.example.domain

import java.time.Instant

object ReportExporter {
    private fun h(s: String?): String = buildString {
        for (c in s.orEmpty()) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }

    fun overlayHtml(v: OverlayView, style: String, lang: String): String =
        if (style == "classic") overlayClassicHtml(v, lang) else overlayHudHtml(v, lang)

    private fun overlayHudHtml(v: OverlayView, lang: String): String {
        fun row(label: String, value: String): String =
            """<div class="r"><span>${h(label)}</span>${if (value.isNotEmpty()) """<span class="sep">•</span><b>${h(value)}</b>""" else ""}</div>"""
        val squad = v.squad?.let { row(OverlayModel.squadLabel(lang), it) } ?: ""
        val liveCls = if (v.currentLive && v.current != null && v.current != "…") " live" else ""
        val dot = if (v.currentLive) """<i class="dot"></i>""" else ""
        return """
            <!doctype html><html lang="$lang"><head><meta charset="utf-8"><meta http-equiv="refresh" content="5">
            <title>${h(L.t(lang, "Overlay coffres", "Chest overlay", "Overlay de cofres"))}</title>
            <style>
            html,body{margin:0;background:transparent}
            body{padding:10px 14px;display:inline-block;color:#fff;font-family:system-ui,sans-serif;font-style:italic;font-weight:700;text-align:right;
                 -webkit-text-stroke:4px #0b0f18;paint-order:stroke fill;letter-spacing:.01em}
            .total{display:flex;align-items:center;justify-content:flex-end;gap:10px;font-size:46px;line-height:1.05}
            .total.live{color:#ffd23f}
            .total svg{width:30px;height:26px;flex:none}
            .r{display:flex;align-items:center;justify-content:flex-end;gap:8px;font-size:21px;line-height:1.3}
            .sep{opacity:.9}
            .dot{width:13px;height:13px;border-radius:50%;background:#ff3b30;box-shadow:0 0 0 2px #0b0f18;flex:none}
            .t{color:#d6beff}.t .star{color:#c49bff}
            </style></head><body>
            <div class="total$liveCls">$dot<svg viewBox="0 0 30 26" aria-hidden="true"><rect x="2" y="10" width="26" height="14" rx="2" fill="#ffd23f" stroke="#0b0f18" stroke-width="2.5"/><path d="M2 12 Q15 -2 28 12" fill="#e0a526" stroke="#0b0f18" stroke-width="2.5"/><rect x="12.5" y="12" width="5" height="6" rx="1" fill="#0b0f18"/></svg>${h(v.total)}</div>
            ${row(L.t(lang, "Dernière", "Last", "Última"), v.last ?: "—")}
            ${row(L.t(lang, "Session", "Session", "Sesión"), v.session)}
            $squad
            <div class="r t"><span class="star">★</span><b>${h(v.treasures)}</b></div>
            </body></html>
        """.trimIndent()
    }

    private fun overlayClassicHtml(v: OverlayView, lang: String): String {
        val rows = v.rows.joinToString("") { r ->
            """<div class="row${if (r.treasure) " t" else ""}"><span>${h(r.label)}</span><b>${h(r.value)}${r.detail?.let { " <small>${h(it)}</small>" } ?: ""}</b></div>"""
        }
        val badge = v.badge?.let { """<span class="live">${h(it)}</span>""" } ?: ""
        val curCls = when {
            v.current == null -> "none"
            v.currentLive -> "cur"
            else -> ""
        }
        return """
            <!doctype html><html lang="$lang"><head><meta charset="utf-8"><meta http-equiv="refresh" content="5">
            <title>${h(L.t(lang, "Overlay coffres", "Chest overlay", "Overlay de cofres"))}</title>
            <style>
            html,body{margin:0;background:transparent;color:#e2e2e8;font:500 16px/1.3 system-ui,sans-serif}
            .box{display:inline-block;min-width:260px;padding:14px 18px 12px;border-radius:16px;background:rgba(18,18,22,.86);border:1px solid #3a3a42}
            .head{display:flex;justify-content:space-between;gap:16px;font:700 12px system-ui,sans-serif;letter-spacing:.08em;color:#9696a2}
            .live{color:#60d680}
            .counters{display:flex;align-items:stretch;gap:18px;margin:4px 0 10px}
            .counters>div{display:flex;flex-direction:column}
            .sep{width:1px;background:#2c2c34;margin:8px 0 2px}
            .big{font-size:40px;font-weight:800;line-height:1.1;font-variant-numeric:tabular-nums}
            .total{color:#f2b84b}.cur{color:#6ee096}.none{color:#50505c}
            .cap{font-size:11px;font-weight:600;color:#82828e}
            .rows{border-top:1px solid #2c2c34;padding-top:8px;display:grid;grid-template-columns:auto 1fr;gap:4px 12px}
            .row{display:contents}.row span{font-size:13px;font-weight:600;color:#82828e}.row b{font-weight:500}
            .row.t b{color:#c4a0ff}.row small{font-size:11px;font-weight:600;color:#82828e}
            </style></head><body><div class="box">
            <div class="head"><span>${h(L.t(lang, "COFFRES", "CHESTS", "COFRES"))}</span>$badge</div>
            <div class="counters">
              <div><span class="big total">${h(v.total)}</span><span class="cap">total</span></div>
              <div class="sep"></div>
              <div><span class="big $curCls">${h(v.current ?: "—")}</span><span class="cap">${h(v.currentCaption)}</span></div>
            </div>
            <div class="rows">$rows</div>
            </div></body></html>
        """.trimIndent()
    }

    fun matchesCsv(
        matches: List<MatchRecordDto>,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            L.t(
                lang,
                "date;mode;joueur;place;joueurs;coffres;coffres_coequipiers;caisses_munitions;tresors;fichier",
                "date;mode;player;placement;players;chests;teammate_chests;ammo_boxes;treasures;file",
                "fecha;modo;jugador;puesto;jugadores;cofres;cofres_companeros;cajas_municion;tesoros;archivo",
            )
        )
        val sorted = GameModes.let {
            Profiles.counted(matches).sortedBy { L.parseUtc(it.matchStartUtc) ?: L.parseUtc(it.analyzedAtUtc) ?: Instant.EPOCH }
        }
        for (m in sorted) {
            val instant = L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH
            val dateStr = L.dateTimeShort(instant, lang, timeZone, clockFormat, isSystem24)
            val treasures = m.treasureFinds.joinToString(" + ") { it.displayName(lang) }
            sb.appendLine(
                listOf(
                    dateStr,
                    Pretty.playlist(m.playlist, lang),
                    m.playerName.orEmpty(),
                    m.placement?.toString().orEmpty(),
                    m.totalPlayers?.toString().orEmpty(),
                    m.chestsOpened.toString(),
                    m.teamChestsOpened.toString(),
                    m.ammoBoxesOpened.toString(),
                    treasures,
                    m.replayFile,
                ).joinToString(";")
            )
        }
        return sb.toString()
    }
}
