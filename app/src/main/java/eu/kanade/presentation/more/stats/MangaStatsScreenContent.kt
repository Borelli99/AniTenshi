package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.stats.components.StatsBigNumber
import eu.kanade.presentation.more.stats.components.StatsContent
import eu.kanade.presentation.more.stats.components.StatsLibraryPart
import eu.kanade.presentation.more.stats.components.statsDurationText
import eu.kanade.presentation.more.stats.components.statsNumber
import eu.kanade.presentation.more.stats.data.StatsData
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

@Composable
fun MangaStatsScreenContent(
    state: StatsScreenState.SuccessManga,
    paddingValues: PaddingValues,
) {
    val chapters = state.chapters
    val progress = state.progress
    val libraryCount = progress.caughtUp + progress.started + progress.notStarted
    val weekTotal = state.week.counts.sum()
    StatsContent(
        paddingValues = paddingValues,
        numbers = listOf(
            StatsBigNumber(
                icon = Icons.Outlined.CollectionsBookmark,
                value = statsNumber(state.overview.libraryMangaCount),
                label = stringResource(MR.strings.in_library),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.Schedule,
                value = statsDurationText(state.overview.totalReadDuration),
                label = stringResource(TLMR.strings.anitenshi_stats_read_time),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.AutoStories,
                value = statsNumber(chapters.readChapterCount),
                label = stringResource(TLMR.strings.anitenshi_stats_chapters_read),
                progress = if (chapters.totalChapterCount > 0) {
                    chapters.readChapterCount.toFloat() / chapters.totalChapterCount
                } else {
                    null
                },
                progressText = stringResource(TLMR.strings.anitenshi_stats_of, statsNumber(chapters.totalChapterCount)),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.CheckCircle,
                value = statsNumber(state.overview.completedMangaCount),
                label = stringResource(TLMR.strings.anitenshi_stats_completed),
            ),
        ),
        week = state.week,
        weekTotalText = pluralStringResource(TLMR.plurals.anitenshi_stats_chapters, weekTotal, statsNumber(weekTotal)),
        libraryParts = listOf(
            StatsLibraryPart(
                label = stringResource(TLMR.strings.anitenshi_stats_caught_up),
                hint = stringResource(TLMR.strings.anitenshi_stats_caught_up_manga),
                count = progress.caughtUp,
            ),
            StatsLibraryPart(stringResource(TLMR.strings.anitenshi_stats_started), null, progress.started),
            StatsLibraryPart(stringResource(TLMR.strings.anitenshi_stats_not_started), null, progress.notStarted),
        ),
        libraryTotalText = pluralStringResource(
            TLMR.plurals.anitenshi_stats_titles,
            libraryCount,
            statsNumber(libraryCount),
        ),
        otherRows = listOf(
            stringResource(MR.strings.label_titles_in_global_update) to
                statsNumber(state.titles.globalUpdateItemCount),
            stringResource(MR.strings.label_local) to statsNumber(state.titles.localMangaCount),
            stringResource(TLMR.strings.anitenshi_stats_chapters_downloaded) to statsNumber(chapters.downloadCount),
            stringResource(MR.strings.label_tracked_titles) to statsNumber(state.trackers.trackedTitleCount),
            stringResource(MR.strings.label_mean_score) to meanScoreText(state.trackers),
            stringResource(TLMR.strings.anitenshi_stats_trackers_used) to statsNumber(state.trackers.trackerCount),
        ),
    )
}

/** The mean score given on the trackers, out of 10, or "N/A" when no title has one. */
@Composable
internal fun meanScoreText(trackers: StatsData.Trackers): String {
    return if (trackers.trackedTitleCount > 0 && !trackers.meanScore.isNaN()) {
        "%.2f ★".format(Locale.getDefault(), trackers.meanScore)
    } else {
        stringResource(MR.strings.not_applicable)
    }
}
