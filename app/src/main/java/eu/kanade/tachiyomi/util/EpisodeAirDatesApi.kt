package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * AniTenshi: original (Japanese) air dates of an anime's episodes, from AniList and from MyAnimeList
 * (through the public Jikan API). Used only for episodes whose extension gives no release date.
 * Only public, read-only data: no account or login is needed, and nothing is written to any list.
 */
class EpisodeAirDatesApi(
    private val client: OkHttpClient = OkHttpClient(),
) {

    /**
     * Air dates (epoch ms) by episode number for the anime with these AniList / MyAnimeList ids.
     * AniList has exact times for recent shows, MyAnimeList fills the gaps (older shows). Empty when
     * neither site knows the dates or they can't be reached.
     */
    suspend fun load(anilistId: Long?, malId: Long?): Map<Int, Long> = withIOContext {
        val nowMillis = System.currentTimeMillis()
        val dates = mutableMapOf<Int, Long>()
        var mal = malId?.takeIf { it > 0 }
        val anilist = anilistId?.takeIf { it > 0 }

        if (anilist != null || mal != null) {
            runCatching { loadAnilist(anilist, mal, nowMillis) }.getOrNull()?.let { result ->
                dates.putAll(result.dates)
                if (mal == null) mal = result.malId
            }
        }
        mal?.let { id ->
            runCatching { loadJikan(id, nowMillis) }.getOrNull()?.forEach { (episode, date) ->
                dates.putIfAbsent(episode, date)
            }
        }
        dates
    }

    /**
     * Finds the AniList entry of an anime from its titles on the source, for anime not linked in Tracking.
     * Returns null unless one entry clearly matches (see [pickAnilistMatch]), so a doubtful match gives
     * no date rather than a wrong one. Throws when AniList can't be reached.
     */
    suspend fun findAnime(titles: List<String>, episodeCount: Int): AnilistCandidate? = withIOContext {
        titles.map(::cleanSourceTitle)
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeTitle)
            .take(2)
            .forEach { title ->
                val body = buildJsonObject {
                    put("query", ANILIST_SEARCH_QUERY)
                    put("variables", buildJsonObject { put("search", title) })
                }.toString().toRequestBody(jsonMime)
                val candidates = parseAnilistSearch(postAnilist(body))
                pickAnilistMatch(title, episodeCount, candidates)?.let { return@withIOContext it }
            }
        null
    }

    private fun postAnilist(body: RequestBody): String =
        client.newCall(POST(ANILIST_URL, body = body)).execute().use { it.body.string() }

    private fun loadAnilist(anilistId: Long?, malId: Long?, nowMillis: Long): AnilistAirDates {
        val dates = mutableMapOf<Int, Long>()
        var foundMalId: Long? = null
        for (page in 1..MAX_ANILIST_PAGES) {
            val body = buildJsonObject {
                put("query", ANILIST_QUERY)
                put(
                    "variables",
                    buildJsonObject {
                        if (anilistId != null) put("id", anilistId) else put("idMal", malId)
                        put("page", page)
                    },
                )
            }.toString().toRequestBody(jsonMime)
            val parsed = parseAnilistAirDates(postAnilist(body), nowMillis)
            dates.putAll(parsed.dates)
            foundMalId = foundMalId ?: parsed.malId
            if (!parsed.hasNextPage) break
        }
        return AnilistAirDates(dates, foundMalId, hasNextPage = false)
    }

    private suspend fun loadJikan(malId: Long, nowMillis: Long): Map<Int, Long> {
        val dates = mutableMapOf<Int, Long>()
        for (page in 1..MAX_JIKAN_PAGES) {
            if (page > 1) delay(JIKAN_PAGE_DELAY_MS) // Jikan allows about 3 requests per second
            val response = client.newCall(GET("$JIKAN_URL/anime/$malId/episodes?page=$page")).execute()
            if (!response.isSuccessful) {
                response.close()
                break
            }
            val parsed = parseJikanAirDates(response.use { it.body.string() }, nowMillis)
            dates.putAll(parsed.first)
            if (!parsed.second) break
        }
        return dates
    }

    companion object {
        private const val ANILIST_URL = "https://graphql.anilist.co"
        private const val JIKAN_URL = "https://api.jikan.moe/v4"
        private const val MAX_ANILIST_PAGES = 30 // 50 episodes each
        private const val MAX_JIKAN_PAGES = 15 // 100 episodes each
        private const val JIKAN_PAGE_DELAY_MS = 400L

        private val ANILIST_QUERY = """
            query (${'$'}id: Int, ${'$'}idMal: Int, ${'$'}page: Int) {
              Media(id: ${'$'}id, idMal: ${'$'}idMal, type: ANIME) {
                idMal
                episodes
                startDate { year month day }
                airingSchedule(page: ${'$'}page, perPage: 50) {
                  pageInfo { hasNextPage }
                  nodes { episode airingAt }
                }
              }
            }
        """.trimIndent()

        private val ANILIST_SEARCH_QUERY = """
            query (${'$'}search: String) {
              Page(perPage: 10) {
                media(search: ${'$'}search, type: ANIME, sort: SEARCH_MATCH) {
                  id
                  idMal
                  episodes
                  title { romaji english native }
                  synonyms
                }
              }
            }
        """.trimIndent()
    }
}

internal data class AnilistAirDates(
    val dates: Map<Int, Long>,
    val malId: Long?,
    val hasNextPage: Boolean,
)

/** An AniList search result: its ids, episode count (null while unknown) and every known title. */
data class AnilistCandidate(
    val id: Long,
    val malId: Long?,
    val episodes: Int?,
    val titles: List<String>,
)

/** Lowest title likeness ([titleSimilarity]) accepted for an AniList search result. */
private const val MIN_TITLE_SIMILARITY = 0.8

private val airDatesJson = Json { ignoreUnknownKeys = true }

/**
 * What sites add to a title for the audio or subtitle version ("Dub ITA", "Sub ITA", "(ITA)", "[Dub]"…),
 * matched at the end of the title.
 */
private val VERSION_TAG = Regex(
    """[\s\-–:|]*[(\[]?\s*(?:\b(?:sub|dub|dubbed|subbed)\s*)?\b(?:ita|eng|italiano|english)\b\s*[)\]]?\s*$""" +
        """|[\s\-–:|]*[(\[]?\s*\b(?:sub|dub|dubbed|subbed|uncensored)\b\s*[)\]]?\s*$""",
    RegexOption.IGNORE_CASE,
)

private val DUB_WORD = Regex("""\b(?:dub|dubbed|doppiato|doppiaggio)\b""", RegexOption.IGNORE_CASE)

/** True for a dubbed version ("Dub ITA"…): it airs on its own schedule, later than in Japan. */
fun isDubTitle(title: String): Boolean = DUB_WORD.containsMatchIn(title)

/** The title without version tags at its end, e.g. "Io sono Teppei! Dub ITA" → "Io sono Teppei!". */
internal fun cleanSourceTitle(title: String): String {
    var current = title.trim()
    while (true) {
        val next = VERSION_TAG.replace(current, "").trim()
        if (next == current || next.isEmpty()) return current
        current = next
    }
}

/** Lowercase words without accents and punctuation, for comparing titles. */
internal fun normalizeTitle(title: String): String =
    Normalizer.normalize(title, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

/** Share of words two titles have in common (1.0 = same words). */
internal fun titleSimilarity(a: String, b: String): Double {
    val wordsA = normalizeTitle(a).split(' ').filter { it.isNotEmpty() }.toSet()
    val wordsB = normalizeTitle(b).split(' ').filter { it.isNotEmpty() }.toSet()
    if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
    return (wordsA intersect wordsB).size.toDouble() / (wordsA union wordsB).size
}

/**
 * The search result that is clearly the source's anime: a title close enough to [sourceTitle], and no fewer
 * episodes than the source has. When two results are equally close (e.g. a remake with the same name), only
 * the one with the same episode count is taken; otherwise none.
 */
internal fun pickAnilistMatch(
    sourceTitle: String,
    episodeCount: Int,
    candidates: List<AnilistCandidate>,
): AnilistCandidate? {
    val scored = candidates
        .filter { it.episodes == null || episodeCount <= 0 || episodeCount <= it.episodes }
        .map { candidate -> candidate to (candidate.titles.maxOfOrNull { titleSimilarity(sourceTitle, it) } ?: 0.0) }
        .filter { it.second >= MIN_TITLE_SIMILARITY }
        .sortedByDescending { it.second }
    val best = scored.firstOrNull() ?: return null
    val tied = scored.filter { it.second >= best.second - 1e-9 }.map { it.first }
    return tied.singleOrNull() ?: tied.singleOrNull { it.episodes == episodeCount }
}

/** Reads an AniList `Page { media { … } }` search response. */
internal fun parseAnilistSearch(body: String): List<AnilistCandidate> {
    val media = runCatching {
        airDatesJson.parseToJsonElement(body).jsonObject["data"]?.asObject()
            ?.get("Page")?.asObject()?.get("media") as? JsonArray
    }.getOrNull() ?: return emptyList()
    return media.mapNotNull { element ->
        val obj = element.asObject() ?: return@mapNotNull null
        val id = obj.primitive("id")?.longOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
        val title = obj["title"]?.asObject()
        val titles = listOfNotNull(
            title?.primitive("romaji")?.contentOrNull,
            title?.primitive("english")?.contentOrNull,
            title?.primitive("native")?.contentOrNull,
        ) + (obj["synonyms"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        AnilistCandidate(
            id = id,
            malId = obj.primitive("idMal")?.longOrNull?.takeIf { it > 0 },
            episodes = obj.primitive("episodes")?.intOrNull?.takeIf { it > 0 },
            titles = titles.filter { it.isNotBlank() },
        )
    }
}

/**
 * Reads an AniList `Media { idMal episodes startDate airingSchedule { … } }` response; only episodes already
 * aired by [nowMillis]. A single-episode entry (a film, an OVA) gets its release date when it has no schedule.
 */
internal fun parseAnilistAirDates(body: String, nowMillis: Long): AnilistAirDates {
    val media = runCatching {
        airDatesJson.parseToJsonElement(body).jsonObject["data"]?.asObject()?.get("Media")?.asObject()
    }.getOrNull() ?: return AnilistAirDates(emptyMap(), null, hasNextPage = false)

    val schedule = media["airingSchedule"]?.asObject()
    val dates = (schedule?.get("nodes") as? JsonArray).orEmpty().mapNotNull { node ->
        val obj = node.asObject() ?: return@mapNotNull null
        val episode = obj.primitive("episode")?.longOrNull?.toInt()?.takeIf { it > 0 } ?: return@mapNotNull null
        val airingAt = obj.primitive("airingAt")?.longOrNull?.times(1000)?.takeIf { it in 1..nowMillis }
            ?: return@mapNotNull null
        episode to airingAt
    }.toMap().toMutableMap()

    if (media.primitive("episodes")?.intOrNull == 1 && 1 !in dates) {
        val start = media["startDate"]?.asObject()
        val year = start?.primitive("year")?.intOrNull
        val month = start?.primitive("month")?.intOrNull
        val day = start?.primitive("day")?.intOrNull
        if (year != null && month != null && day != null) {
            runCatching { LocalDate.of(year, month, day) }.getOrNull()
                ?.let(::dayAtNoonUtc)
                ?.takeIf { it in 1..nowMillis }
                ?.let { dates[1] = it }
        }
    }

    val hasNextPage = schedule?.get("pageInfo")?.asObject()?.primitive("hasNextPage")?.booleanOrNull == true
    val malId = media.primitive("idMal")?.longOrNull?.takeIf { it > 0 }
    return AnilistAirDates(dates, malId, hasNextPage)
}

/** Reads a Jikan `/anime/{id}/episodes` page: air dates by episode number, and whether there is a next page. */
internal fun parseJikanAirDates(body: String, nowMillis: Long): Pair<Map<Int, Long>, Boolean> {
    val root = runCatching { airDatesJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return emptyMap<Int, Long>() to false
    val dates = (root["data"] as? JsonArray).orEmpty().mapNotNull { item ->
        val obj = item.asObject() ?: return@mapNotNull null
        val episode = obj.primitive("mal_id")?.doubleOrNull?.toInt()?.takeIf { it > 0 } ?: return@mapNotNull null
        val aired = obj.primitive("aired")?.contentOrNull ?: return@mapNotNull null
        // MyAnimeList gives the day only (at midnight UTC)
        val millis = runCatching { OffsetDateTime.parse(aired).toLocalDate() }.getOrNull()
            ?.let(::dayAtNoonUtc)
            ?.takeIf { it in 1..nowMillis }
            ?: return@mapNotNull null
        episode to millis
    }.toMap()
    val hasNextPage = root["pagination"]?.asObject()?.primitive("has_next_page")?.booleanOrNull == true
    return dates to hasNextPage
}

/** A day without a time, as noon UTC: the same calendar day in every time zone from UTC-12 to UTC+12. */
private fun dayAtNoonUtc(day: LocalDate): Long =
    day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC).toEpochMilli()

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive
