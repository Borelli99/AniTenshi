package eu.kanade.domain.items

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SourceDateTest {

    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun dateFoundAtFetchTimeIsUnknown() {
        assertTrue(isUnknownUploadDate(dateUpload = 0L, dateFetch = now))
        assertTrue(isUnknownUploadDate(dateUpload = now, dateFetch = now + 27))
        assertFalse(isUnknownUploadDate(dateUpload = now - 3 * day, dateFetch = now))
    }

    @Test
    fun realSourceDateIsKept() {
        assertEquals(now - 3 * day, sanitizedSourceDate(now - 3 * day, storedDate = 0L, storedFetch = now - day, now))
    }

    @Test
    fun currentTimeFromSourceKeepsStoredDate() {
        // 0 = keep what is stored (e.g. an air date from AniList)
        assertEquals(0L, sanitizedSourceDate(now - 5_000, storedDate = now - 400 * day, storedFetch = now - day, now))
        assertEquals(0L, sanitizedSourceDate(0L, storedDate = now - 400 * day, storedFetch = now - day, now))
    }

    @Test
    fun earlierMadeUpDateGoesBackToFetchTime() {
        val fetch = now - 10 * day
        assertEquals(fetch, sanitizedSourceDate(now - 5_000, storedDate = now - day, storedFetch = fetch, now))
    }
}
