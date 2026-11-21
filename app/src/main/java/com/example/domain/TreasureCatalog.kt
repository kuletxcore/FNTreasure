package com.example.domain

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.lang.Math.log1p
import kotlin.math.ceil
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

data class TreasurePrize(
    val id: String,
    val kind: String,
    val name: String,
    val image: String?,
    val prizeIds: List<String>,
)

data class TreasureWeek(
    val number: Int,
    val from: Instant,
    val to: Instant,
    val title: String,
    val prizes: List<TreasurePrize>,
)

data class TreasureHour(
    val startUtc: Instant,
    val endUtc: Instant,
    val boosts: Map<String, Double>,
    val confirmed: Boolean,
)

data class TreasureOdds(
    val kind: String,
    val oneIn: Int,
    val weekly: Boolean,
)

data class Notice(
    val id: String,
    val fromUtc: Instant,
    val toUtc: Instant,
    val level: String,
    val text: Map<String, String>,
    val title: Map<String, String>,
    val link: String?,
    val minVersion: String?,
    val maxVersion: String?,
) {
    fun textIn(lang: String): String = pick(text, lang)
    fun titleIn(lang: String): String = pick(title, lang)

    fun concerns(versionText: String): Boolean {
        val current = parseVersion(versionText) ?: return true
        val min = minVersion?.let(::parseVersion)
        val max = maxVersion?.let(::parseVersion)
        return (min == null || compareVersions(current, min) >= 0) &&
            (max == null || compareVersions(current, max) <= 0)
    }

    private fun pick(d: Map<String, String>, lang: String): String =
        d[lang] ?: d["en"] ?: d.values.firstOrNull() ?: ""

    private fun parseVersion(v: String): Triple<Int, Int, Int>? {
        val parts = v.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        return Triple(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
    }

    private fun compareVersions(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
        if (a.first != b.first) return a.first.compareTo(b.first)
        if (a.second != b.second) return a.second.compareTo(b.second)
        return a.third.compareTo(b.third)
    }
}

data class CatalogData(
    val revision: Int,
    val updated: String,
    val odds: List<TreasureOdds>,
    val pirateShopAccess: TreasurePrize,
    val weeks: List<TreasureWeek>,
    val treasureHours: List<TreasureHour>,
    val notices: List<Notice>,
    val oddsSource: String,
    val hoursSource: String,
)

data class ChanceResult(
    val p: Double,
    val logNever: Double,
    val average: Double,
    val chests: Int,
) {
    companion object {
        fun of(odds: TreasureOdds, times: List<Instant>, plain: Long): ChanceResult {
            val log = TreasureCatalog.logNever(odds, times, plain)
            return ChanceResult(
                p = -expm1(log),
                logNever = log,
                average = TreasureCatalog.expected(odds, times, plain),
                chests = (times.size + maxOf(0L, plain)).toInt(),
            )
        }
    }
}

enum class VerdictTier { VUNLIKELY, UNLIKELY, POSSIBLE, LIKELY, VLIKELY, CERTAIN }

object TreasureCatalog {
    const val GOLD_OUTFIT = "outfit"
    const val PIRATE_SHOP = "pirate-shop"
    const val VEHICLE_DECAL = "car"
    const val PICKAXE_OR_BACK_BLING = "pickaxe"
    const val JAM_TRACK = "jam"
    const val LOADING_SCREEN = "loading"
    const val GIZMO = "gizmo"

    const val PIRATE_SHOP_PRIZE_ID = "9f73f94d-2e61-4523-97d7-d61235d3365e"
    const val DEFAULT_ODDS_SOURCE = "https://x.com/FortniteStatus/status/2119202610322227327"
    const val DEFAULT_HOURS_SOURCE = "https://fortnite.bot"

    private val allowedKinds = listOf(
        GOLD_OUTFIT, PIRATE_SHOP, VEHICLE_DECAL, PICKAXE_OR_BACK_BLING, JAM_TRACK, LOADING_SCREEN, GIZMO
    )
    private val safeId = Regex("^[a-z0-9][a-z0-9-]{0,39}$")
    private val safePrizeId = Regex("^[A-Za-z0-9:_.-]{1,80}$")

    @Volatile
    private var loaded: CatalogData? = null

    val data: CatalogData
        get() = loaded ?: throw IllegalStateException("TreasureCatalog not initialized")

    fun init(context: Context) {
        if (loaded == null) {
            val rawJson = context.resources.openRawResource(R.raw.content).bufferedReader().use { it.readText() }
            val embedded = parse(rawJson) ?: error("Embedded content.json is invalid")
            loaded = embedded
            loadCached(context.filesDir)
        }
    }

    fun loadCached(filesDir: File) {
        try {
            val file = File(filesDir, "content.json")
            if (file.exists()) {
                val cached = parse(file.readText())
                val current = loaded
                if (cached != null && (current == null || cached.revision > current.revision)) {
                    loaded = cached
                }
            }
        } catch (_: Exception) {
        }
    }

    fun refreshBlocking(filesDir: File): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        var updatedAny = false
        for (url in listOf(AppInfo.CONTENT_URL, AppInfo.UPSTREAM_CONTENT_URL)) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ChestCounter/${AppInfo.VERSION_NAME} (+${AppInfo.REPO_URL})")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body.string()
                    if (body.length > 512 * 1024) return@use
                    val fresh = parse(body) ?: return@use
                    val current = loaded
                    if (current != null && fresh.revision <= current.revision) return@use
                    val target = File(filesDir, "content.json")
                    val tmp = File(filesDir, "content.json.tmp")
                    tmp.writeText(body)
                    tmp.renameTo(target)
                    loaded = fresh
                    updatedAny = true
                }
            } catch (_: Exception) {
            }
        }
        return updatedAny
    }

    fun parse(jsonText: String): CatalogData? {
        return try {
            val root = Json.parseToJsonElement(jsonText).jsonObject
            if (root["schema"]?.jsonPrimitive?.intOrNull != 1) return null
            val revision = root["revision"]?.jsonPrimitive?.intOrNull ?: return null
            if (revision < 1) return null
            val updated = root["updated"]?.jsonPrimitive?.content ?: ""

            fun parseKind(v: String?): String =
                if (v != null && v in allowedKinds) v else throw IllegalArgumentException("kind")

            fun parsePrize(obj: JsonObject): TreasurePrize {
                val id = obj["id"]?.jsonPrimitive?.content ?: ""
                require(safeId.matches(id)) { "prize id" }
                val kind = parseKind(obj["kind"]?.jsonPrimitive?.content)
                val name = obj["name"]?.jsonPrimitive?.content?.takeIf { it.length <= 80 } ?: throw IllegalArgumentException("name")
                val image = obj["image"]?.jsonPrimitive?.content?.also {
                    require(it.length <= 300 && it.startsWith("https://")) { "image" }
                }
                val ids = obj["prizeIds"]?.jsonArray?.take(6)?.mapNotNull {
                    val s = it.jsonPrimitive.content
                    require(safePrizeId.matches(s)) { "prizeIds" }
                    s
                } ?: emptyList()
                return TreasurePrize(id, kind, name, image, ids)
            }

            val odds = root["odds"]?.jsonArray?.take(20)?.map { el ->
                val o = el.jsonObject
                val oneIn = o["oneIn"]?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("oneIn")
                require(oneIn in 1..100_000_000) { "oneIn range" }
                val kind = parseKind(o["kind"]?.jsonPrimitive?.content)
                val weekly = o["weekly"]?.jsonPrimitive?.booleanOrNull ?: false
                TreasureOdds(kind, oneIn, weekly)
            } ?: return null
            if (allowedKinds.any { k -> odds.none { it.kind == k } }) return null

            val weeks = root["weeks"]?.jsonArray?.take(30)?.map { el ->
                val w = el.jsonObject
                val number = w["number"]?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("number")
                val from = Instant.parse(w["start"]!!.jsonPrimitive.content)
                val to = Instant.parse(w["end"]!!.jsonPrimitive.content)
                require(to.isAfter(from)) { "week dates" }
                val title = w["title"]?.jsonPrimitive?.content?.takeIf { it.length <= 80 } ?: ""
                val prizes = w["prizes"]?.jsonArray?.take(20)?.map { parsePrize(it.jsonObject) } ?: emptyList()
                TreasureWeek(number, from, to, title, prizes)
            } ?: return null

            val hours = root["treasureHours"]?.jsonArray?.take(300)?.map { el ->
                val h = el.jsonObject
                val start = Instant.parse(h["start"]!!.jsonPrimitive.content)
                val end = Instant.parse(h["end"]!!.jsonPrimitive.content)
                val dur = Duration.between(start, end)
                require(!dur.isNegative && !dur.isZero && dur <= Duration.ofHours(24)) { "hour dates" }
                val boosts = h["boosts"]!!.jsonObject.entries.associate { (k, v) ->
                    parseKind(k)
                    val b = v.jsonPrimitive.doubleOrNull ?: throw IllegalArgumentException("boost")
                    require(b in 1.0..100.0) { "boost range" }
                    k to b
                }
                val confirmed = h["confirmed"]?.jsonPrimitive?.booleanOrNull ?: false
                TreasureHour(start, end, boosts, confirmed)
            } ?: emptyList()

            fun parseLangMap(obj: JsonObject?, max: Int): Map<String, String> {
                if (obj == null) return emptyMap()
                return obj.entries.filter { it.key in listOf("fr", "en", "es") }
                    .associate { it.key to (it.value.jsonPrimitive.content.take(max)) }
            }

            val notices = root["notices"]?.jsonArray?.take(10)?.map { el ->
                val n = el.jsonObject
                val id = n["id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("notice id")
                require(safeId.matches(id)) { "notice id" }
                val from = Instant.parse(n["from"]!!.jsonPrimitive.content)
                val to = Instant.parse(n["to"]!!.jsonPrimitive.content)
                val level = n["level"]?.jsonPrimitive?.content?.takeIf { it == "info" || it == "warning" } ?: "info"
                val text = parseLangMap(n["text"]?.jsonObject, 600)
                val title = parseLangMap(n["title"]?.jsonObject, 100)
                val link = n["link"]?.jsonPrimitive?.content?.takeIf { it.length <= 300 && it.startsWith("https://") }
                val minV = n["minVersion"]?.jsonPrimitive?.content
                val maxV = n["maxVersion"]?.jsonPrimitive?.content
                Notice(id, from, to, level, text, title, link, minV, maxV)
            } ?: emptyList()

            val pirate = parsePrize(root["pirateShop"]!!.jsonObject)
            require(pirate.kind == PIRATE_SHOP) { "pirateShop kind" }

            val sources = root["sources"]?.jsonObject
            val oddsSource = sources?.get("odds")?.jsonPrimitive?.content?.takeIf { it.startsWith("https://") } ?: DEFAULT_ODDS_SOURCE
            val hoursSource = sources?.get("treasureHours")?.jsonPrimitive?.content?.takeIf { it.startsWith("https://") } ?: DEFAULT_HOURS_SOURCE

            CatalogData(revision, updated, odds, pirate, weeks, hours, notices, oddsSource, hoursSource)
        } catch (_: Exception) {
            null
        }
    }

    fun activeNotices(utc: Instant = Instant.now()): List<Notice> =
        data.notices.filter { !utc.isBefore(it.fromUtc) && utc.isBefore(it.toUtc) && it.concerns(AppInfo.VERSION_NAME) }

    fun find(prizeId: String): TreasurePrize? =
        (listOf(data.pirateShopAccess) + data.weeks.flatMap { it.prizes })
            .firstOrNull { p -> p.prizeIds.any { it.equals(prizeId, ignoreCase = true) } }

    fun boost(kind: String, utc: Instant): Double =
        data.treasureHours
            .firstOrNull { !utc.isBefore(it.startUtc) && utc.isBefore(it.endUtc) }
            ?.boosts?.get(kind)
            ?.takeIf { it > 0.0 } ?: 1.0

    fun currentTreasureHour(utc: Instant): TreasureHour? =
        data.treasureHours.firstOrNull { !utc.isBefore(it.startUtc) && utc.isBefore(it.endUtc) }

    fun nextTreasureHour(utc: Instant): TreasureHour? =
        data.treasureHours.filter { it.startUtc.isAfter(utc) }.minByOrNull { it.startUtc }

    fun weekOf(utc: Instant): TreasureWeek? =
        data.weeks.firstOrNull { !utc.isBefore(it.from) && utc.isBefore(it.to) }

    val huntStart: Instant
        get() = data.weeks.firstOrNull()?.from ?: Instant.EPOCH

    val huntEnd: Instant
        get() = data.weeks.lastOrNull()?.to ?: Instant.EPOCH

    fun shownWeek(utc: Instant): TreasureWeek? =
        weekOf(utc) ?: data.weeks.lastOrNull { !it.to.isAfter(utc) }

    fun kindName(kind: String, lang: String): String = when (kind) {
        GOLD_OUTFIT -> L.t(lang, "Tenue en or", "Gold Outfit", "Traje de oro")
        PIRATE_SHOP -> L.t(lang, "Accès à la boutique pirate", "Pirate Shop Access", "Acceso a la tienda pirata")
        VEHICLE_DECAL -> L.t(lang, "Véhicule et autocollant", "Vehicle & Decal", "Vehículo y adhesivo")
        PICKAXE_OR_BACK_BLING -> L.t(lang, "Pioche ou sac à dos", "Pickaxe or Back Bling", "Pico o mochila")
        JAM_TRACK -> L.t(lang, "Piste musicale", "Jam Track", "Pista musical")
        LOADING_SCREEN -> L.t(lang, "Écran de chargement", "Loading Screen", "Pantalla de carga")
        else -> L.t(lang, "Gizmo (n'importe lequel)", "Gizmo (any)", "Artilugio (cualquiera)")
    }

    fun atLeastOnce(oneIn: Int, chests: Long): Double =
        if (chests <= 0) 0.0 else -expm1(chests * log1p(-1.0 / oneIn))

    fun logNever(odds: TreasureOdds, chestTimesUtc: List<Instant>, plainChests: Long): Double {
        val p = 1.0 / odds.oneIn
        var sum = maxOf(0L, plainChests) * log1p(-p)
        for (t in chestTimesUtc) {
            sum += log1p(-min(0.999, p * boost(odds.kind, t)))
        }
        return sum
    }

    fun expected(odds: TreasureOdds, chestTimesUtc: List<Instant>, plainChests: Long): Double {
        val p = 1.0 / odds.oneIn
        var sum = maxOf(0L, plainChests) * p
        for (t in chestTimesUtc) {
            sum += min(0.999, p * boost(odds.kind, t))
        }
        return sum
    }

    fun chestsFor(oneIn: Int, chance: Double): Long =
        ceil(ln(1.0 - chance) / log1p(-1.0 / oneIn)).toLong()

    fun verdict(p: Double, lang: String): Pair<VerdictTier, String> {
        val pct = p * 100.0
        return when {
            pct >= 99.95 -> VerdictTier.CERTAIN to L.t(lang, "Presque sûr", "Almost certain", "Casi seguro")
            pct >= 95.0 -> VerdictTier.VLIKELY to L.t(lang, "Très probable", "Very likely", "Muy probable")
            pct >= 75.0 -> VerdictTier.LIKELY to L.t(lang, "Probable", "Likely", "Probable")
            pct >= 25.0 -> VerdictTier.POSSIBLE to L.t(lang, "Peut-être", "Maybe", "Quizás")
            pct >= 5.0 -> VerdictTier.UNLIKELY to L.t(lang, "Peu probable", "Unlikely", "Poco probable")
            else -> VerdictTier.VUNLIKELY to L.t(lang, "Très peu probable", "Very unlikely", "Muy poco probable")
        }
    }

    fun percentText(p: Double, lang: String): String {
        val v = p * 100.0
        if (v >= 99.95) {
            val most = L.f(lang, 99.9, 1)
            return if (lang == "en") ">$most%" else "> $most %"
        }
        val decimals = when {
            v >= 10.0 -> 1
            v <= 0.0 -> 0
            else -> (ceil(-log10(v)).toInt() + 1).coerceIn(1, 8)
        }
        val text = L.f(lang, v, decimals)
        return if (lang == "en") "$text%" else "$text %"
    }

    fun expectedText(average: Double, lang: String): String =
        if (average < 0.05) "< ${L.f(lang, 0.1, 1)}"
        else "≈ ${L.f(lang, average, if (average >= 10.0) 0 else 1)}"

    fun oneInHuge(logChance: Double, lang: String): String {
        val log10 = -logChance / ln(10.0)
        if (log10 < 9.0) {
            return L.nLong(lang, kotlin.math.exp(-logChance).roundToLong())
        }
        val exp = floor(log10).toInt()
        val mantissa = 10.0.pow(log10 - exp)
        val sup = "⁰¹²³⁴⁵⁶⁷⁸⁹"
        val expSup = exp.toString().map { sup[it - '0'] }.joinToString("")
        return "${L.f(lang, mantissa, 1)} × 10$expSup"
    }

    fun countdownText(target: Instant, now: Instant, lang: String): Pair<String, Boolean> {
        val ms = Duration.between(now, target).toMillis()
        if (ms <= 0) return L.t(lang, "maintenant", "now", "ahora") to false
        val units = L.t(lang, "j h min s", "d h m s", "d h m s").split(' ')
        val totalSec = ms / 1000
        val dd = totalSec / 86400
        val h = (totalSec % 86400) / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val text = when {
            dd > 0 -> "$dd${units[0]} $h${units[1]} $m${units[2]}"
            h > 0 -> "$h${units[1]} $m${units[2]} $s${units[3]}"
            else -> "$m${units[2]} $s${units[3]}"
        }
        val soon = ms < 86_400_000L
        return text to soon
    }

    fun nextRegularHours(now: Instant): List<Instant> {
        fun next(day: DayOfWeek, hour: Int): Instant {
            val zNow = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
            val diff = (day.value - zNow.dayOfWeek.value + 7) % 7
            val candidate = zNow.toLocalDate().plusDays(diff.toLong()).atTime(hour, 0).atZone(ZoneOffset.UTC).toInstant()
            return if (candidate.plusSeconds(7200).isAfter(now)) candidate else candidate.plusSeconds(7 * 86400L)
        }
        return listOf(next(DayOfWeek.SATURDAY, 19), next(DayOfWeek.SUNDAY, 2)).sorted()
    }

    @DrawableRes
    fun localDrawableFor(imageUrl: String?): Int? {
        if (imageUrl == null) return null
        val file = imageUrl.substringAfterLast('/')
        val namePart = file.substringBefore('?').substringBeforeLast('.')
        val query = file.substringAfter('?', "").filter { it.isLetterOrDigit() }
        val key = if (query.isNotEmpty()) "${namePart}_$query" else namePart
        return when (key.lowercase(Locale.ROOT).replace('-', '_')) {
            "token" -> R.drawable.treasure_token
            "w1_car" -> R.drawable.treasure_w1_car
            "w1_jam" -> R.drawable.treasure_w1_jam
            "w1_loading" -> R.drawable.treasure_w1_loading
            "w1_outfit" -> R.drawable.treasure_w1_outfit
            "w1_pickaxe" -> R.drawable.treasure_w1_pickaxe
            "w2_backbling" -> R.drawable.treasure_w2_backbling
            "w2_car" -> R.drawable.treasure_w2_car
            "w2_jam" -> R.drawable.treasure_w2_jam
            "w2_loading_v3" -> R.drawable.treasure_w2_loading_v3
            "w2_outfit" -> R.drawable.treasure_w2_outfit
            "w3_backbling" -> R.drawable.treasure_w3_backbling
            "w3_car_2" -> R.drawable.treasure_w3_car_2
            "w3_jamtrack_2" -> R.drawable.treasure_w3_jamtrack_2
            "w3_loading" -> R.drawable.treasure_w3_loading
            "w3_outfit" -> R.drawable.treasure_w3_outfit
            else -> null
        }
    }
}
