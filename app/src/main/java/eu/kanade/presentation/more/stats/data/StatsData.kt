package eu.kanade.presentation.more.stats.data

import java.time.LocalDate

sealed interface StatsData {

    data class MangaOverview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData

    data class AnimeOverview(
        val libraryAnimeCount: Int,
        val completedAnimeCount: Int,
        val totalSeenDuration: Long,
    ) : StatsData

    data class MangaTitles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class AnimeTitles(
        val globalUpdateItemCount: Int,
        val startedAnimeCount: Int,
        val localAnimeCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Episodes(
        val totalEpisodeCount: Int,
        val readEpisodeCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData

    /**
     * AniTenshi: how many episodes or chapters were last watched or read on each of the last 7 days, the oldest
     * first and today last.
     */
    data class Week(
        val firstDay: LocalDate,
        val counts: List<Int>,
    ) : StatsData

    /**
     * AniTenshi: the titles of the library by how far you've got: all that is out watched or read ([caughtUp]),
     * started, or not started yet.
     */
    data class LibraryProgress(
        val caughtUp: Int,
        val started: Int,
        val notStarted: Int,
    ) : StatsData
}
