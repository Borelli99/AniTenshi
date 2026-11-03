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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import java.time.OffsetDateTime

/**
 * AniTenshi: original (Japanese) air dates of an anime's episodes, from AniList and from MyAnimeList
 * (through the public Jikan API). Used only for episodes whose extension gives no release date.
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

    private suspend fun loadAnilist(anilistId: Long?, malId: Long?, nowMillis: Long): AnilistAirDates {
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
            val response = client.newCall(POST(ANILIST_URL, body = body)).execute()
            val text = response.use { it.body.string() }
            val parsed = parseAnilistAirDates(text, nowMillis)
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
                airingSchedule(page: ${'$'}page, perPage: 50) {
                  pageInfo { hasNextPage }
                  nodes { episode airingAt }
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

private val airDatesJson = Json { ignoreUnknownKeys = true }

/** Reads an AniList `Media { idMal airingSchedule { … } }` response; only episodes already aired by [nowMillis]. */
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
    }.toMap()
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
        val millis = runCatching { OffsetDateTime.parse(aired).toInstant().toEpochMilli() }.getOrNull()
            ?.takeIf { it in 1..nowMillis }
            ?: return@mapNotNull null
        episode to millis
    }.toMap()
    val hasNextPage = root["pagination"]?.asObject()?.primitive("has_next_page")?.booleanOrNull == true
    return dates to hasNextPage
}

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive
