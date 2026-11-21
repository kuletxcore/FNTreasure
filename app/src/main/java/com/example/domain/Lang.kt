package com.example.domain

import android.text.format.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object L {
    val codes = listOf("fr", "en", "es")

    fun normalize(code: String?): String? = when (code?.trim()?.lowercase(Locale.ROOT)) {
        "fr", "fra", "fre", "french", "français", "francais", "francés", "frances" -> "fr"
        "en", "eng", "english", "anglais", "inglés", "ingles" -> "en"
        "es", "spa", "spanish", "español", "espanol", "espagnol" -> "es"
        else -> null
    }

    fun systemDefault(): String =
        normalize(Locale.getDefault().language) ?: "en"

    fun nativeName(code: String): String = when (code) {
        "en" -> "English"
        "es" -> "Español"
        else -> "Français"
    }

    fun locale(lang: String): Locale = when (lang) {
        "en" -> Locale.UK
        "es" -> Locale("es", "ES")
        else -> Locale.FRANCE
    }

    fun t(lang: String, fr: String, en: String, es: String): String = when (lang) {
        "en" -> en
        "es" -> es
        else -> fr
    }

    fun n(lang: String, number: Int): String =
        NumberFormat.getIntegerInstance(locale(lang)).format(number)

    fun nLong(lang: String, number: Long): String =
        NumberFormat.getIntegerInstance(locale(lang)).format(number)

    fun f(lang: String, value: Double, decimals: Int = 1): String =
        NumberFormat.getNumberInstance(locale(lang)).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    fun count(lang: String, n: Int, fr: String, en: String, es: String): String =
        count(lang, n, fr, fr + "s", en, en + "s", es, es + "s")

    fun count(
        lang: String,
        n: Int,
        frOne: String,
        frMany: String,
        enOne: String,
        enMany: String,
        esOne: String,
        esMany: String,
    ): String = when (lang) {
        "en" -> "${n(lang, n)} ${if (n == 1) enOne else enMany}"
        "es" -> "${n(lang, n)} ${if (n == 1) esOne else esMany}"
        else -> "${n(lang, n)} ${if (n > 1) frMany else frOne}"
    }

    fun ordinal(lang: String, n: Int): String {
        if (lang == "es") return "$n.º"
        if (lang != "en") return if (n == 1) "1er" else "${n}e"
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    fun is24Hour(clockFormat: String, isSystem24Hour: Boolean = true): Boolean = when (clockFormat) {
        "12" -> false
        "24" -> true
        else -> isSystem24Hour
    }

    fun zoneId(timeZone: String): ZoneId =
        if (timeZone == "utc") ZoneOffset.UTC else ZoneId.systemDefault()

    fun zoneSuffix(timeZone: String): String =
        if (timeZone == "utc") " UTC" else ""

    fun localOffsetLabel(): String {
        val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val totalSeconds = offset.totalSeconds
        if (totalSeconds == 0) return "UTC"
        val sign = if (totalSeconds < 0) "−" else "+"
        val absSec = abs(totalSeconds)
        val hours = absSec / 3600
        val mins = (absSec % 3600) / 60
        return if (mins != 0) "UTC$sign$hours:${mins.toString().padStart(2, '0')}" else "UTC$sign$hours"
    }

    fun timePattern(is24Hour: Boolean, seconds: Boolean = false): String =
        if (is24Hour) {
            if (seconds) "HH:mm:ss" else "HH:mm"
        } else {
            if (seconds) "h:mm:ss a" else "h:mm a"
        }

    fun clock(
        instant: Instant,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean = true,
        seconds: Boolean = false,
    ): String {
        val fmt = DateTimeFormatter.ofPattern(
            timePattern(is24Hour(clockFormat, isSystem24), seconds),
            locale(lang),
        ).withZone(zoneId(timeZone))
        return fmt.format(instant) + zoneSuffix(timeZone)
    }

    fun dateTimeShort(
        instant: Instant,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean = true,
    ): String {
        val datePart = if (lang == "en") "d MMM " else "dd/MM "
        val fmt = DateTimeFormatter.ofPattern(
            datePart + timePattern(is24Hour(clockFormat, isSystem24)),
            locale(lang),
        ).withZone(zoneId(timeZone))
        return fmt.format(instant) + zoneSuffix(timeZone)
    }

    fun shortWhen(
        instant: Instant,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean = true,
    ): String {
        val pat = t(lang, "d MMM ", "MMM d, ", "d MMM ") + timePattern(is24Hour(clockFormat, isSystem24))
        return DateTimeFormatter.ofPattern(pat, locale(lang))
            .withZone(zoneId(timeZone))
            .format(instant) + zoneSuffix(timeZone)
    }

    fun dayTime(
        instant: Instant,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean = true,
    ): String {
        val pat = t(lang, "EEEE d MMM, ", "EEEE, MMM d, ", "EEEE d MMM, ") + timePattern(is24Hour(clockFormat, isSystem24))
        return DateTimeFormatter.ofPattern(pat, locale(lang))
            .withZone(zoneId(timeZone))
            .format(instant) + zoneSuffix(timeZone)
    }

    fun weekdayTime(
        instant: Instant,
        lang: String,
        timeZone: String,
        clockFormat: String,
        isSystem24: Boolean = true,
    ): String {
        val pat = "EEEE " + timePattern(is24Hour(clockFormat, isSystem24))
        return DateTimeFormatter.ofPattern(pat, locale(lang))
            .withZone(zoneId(timeZone))
            .format(instant) + zoneSuffix(timeZone)
    }

    fun dateOnly(instant: Instant, lang: String, timeZone: String): String {
        val pat = t(lang, "d MMMM", "MMMM d", "d 'de' MMMM")
        return DateTimeFormatter.ofPattern(pat, locale(lang))
            .withZone(zoneId(timeZone))
            .format(instant)
    }

    fun dayAxisLabel(date: LocalDate, lang: String, includeDayOfWeek: Boolean = false): String {
        val pat = if (includeDayOfWeek) {
            t(lang, "EEE dd/MM", "EEE d MMM", "EEE dd/MM")
        } else {
            t(lang, "dd/MM", "d MMM", "dd/MM")
        }
        return date.format(DateTimeFormatter.ofPattern(pat, locale(lang)))
    }

    fun fullDateAndTime(instant: Instant, lang: String, timeZone: String, clockFormat: String, isSystem24: Boolean = true): String {
        val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale(lang)).withZone(zoneId(timeZone))
        return "${dateFmt.format(instant)} · ${clock(instant, lang, timeZone, clockFormat, isSystem24, seconds = true)}"
    }

    fun parseUtc(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso)
        } catch (_: Exception) {
            try {
                // Handle .NET DateTime strings that might lack trailing Z or have offsets
                java.time.OffsetDateTime.parse(iso).toInstant()
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private val names: Map<String, Pair<String, String>> = mapOf(
        "Coffre" to ("Chest" to "Cofre"),
        "Coffre rare" to ("Rare chest" to "Cofre raro"),
        "Coffre enterré" to ("Buried chest" to "Cofre enterrado"),
        "Coffre d'esprit (rare)" to ("Sprite chest (rare)" to "Cofre de espíritu (raro)"),
        "Coffre des affiches d'esprit" to ("Sprite poster chest" to "Cofre de los carteles de espíritu"),
        "Autre coffre" to ("Other chest" to "Otro cofre"),
        "Canon à butin" to ("Loot cannon" to "Cañón de botín"),
        "Caisse de munitions" to ("Ammo box" to "Caja de munición"),
        "Caisse de nourriture" to ("Food box" to "Caja de comida"),
        "Autre conteneur" to ("Other container" to "Otro contenedor"),
        "Trésor" to ("Treasure" to "Tesoro"),
        "Trésor (type inconnu)" to ("Treasure (unknown type)" to "Tesoro (tipo desconocido)"),
        "Feu d'artifice doré" to ("Golden firework" to "Fuego artificial dorado"),
        "Extracteur portable" to ("Portable extractor" to "Extractor portátil"),
        "Localisateur d'esprit" to ("Sprite locator" to "Localizador de espíritus"),
        "Localisateur chanceux" to ("Lucky locator" to "Localizador de la suerte"),
        "Accélérateur d'extraction" to ("Extraction accelerator" to "Acelerador de extracción"),
        "inconnu" to ("unknown" to "desconocido"),
    ).mapKeys { it.key.lowercase(Locale.ROOT) }

    fun name(stored: String?, lang: String): String {
        if (stored.isNullOrEmpty()) return ""
        val canonical = TreasureNames.canonical(ChestKinds.canonical(stored))
        if (lang == "fr") return canonical
        val pair = names[canonical.lowercase(Locale.ROOT)] ?: return canonical
        return if (lang == "en") pair.first else pair.second
    }
}

object ChestKinds {
    const val NORMAL = "Coffre"
    const val RARE = "Coffre rare"
    const val BURIED = "Coffre enterré"
    const val EXTRACTION = "Coffre d'esprit (rare)"
    const val POSTER_CHEST = "Coffre des affiches d'esprit"
    const val OTHER = "Autre coffre"

    const val CANNON = "Canon à butin"
    const val AMMO_BOX = "Caisse de munitions"
    const val FOOD_BOX = "Caisse de nourriture"
    const val OTHER_CONTAINER = "Autre conteneur"

    private val chestSet = setOf(NORMAL, RARE, BURIED, EXTRACTION, POSTER_CHEST, OTHER)

    fun isChest(kind: String): Boolean = canonical(kind) in chestSet

    fun canonical(kind: String): String =
        if (kind == "Coffre d'extraction / reliques" || kind == "Coffre de chambre forte") EXTRACTION else kind

    fun colorHex(kind: String): Long = when (canonical(kind)) {
        NORMAL -> 0xFFC9A24FL
        RARE -> 0xFF2C7FB8L
        BURIED -> 0xFF8A5A2BL
        EXTRACTION -> 0xFF1F7F7AL
        POSTER_CHEST -> 0xFF7A4FC2L
        else -> 0xFF9A8F7AL
    }
}

object TreasureNames {
    const val UNKNOWN_PREFIX = "Unknown:"
    private const val OLD_UNKNOWN_PREFIX = "Inconnu:"
    const val UNKNOWN_FR = "Trésor (type inconnu)"
    const val UNKNOWN_EN = "Treasure (unknown type)"
    const val UNKNOWN_ES = "Tesoro (tipo desconocido)"
    private const val OLD_UNKNOWN_NAME = "Gizmo de trésor"

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun canonical(stored: String): String =
        if (stored == OLD_UNKNOWN_NAME) UNKNOWN_FR else stored

    private fun knownPrize(id: String, lang: String): String? {
        val prize = TreasureCatalog.find(id) ?: return null
        if (prize.kind == TreasureCatalog.PIRATE_SHOP) {
            return TreasureCatalog.kindName(prize.kind, lang)
        }
        val kind = TreasureCatalog.kindName(prize.kind, lang)
        val formattedKind = if (lang == "en" || kind.isEmpty()) kind else kind.replaceFirstChar { it.lowercase() }
        return "${prize.name} ($formattedKind)"
    }

    fun friendly(item: String, lang: String): String {
        if (!item.contains(':')) {
            knownPrize(item, lang)?.let { return it }
        }
        if (uuidRegex.matches(item)) {
            return L.t(lang, "Récompense de coffre doré", "Golden chest reward", "Recompensa de cofre dorado") + " (${item.take(8)})"
        }
        if (item.contains(':')) {
            knownPrize(item, lang)?.let { return it }
            if (item.startsWith(UNKNOWN_PREFIX, ignoreCase = true) || item.startsWith(OLD_UNKNOWN_PREFIX, ignoreCase = true)) {
                return L.t(lang, UNKNOWN_FR, UNKNOWN_EN, UNKNOWN_ES)
            }
            return L.t(lang, "Récompense de coffre doré", "Golden chest reward", "Recompensa de cofre dorado") +
                " (${item.substring(item.indexOf(':') + 1)})"
        }
        val low = item.lowercase(Locale.ROOT)
        return when {
            "flameceleb" in low -> L.name("Feu d'artifice doré", lang)
            "relicextractor" in low -> L.name("Extracteur portable", lang)
            "relicfinder" in low -> L.name("Localisateur d'esprit", lang)
            "luckylocator" in low -> L.name("Localisateur chanceux", lang)
            "smugglerextractionkey" in low || "extractionaccelerat" in low -> L.name("Accélérateur d'extraction", lang)
            else -> item
        }
    }
}
