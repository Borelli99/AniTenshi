package eu.kanade.presentation.updates.manga

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.entries.manga.components.ChapterDownloadAction
import eu.kanade.presentation.entries.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.updates.UpdatesCardRow
import eu.kanade.presentation.updates.UpdatesDayHeader
import eu.kanade.presentation.updates.UpdatesLastUpdatedLine
import eu.kanade.presentation.updates.UpdatesNextCard
import eu.kanade.presentation.updates.updatesNewCounts
import eu.kanade.presentation.updates.updatesRowPlaces
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.updates.manga.MangaUpdatesItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground

internal fun LazyListScope.mangaUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "mangaUpdates-lastUpdated") {
        UpdatesLastUpdatedLine(
            lastUpdated = lastUpdated,
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
    }
}

/**
 * AniTenshi: the latest chapter not read yet, first on the screen, with a button to read it or go on.
 */
internal fun LazyListScope.mangaUpdatesNextItem(
    next: MangaUpdatesItem?,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onOpenChapter: (MangaUpdatesItem) -> Unit,
) {
    if (next == null) return
    item(key = "mangaUpdates-next") {
        val update = next.update
        val started = !update.read && update.lastPageRead > 0L
        UpdatesNextCard(
            label = stringResource(TLMR.strings.anitenshi_updates_next_manga),
            title = update.mangaTitle,
            subtitle = update.chapterName,
            coverData = update.coverData,
            // The number of pages of a chapter is not known before it is opened: the page reached, without a bar
            progress = null,
            progressText = if (started) stringResource(MR.strings.chapter_progress, update.lastPageRead + 1) else null,
            buttonText = stringResource(
                if (started) MR.strings.action_resume else TLMR.strings.anitenshi_updates_read,
            ),
            buttonIcon = Icons.AutoMirrored.Outlined.MenuBook,
            onClick = { onOpenChapter(next) },
            onClickCover = { onClickCover(next) },
            modifier = Modifier.animateItem(),
        )
    }
}

internal fun LazyListScope.mangaUpdatesUiItems(
    uiModels: List<MangaUpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (MangaUpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onClickUpdate: (MangaUpdatesItem) -> Unit,
    onDownloadChapter: (List<MangaUpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    // AniTenshi: each day is a card, drawn row by row, and its header says how many chapters are new
    val isHeader = { model: MangaUpdatesUiModel -> model is MangaUpdatesUiModel.Header }
    val places = updatesRowPlaces(uiModels, isHeader)
    val newCounts = updatesNewCounts(uiModels, isHeader) { model ->
        (model as MangaUpdatesUiModel.Item).item.update.read.not()
    }
    itemsIndexed(
        items = uiModels,
        contentType = { _, it ->
            when (it) {
                is MangaUpdatesUiModel.Header -> "header"
                is MangaUpdatesUiModel.Item -> "item"
            }
        },
        key = { _, it ->
            when (it) {
                is MangaUpdatesUiModel.Header -> "mangaUpdatesHeader-${it.hashCode()}"
                is MangaUpdatesUiModel.Item -> "mangaUpdates-${it.item.update.mangaId}-${it.item.update.chapterId}"
            }
        },
    ) { index, item ->
        when (item) {
            is MangaUpdatesUiModel.Header -> {
                UpdatesDayHeader(
                    date = item.date,
                    newCount = newCounts[index],
                    modifier = Modifier.animateItem(),
                )
            }

            is MangaUpdatesUiModel.Item -> {
                val updatesItem = item.item
                UpdatesCardRow(
                    place = places[index]!!,
                    modifier = Modifier.animateItem(),
                ) {
                    MangaUpdatesUiItem(
                        update = updatesItem.update,
                        selected = updatesItem.selected,
                        readProgress = updatesItem.update.lastPageRead
                            .takeIf { !updatesItem.update.read && it > 0L }
                            ?.let {
                                stringResource(
                                    MR.strings.chapter_progress,
                                    it + 1,
                                )
                            },
                        onLongClick = {
                            onUpdateSelected(updatesItem, !updatesItem.selected, true)
                        },
                        onClick = {
                            when {
                                selectionMode -> onUpdateSelected(updatesItem, !updatesItem.selected, false)
                                else -> onClickUpdate(updatesItem)
                            }
                        },
                        onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                        onDownloadChapter = { action: ChapterDownloadAction ->
                            onDownloadChapter(listOf(updatesItem), action)
                        }.takeIf { !selectionMode },
                        downloadStateProvider = updatesItem.downloadStateProvider,
                        downloadProgressProvider = updatesItem.downloadProgressProvider,
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaUpdatesUiItem(
    update: MangaUpdatesWithRelations,
    selected: Boolean,
    readProgress: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadChapter: ((ChapterDownloadAction) -> Unit)?,
    // Download Indicator
    downloadStateProvider: () -> MangaDownload.State,
    downloadProgressProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    // AniTenshi: a chapter already read is dimmed, cover included
    val readAlpha = if (update.read) DISABLED_ALPHA else 1f
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
                .alpha(readAlpha),
            data = update.coverData,
            shape = RoundedCornerShape(8.dp),
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .weight(1f)
                .alpha(readAlpha),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = update.mangaTitle,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                if (!update.read) {
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
                        imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier
                            .sizeIn(
                                maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp },
                            ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = update.chapterName,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
                if (readProgress != null) {
                    DotSeparatorText()
                    Text(
                        text = readProgress,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ChapterDownloadIndicator(
            enabled = onDownloadChapter != null,
            modifier = Modifier.padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = { onDownloadChapter?.invoke(it) },
        )
    }
}
