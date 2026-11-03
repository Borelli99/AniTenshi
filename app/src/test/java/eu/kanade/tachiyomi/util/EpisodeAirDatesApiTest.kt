package eu.kanade.tachiyomi.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class EpisodeAirDatesApiTest {

    private val now = OffsetDateTime.parse("2026-11-03T10:00:00+01:00").toInstant().toEpochMilli()

    private fun noonUtc(day: String) = OffsetDateTime.parse("${day}T12:00:00Z").toInstant().toEpochMilli()

    @Test
    fun anilistScheduleKeepsAiredEpisodesAndMalId() {
        val body = """
            {"data":{"Media":{"idMal":52991,"episodes":28,"startDate":{"year":2023,"month":9,"day":29},
            "airingSchedule":{"pageInfo":{"hasNextPage":true},"nodes":[
              {"episode":1,"airingAt":1696003200},
              {"episode":2,"airingAt":1696608000},
              {"episode":40,"airingAt":4102444800},
              {"episode":null,"airingAt":1696608000}
            ]}}}}
        """.trimIndent()

        val result = parseAnilistAirDates(body, now)

        assertEquals(mapOf(1 to 1_696_003_200_000L, 2 to 1_696_608_000_000L), result.dates)
        assertEquals(52991L, result.malId)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun anilistFilmGetsItsReleaseDay() {
        val body = """
            {"data":{"Media":{"idMal":32950,"episodes":1,"startDate":{"year":2016,"month":9,"day":3},
            "airingSchedule":{"pageInfo":{"hasNextPage":false},"nodes":[]}}}}
        """.trimIndent()

        assertEquals(mapOf(1 to noonUtc("2016-09-03")), parseAnilistAirDates(body, now).dates)
    }

    @Test
    fun anilistNotFoundGivesNothing() {
        val body = """{"errors":[{"message":"Not Found.","status":404}],"data":{"Media":null}}"""

        val result = parseAnilistAirDates(body, now)

        assertTrue(result.dates.isEmpty())
        assertNull(result.malId)
        assertFalse(result.hasNextPage)
    }

    @Test
    fun jikanEpisodesWithDates() {
        val body = """
            {"pagination":{"last_visible_page":1,"has_next_page":false},"data":[
              {"mal_id":1,"title":"Episode 1","aired":"1977-09-12T00:00:00+00:00","filler":false,"recap":false},
              {"mal_id":2,"title":"Episode 2","aired":"1977-09-19T00:00:00+00:00","filler":false,"recap":false},
              {"mal_id":3,"title":"Episode 3","aired":null,"filler":false,"recap":false}
            ]}
        """.trimIndent()

        val (dates, hasNextPage) = parseJikanAirDates(body, now)

        assertEquals(mapOf(1 to noonUtc("1977-09-12"), 2 to noonUtc("1977-09-19")), dates)
        assertFalse(hasNextPage)
    }

    @Test
    fun jikanErrorGivesNothing() {
        val (dates, hasNextPage) = parseJikanAirDates("""{"status":429,"message":"Too Many Requests"}""", now)

        assertTrue(dates.isEmpty())
        assertFalse(hasNextPage)
    }

    @Test
    fun versionTagsAreRemovedFromTitles() {
        assertEquals("Io sono Teppei!", cleanSourceTitle("Io sono Teppei! Dub ITA"))
        assertEquals("One Piece", cleanSourceTitle("One Piece Sub ITA"))
        assertEquals("Naruto", cleanSourceTitle("Naruto (ITA)"))
        assertEquals("Dragon Ball Super", cleanSourceTitle("Dragon Ball Super [Dub]"))
        assertEquals("Kami-sama Hajimemashita", cleanSourceTitle("Kami-sama Hajimemashita"))
        assertEquals("ITA", cleanSourceTitle("ITA"))
    }

    @Test
    fun searchResultsAreRead() {
        val body = """
            {"data":{"Page":{"media":[
              {"id":16498,"idMal":16498,"episodes":25,
               "title":{"romaji":"Shingeki no Kyojin","english":"Attack on Titan","native":"進撃の巨人"},
               "synonyms":["AoT","SnK"]}
            ]}}}
        """.trimIndent()

        val result = parseAnilistSearch(body).single()

        assertEquals(16498L, result.id)
        assertEquals(16498L, result.malId)
        assertEquals(25, result.episodes)
        assertEquals(listOf("Shingeki no Kyojin", "Attack on Titan", "進撃の巨人", "AoT", "SnK"), result.titles)
    }

    @Test
    fun matchNeedsCloseTitleAndCompatibleEpisodes() {
        val first = AnilistCandidate(16498, 16498, 25, listOf("Shingeki no Kyojin", "Attack on Titan"))
        val second = AnilistCandidate(20958, 25777, 12, listOf("Shingeki no Kyojin Season 2", "Attack on Titan Season 2"))

        assertEquals(first, pickAnilistMatch(listOf("Shingeki no Kyojin"), 25, listOf(second, first)))
        assertEquals(second, pickAnilistMatch(listOf("Attack on Titan Season 2"), 12, listOf(first, second)))
        // More episodes on the source than the entry has: not the same anime
        assertNull(pickAnilistMatch(listOf("Shingeki no Kyojin"), 30, listOf(first)))
        // Title too different
        assertNull(pickAnilistMatch(listOf("Io sono Teppei!"), 28, listOf(first, second)))
    }

    @Test
    fun sameTitleTwiceNeedsSameEpisodeCount() {
        val old = AnilistCandidate(136, 136, 62, listOf("Hunter x Hunter"))
        val remake = AnilistCandidate(11061, 11061, 148, listOf("Hunter x Hunter (2011)", "Hunter x Hunter"))

        assertEquals(remake, pickAnilistMatch(listOf("Hunter x Hunter"), 148, listOf(old, remake)))
        // Both could fit 40 episodes: no guess
        assertNull(pickAnilistMatch(listOf("Hunter x Hunter"), 40, listOf(old, remake)))
    }

    @Test
    fun seasonNumbersAreWrittenTheSameWay() {
        assertEquals(titleWords("Black Clover 2"), titleWords("Black Clover Season 2"))
        assertEquals(titleWords("Black Clover 2"), titleWords("Black Clover 2nd Season"))
        assertEquals(titleWords("Black Clover 2"), titleWords("Black Clover S2"))
        assertEquals(titleWords("Black Clover 2"), titleWords("Black Clover II"))
    }

    @Test
    fun secondSeasonIsNotTheFirst() {
        val first = AnilistCandidate(97940, 34572, 170, listOf("Black Clover"))
        val second = AnilistCandidate(190000, 59000, null, listOf("Black Clover Season 2", "Black Clover 2nd Season"))

        assertEquals(second, pickAnilistMatch(listOf("Black Clover 2"), 5, listOf(first, second)))
        assertEquals(first, pickAnilistMatch(listOf("Black Clover"), 170, listOf(first, second)))
    }

    @Test
    fun alternativeTitlesAreReadFromTheDescription() {
        val description = """
            Seguito di Tokyo Revengers: Tenjiku Arc

            Titolo Alternativo: Tokyo Revengers: Santen Sensou-hen
        """.trimIndent()

        assertEquals(listOf("Tokyo Revengers: Santen Sensou-hen"), alternativeTitles(description))
        assertEquals(listOf("Koori no Jouheki 2"), alternativeTitles("Seconda stagione\ntitolo alternativo: Koori no Jouheki 2"))
        assertTrue(alternativeTitles("Satoko appartiene ad una famiglia nobile.").isEmpty())
        assertTrue(alternativeTitles(null).isEmpty())
    }

    @Test
    fun alternativeTitleFindsTheAnimeWhenTheSiteTitleDiffers() {
        val entry = AnilistCandidate(
            200000,
            60000,
            null,
            listOf("Tokyo Revengers: Santen Sensou-hen", "Tokyo Revengers: Three Titans War Arc"),
        )
        val siteTitle = "Tokyo Revengers: War of the Three Titan Arc"

        assertNull(pickAnilistMatch(listOf(siteTitle), 5, listOf(entry)))
        assertEquals(entry, pickAnilistMatch(listOf("Tokyo Revengers: Santen Sensou-hen", siteTitle), 5, listOf(entry)))
    }
}
