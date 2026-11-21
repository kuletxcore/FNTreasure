package com.kuletx.forttreasure.domain

import java.time.Instant
import java.util.UUID

object Profiles {
    fun key(m: MatchRecordDto): String =
        if (!m.epicId.isNullOrEmpty()) m.epicId else m.playerName ?: ""

    fun counted(matches: List<MatchRecordDto>): List<MatchRecordDto> =
        matches.filter(GameModes::counts)

    private fun whenInstant(m: MatchRecordDto): Instant =
        L.parseUtc(m.matchStartUtc) ?: L.parseUtc(m.analyzedAtUtc) ?: Instant.EPOCH

    fun list(matches: List<MatchRecordDto>, lang: String): List<Profile> =
        counted(matches)
            .groupBy(::key)
            .map { (k, group) ->
                val latest = group.maxByOrNull(::whenInstant)!!
                val name = group.sortedByDescending(::whenInstant)
                    .mapNotNull { it.playerName?.takeIf { n -> n.isNotBlank() } }
                    .firstOrNull()
                    ?: L.t(lang, "Compte inconnu", "Unknown account", "Cuenta desconocida")
                Profile(
                    key = k,
                    name = name,
                    matches = group.size,
                    chests = group.sumOf { it.chestsOpened },
                    lastPlayedEpochMs = whenInstant(latest).toEpochMilli(),
                )
            }
            .sortedByDescending { it.lastPlayedEpochMs }

    fun current(matches: List<MatchRecordDto>, selectedProfile: String?, lang: String): Profile? {
        val all = list(matches, lang)
        return all.firstOrNull { it.key == selectedProfile } ?: all.firstOrNull()
    }

    fun buildView(
        matches: List<MatchRecordDto>,
        manualEntriesByProfile: Map<String, List<ManualEntryDto>>,
        obtainedByProfile: Map<String, List<String>>,
        selectedProfile: String?,
        lang: String,
    ): TrackerView {
        val cur = current(matches, selectedProfile, lang)
        val countedMatches = counted(matches)
        return if (cur == null) {
            val entries = manualEntriesByProfile[""].orEmpty()
                .sortedWith(compareBy<ManualEntryDto> { it.kind == ManualEntryDto.EARLIER }
                    .thenByDescending { L.parseUtc(it.addedUtc) ?: Instant.EPOCH })
            TrackerView(
                matches = countedMatches,
                manualAdjust = entries.sumOf { it.chests },
                manualEntries = entries,
                obtainedIds = obtainedByProfile[""].orEmpty(),
            )
        } else {
            val profileEntries = manualEntriesByProfile[cur.key].orEmpty()
            val earlyEntries = manualEntriesByProfile[""].orEmpty()
            val combinedEntries = (profileEntries + earlyEntries)
                .sortedWith(compareBy<ManualEntryDto> { it.kind == ManualEntryDto.EARLIER }
                    .thenByDescending { L.parseUtc(it.addedUtc) ?: Instant.EPOCH })
            val obtained = (obtainedByProfile[cur.key].orEmpty() + obtainedByProfile[""].orEmpty()).distinct()
            TrackerView(
                matches = countedMatches.filter { key(it) == cur.key },
                manualAdjust = combinedEntries.sumOf { it.chests },
                manualEntries = combinedEntries,
                obtainedIds = obtained,
            )
        }
    }

    fun reconcileHistory(entries: List<ManualEntryDto>, targetTotal: Int): List<ManualEntryDto> {
        val list = entries.toMutableList()
        val missing = targetTotal - list.sumOf { it.chests }
        if (missing == 0) return list
        val idx = list.indexOfFirst { it.kind == ManualEntryDto.EARLIER }
        if (idx < 0) {
            list.add(ManualEntryDto(id = ManualEntryDto.EARLIER, chests = missing, kind = ManualEntryDto.EARLIER))
        } else {
            val newVal = list[idx].chests + missing
            if (newVal == 0) list.removeAt(idx) else list[idx] = list[idx].copy(chests = newVal)
        }
        return list
    }

    fun newManualEntry(chests: Int, careerMatches: Int? = null, weekFromUtc: Instant? = null): ManualEntryDto =
        ManualEntryDto(
            id = UUID.randomUUID().toString().replace("-", "").take(8),
            chests = chests,
            addedUtc = Instant.now().toString(),
            kind = if (careerMatches != null) ManualEntryDto.ESTIMATE else ManualEntryDto.HAND,
            careerMatches = careerMatches,
            weekFromUtc = if (careerMatches == null) weekFromUtc?.toString() else null,
        )
}
