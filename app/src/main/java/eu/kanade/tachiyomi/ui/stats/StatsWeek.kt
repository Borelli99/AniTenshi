package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * AniTenshi: the last 7 days, today last, with how many of the times [load] gives (epoch milliseconds, from the
 * start of the first day on) fall on each.
 */
internal suspend fun lastWeek(load: suspend (since: Long) -> List<Long>): StatsData.Week {
    val zone = ZoneId.systemDefault()
    val firstDay = LocalDate.now(zone).minusDays(6)
    val counts = IntArray(7)
    load(firstDay.atStartOfDay(zone).toInstant().toEpochMilli()).forEach { time ->
        val day = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
        val index = ChronoUnit.DAYS.between(firstDay, day).toInt()
        if (index in counts.indices) counts[index]++
    }
    return StatsData.Week(firstDay = firstDay, counts = counts.toList())
}
