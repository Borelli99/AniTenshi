package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.stats.components.StatsBigNumber
import eu.kanade.presentation.more.stats.components.StatsContent
import eu.kanade.presentation.more.stats.components.StatsLibraryPart
import eu.kanade.presentation.more.stats.components.statsDurationText
import eu.kanade.presentation.more.stats.components.statsNumber
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun AnimeStatsScreenContent(
    state: StatsScreenState.SuccessAnime,
    paddingValues: PaddingValues,
) {
    val episodes = state.episodes
    val progress = state.progress
    val libraryCount = progress.caughtUp + progress.started + progress.notStarted
    val weekTotal = state.week.counts.sum()
    StatsContent(
        paddingValues = paddingValues,
        numbers = listOf(
            StatsBigNumber(
                icon = Icons.Outlined.CollectionsBookmark,
                value = statsNumber(state.overview.libraryAnimeCount),
                label = stringResource(MR.strings.in_library),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.Schedule,
                value = statsDurationText(state.overview.totalSeenDuration),
                label = stringResource(AYMR.strings.label_watched_duration),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.PlayCircle,
                value = statsNumber(episodes.readEpisodeCount),
                label = stringResource(TLMR.strings.anitenshi_stats_episodes_watched),
                progress = if (episodes.totalEpisodeCount > 0) {
                    episodes.readEpisodeCount.toFloat() / episodes.totalEpisodeCount
                } else {
                    null
                },
                progressText = stringResource(TLMR.strings.anitenshi_stats_of, statsNumber(episodes.totalEpisodeCount)),
            ),
            StatsBigNumber(
                icon = Icons.Outlined.CheckCircle,
                value = statsNumber(state.overview.completedAnimeCount),
                label = stringResource(TLMR.strings.anitenshi_stats_completed),
            ),
        ),
        week = state.week,
        weekTotalText = pluralStringResource(TLMR.plurals.anitenshi_stats_episodes, weekTotal, statsNumber(weekTotal)),
        libraryParts = listOf(
            StatsLibraryPart(
                label = stringResource(TLMR.strings.anitenshi_stats_caught_up),
                hint = stringResource(TLMR.strings.anitenshi_stats_caught_up_anime),
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
            stringResource(MR.strings.label_local) to statsNumber(state.titles.localAnimeCount),
            stringResource(TLMR.strings.anitenshi_stats_episodes_downloaded) to statsNumber(episodes.downloadCount),
            stringResource(MR.strings.label_tracked_titles) to statsNumber(state.trackers.trackedTitleCount),
            stringResource(MR.strings.label_mean_score) to meanScoreText(state.trackers),
            stringResource(TLMR.strings.anitenshi_stats_trackers_used) to statsNumber(state.trackers.trackerCount),
        ),
    )
}
