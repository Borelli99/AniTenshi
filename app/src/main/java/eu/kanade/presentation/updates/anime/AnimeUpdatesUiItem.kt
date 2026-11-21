package eu.kanade.presentation.updates.anime
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadIndicator
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.updates.UpdatesCardRow
import eu.kanade.presentation.updates.UpdatesDayHeader
import eu.kanade.presentation.updates.UpdatesLastUpdatedLine
import eu.kanade.presentation.updates.UpdatesNextCard
import eu.kanade.presentation.updates.UpdatesProgress
import eu.kanade.presentation.updates.updatesNewCounts
import eu.kanade.presentation.updates.updatesRowPlaces
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadProvider
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesItem
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

internal fun LazyListScope.animeUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "animeUpdates-lastUpdated") {
        UpdatesLastUpdatedLine(
            lastUpdated = lastUpdated,
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
    }
}

/**
 * AniTenshi: the latest episode not watched yet, first on the screen, with a button to watch it or go on.
 */
internal fun LazyListScope.animeUpdatesNextItem(
    next: AnimeUpdatesItem?,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onOpenEpisode: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
) {
    if (next == null) return
    item(key = "animeUpdates-next") {
        val update = next.update
        val started = !update.seen && update.lastSecondSeen > 0L && update.totalSeconds > 0L
        UpdatesNextCard(
            label = stringResource(TLMR.strings.anitenshi_updates_next_anime),
            title = update.animeTitle,
            subtitle = update.episodeName,
            coverData = update.coverData,
            progress = if (started) update.lastSecondSeen.toFloat() / update.totalSeconds else null,
            progressText = if (started) {
                "${formatProgress(update.lastSecondSeen)} / ${formatProgress(update.totalSeconds)}"
            } else {
                null
            },
            buttonText = stringResource(
                if (started) MR.strings.action_resume else TLMR.strings.anitenshi_updates_watch,
            ),
            buttonIcon = Icons.Filled.PlayArrow,
            onClick = { onOpenEpisode(next, false) },
            onClickCover = { onClickCover(next) },
            modifier = Modifier.animateItem(),
        )
    }
}

internal fun LazyListScope.animeUpdatesUiItems(
    uiModels: List<AnimeUpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (AnimeUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onClickUpdate: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
) {
    // AniTenshi: each day is a card, drawn row by row, and its header says how many episodes are new
    val isHeader = { model: AnimeUpdatesUiModel -> model is AnimeUpdatesUiModel.Header }
    val places = updatesRowPlaces(uiModels, isHeader)
    val newCounts = updatesNewCounts(uiModels, isHeader) { model ->
        (model as AnimeUpdatesUiModel.Item).item.update.seen.not()
    }
    itemsIndexed(
        items = uiModels,
        contentType = { _, it ->
            when (it) {
                is AnimeUpdatesUiModel.Header -> "header"
                is AnimeUpdatesUiModel.Item -> "item"
            }
        },
        key = { _, it ->
            when (it) {
                is AnimeUpdatesUiModel.Header -> "animeUpdatesHeader-${it.hashCode()}"
                is AnimeUpdatesUiModel.Item -> "animeUpdates-${it.item.update.animeId}-${it.item.update.episodeId}"
            }
        },
    ) { index, item ->
        when (item) {
            is AnimeUpdatesUiModel.Header -> {
                UpdatesDayHeader(
                    date = item.date,
                    newCount = newCounts[index],
                    modifier = Modifier.animateItem(),
                )
            }

            is AnimeUpdatesUiModel.Item -> {
                val updatesItem = item.item
                val update = updatesItem.update
                val started = !update.seen && update.lastSecondSeen > 0L
                UpdatesCardRow(
                    place = places[index]!!,
                    modifier = Modifier.animateItem(),
                ) {
                    AnimeUpdatesUiItem(
                        update = update,
                        selected = updatesItem.selected,
                        watchProgress = if (started) {
                            stringResource(
                                AYMR.strings.episode_progress,
                                formatProgress(update.lastSecondSeen),
                                formatProgress(update.totalSeconds),
                            )
                        } else {
                            null
                        },
                        watchFraction = if (started && update.totalSeconds > 0L) {
                            update.lastSecondSeen.toFloat() / update.totalSeconds
                        } else {
                            null
                        },
                        onLongClick = {
                            onUpdateSelected(updatesItem, !updatesItem.selected, true, true)
                        },
                        onClick = {
                            when {
                                selectionMode -> onUpdateSelected(
                                    updatesItem,
                                    !updatesItem.selected,
                                    true,
                                    false,
                                )

                                else -> onClickUpdate(updatesItem, false)
                            }
                        },
                        onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                        onDownloadEpisode = { action: EpisodeDownloadAction ->
                            onDownloadEpisode(listOf(updatesItem), action)
                        }.takeIf { !selectionMode },
                        downloadStateProvider = updatesItem.downloadStateProvider,
                        downloadProgressProvider = updatesItem.downloadProgressProvider,
                        // AM (FILE_SIZE) -->
                        updatesItem = updatesItem,
                        // <-- AM (FILE_SIZE)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeUpdatesUiItem(
    update: AnimeUpdatesWithRelations,
    selected: Boolean,
    watchProgress: String?,
    watchFraction: Float?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadEpisode: ((EpisodeDownloadAction) -> Unit)?,
    // Download Indicator
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    // AM (FILE_SIZE) -->
    updatesItem: AnimeUpdatesItem,
    // <-- AM (FILE_SIZE)
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    // AniTenshi: an episode already watched is dimmed, cover included
    val seenAlpha = if (update.seen) DISABLED_ALPHA else 1f
    val unreadDescription = stringResource(MR.strings.unread)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .heightIn(min = 68.dp)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier
                .width(40.dp)
                .alpha(seenAlpha),
            data = update.coverData,
            shape = RoundedCornerShape(8.dp),
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .weight(1f)
                .alpha(seenAlpha),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = update.animeTitle,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                if (!update.seen) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .semantics { contentDescription = unreadDescription },
                    )
                }
                if (update.bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier
                            .sizeIn(
                                maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp },
                            ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                if (update.fillermark) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = stringResource(AYMR.strings.action_filter_fillermarked),
                        modifier = Modifier
                            .sizeIn(
                                maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp },
                            ),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = update.episodeName,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
            }
            // AniTenshi: how far the episode has got, as a thin gold bar
            if (watchFraction != null) {
                UpdatesProgress(
                    fraction = watchFraction,
                    text = watchProgress,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
// AM (FILE_SIZE) -->
        var fileSizeAsync: Long? by remember { mutableStateOf(updatesItem.fileSize) }
        if (downloadStateProvider() == AnimeDownload.State.DOWNLOADED &&
            storagePreferences.showEpisodeFileSize.get() &&
            fileSizeAsync == null
        ) {
            LaunchedEffect(update, Unit) {
                fileSizeAsync = withIOContext {
                    animeDownloadProvider.getEpisodeFileSize(
                        update.episodeName,
                        null,
                        update.scanlator,
                        // AM (CUSTOM_INFORMATION) -->
                        update.animeTitle,
                        // <-- AM (CUSTOM_INFORMATION)
                        animeSourceManager.getOrStub(update.sourceId),
                    )
                }
                updatesItem.fileSize = fileSizeAsync
            }
        }
        // <-- AM (FILE_SIZE)

        EpisodeDownloadIndicator(
            enabled = onDownloadEpisode != null,
            modifier = Modifier.padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = { onDownloadEpisode?.invoke(it) },
            // AM (FILE_SIZE) -->
            fileSize = fileSizeAsync,
            // <-- AM (FILE_SIZE)
        )
    }
}

internal fun formatProgress(milliseconds: Long): String {
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

// AM (FILE_SIZE) -->
private val storagePreferences: StoragePreferences by injectLazy()
private val animeDownloadProvider: AnimeDownloadProvider by injectLazy()
private val animeSourceManager: AnimeSourceManager by injectLazy()
// <-- AM (FILE_SIZE)
