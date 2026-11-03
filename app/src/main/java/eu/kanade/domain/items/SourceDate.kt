package eu.kanade.domain.items

import kotlin.math.abs

/** A release date this close (ms) to the moment of a refresh is taken as made up by the source. */
private const val MADE_UP_DATE_WINDOW_MS = 10 * 60_000L

/**
 * AniTenshi: the release date to save for an episode or chapter the app already had, from the date its source
 * sends now. 0 means "keep the stored date".
 *
 * Some sources send the current time instead of the real release date, which would move every item to "Now"
 * at each refresh. An item the app already knew cannot have come out just now, so such a date is ignored.
 * If an earlier refresh already stored a made-up date (later than when the item was found), it goes back to
 * the moment the item was found, which the episode and chapter lists hide.
 */
fun sanitizedSourceDate(
    sourceDate: Long,
    storedDate: Long,
    storedFetch: Long,
    nowMillis: Long,
): Long = when {
    sourceDate <= 0L -> 0L
    abs(nowMillis - sourceDate) >= MADE_UP_DATE_WINDOW_MS -> sourceDate
    storedFetch > 0L && storedDate > storedFetch + MADE_UP_DATE_WINDOW_MS -> storedFetch
    else -> 0L
}
