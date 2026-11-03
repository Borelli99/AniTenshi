package eu.kanade.tachiyomi.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class EpisodeAirDatesApiTest {

    private val now = OffsetDateTime.parse("2026-11-03T10:00:00+01:00").toInstant().toEpochMilli()

    @Test
    fun anilistScheduleKeepsAiredEpisodesAndMalId() {
        val body = """
            {"data":{"Media":{"idMal":52991,"airingSchedule":{"pageInfo":{"hasNextPage":true},"nodes":[
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

        assertEquals(
            mapOf(
                1 to OffsetDateTime.parse("1977-09-12T00:00:00Z").toInstant().toEpochMilli(),
                2 to OffsetDateTime.parse("1977-09-19T00:00:00Z").toInstant().toEpochMilli(),
            ),
            dates,
        )
        assertFalse(hasNextPage)
    }

    @Test
    fun jikanErrorGivesNothing() {
        val (dates, hasNextPage) = parseJikanAirDates("""{"status":429,"message":"Too Many Requests"}""", now)

        assertTrue(dates.isEmpty())
        assertFalse(hasNextPage)
    }
}
