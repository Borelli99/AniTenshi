package eu.kanade.presentation.history.anime.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.updates.UpdatesNextCard
import eu.kanade.presentation.updates.UpdatesProgress
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

/**
 * AniTenshi: how far an episode got, as watched and total milliseconds; null when it was not started or was
 * finished. Read from the episode, as the history does not keep it.
 */
@Composable
private fun rememberEpisodeProgress(episodeId: Long): Pair<Long, Long>? {
    var progress by remember(episodeId) { mutableStateOf<Pair<Long, Long>?>(null) }
    LaunchedEffect(episodeId) {
        val getEpisode: GetEpisode = Injekt.get()
        val ep = try {
            getEpisode.await(episodeId)
        } catch (e: Exception) {
            null
        }
        progress = if (ep != null && !ep.seen && ep.lastSecondSeen > 0L) {
            ep.lastSecondSeen to ep.totalSeconds
        } else {
            null
        }
    }
    return progress
}

/**
 * AniTenshi: the last title watched, first in the history, with a button to go on from where it was left.
 */
@Composable
fun AnimeHistoryContinueCard(
    history: AnimeHistoryWithRelations,
    onClick: () -> Unit,
    onClickCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = rememberEpisodeProgress(history.episodeId)
    UpdatesNextCard(
        label = stringResource(TLMR.strings.anitenshi_history_continue_anime),
        title = history.title,
        subtitle = if (history.episodeNumber > -1) {
            stringResource(AYMR.strings.display_mode_episode, formatEpisodeNumber(history.episodeNumber))
        } else {
            ""
        },
        coverData = history.coverData,
        progress = progress?.let { (seen, total) -> if (total > 0L) seen.toFloat() / total else null },
        progressText = progress?.let { (seen, total) -> "${formatProgress(seen)} / ${formatProgress(total)}" },
        buttonText = stringResource(MR.strings.action_resume),
        buttonIcon = Icons.Filled.PlayArrow,
        onClick = onClick,
        onClickCover = onClickCover,
        modifier = modifier,
    )
}

@Composable
fun AnimeHistoryItem(
    history: AnimeHistoryWithRelations,
    timeFormatter: DateTimeFormatter,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClickResume)
            .heightIn(min = 76.dp)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier.width(44.dp),
            data = history.coverData,
            shape = RoundedCornerShape(8.dp),
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = history.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            val seenAt = remember(timeFormatter, history.seenAt) {
                history.seenAt?.toTimestampString(timeFormatter) ?: ""
            }
            Text(
                text = if (history.episodeNumber > -1) {
                    stringResource(
                        AYMR.strings.recent_anime_time,
                        formatEpisodeNumber(history.episodeNumber),
                        seenAt,
                    )
                } else {
                    seenAt
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // AniTenshi: an episode left halfway has a thin gold bar of how far it got
            val progress = rememberEpisodeProgress(history.episodeId)
            if (progress != null && progress.second > 0L) {
                UpdatesProgress(
                    fraction = progress.first.toFloat() / progress.second,
                    text = "${formatProgress(progress.first)} / ${formatProgress(progress.second)}",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (!history.coverData.isAnimeFavorite) {
            IconButton(onClick = onClickFavorite) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(MR.strings.add_to_library),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(AnimeHistoryWithRelationsProvider::class)
    historyWithRelations: AnimeHistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            AnimeHistoryItem(
                history = historyWithRelations,
                timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
