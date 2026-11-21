package eu.kanade.presentation.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.updates.UpdatesColumnItem
import eu.kanade.presentation.updates.UpdatesProgress
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.download.DownloadQueueEntry
import eu.kanade.tachiyomi.ui.download.DownloadQueueSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.MoreVert
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DownloadQueueList(
    sections: List<DownloadQueueSection>,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onReorder: (List<DownloadQueueEntry>) -> Unit,
    onMove: (DownloadQueueEntry, toTop: Boolean) -> Unit,
    onMoveSeries: (DownloadQueueEntry, toTop: Boolean) -> Unit,
    onCancel: (DownloadQueueEntry) -> Unit,
    onCancelSeries: (DownloadQueueEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionsState = remember { sections.toMutableStateList() }
    // While a section is dragged its downloads are hidden, so it moves past whole sections
    var isSectionDragging by remember { mutableStateOf(false) }
    // While a download is dragged only its own section's rows can be swapped with it
    var draggingSectionId by remember { mutableStateOf<Long?>(null) }
    // The release animation starts from the finger, so the last dragged row stays held while it settles too
    var lastDraggedKey by remember { mutableStateOf<String?>(null) }
    val listCoordinates = remember { CoordinatesHolder() }
    val draggingSection by rememberUpdatedState(sectionsState.find { it.id == draggingSectionId })
    // Auto-scroll would otherwise carry a download held at its section's edge off screen
    val sectionScrollState = remember(lazyListState) {
        LimitedScrollableState(lazyListState) { pixels ->
            draggingSection?.let { lazyListState.limitScrollToSection(it, pixels) } ?: pixels
        }
    }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = contentPadding,
        scroller = rememberScroller(
            scrollableState = sectionScrollState,
            pixelAmountProvider = { lazyListState.layoutInfo.viewportSize.height * 0.05f },
        ),
    ) { from, to ->
        val fromKey = from.key as String
        val toKey = to.key as String
        when {
            fromKey.startsWith(SECTION_KEY) && toKey.startsWith(SECTION_KEY) -> {
                val fromIndex = sectionsState.indexOfFirst { it.key == fromKey }
                val toIndex = sectionsState.indexOfFirst { it.key == toKey }
                sectionsState.add(toIndex, sectionsState.removeAt(fromIndex))
            }

            // Downloads don't change section
            fromKey.startsWith(DOWNLOAD_KEY) && toKey.startsWith(DOWNLOAD_KEY) -> {
                val sectionIndex = sectionsState.indexOfFirst { section ->
                    section.downloads.any { it.key == fromKey }
                }
                val section = sectionsState[sectionIndex]
                val toIndex = section.downloads.indexOfFirst { it.key == toKey }
                if (toIndex != -1) {
                    val downloads = section.downloads.toMutableList()
                    val fromIndex = downloads.indexOfFirst { it.key == fromKey }
                    downloads.add(toIndex, downloads.removeAt(fromIndex))
                    sectionsState[sectionIndex] = section.copy(downloads = downloads)
                }
            }
        }
    }

    LaunchedEffect(sections, reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            sectionsState.clear()
            sectionsState.addAll(sections)
        }
    }

    val onDragStopped = {
        isSectionDragging = false
        draggingSectionId = null
        onReorder(sectionsState.flatMap { it.downloads })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates.value = it },
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        sectionsState.forEachIndexed { sectionIndex, section ->
            item(key = section.key) {
                ReorderableItem(reorderableState, section.key, enabled = draggingSectionId == null) { isDragging ->
                    DownloadQueueHeader(
                        section = section,
                        isDragging = isDragging,
                        // While a title is dragged the downloads are hidden, so each title's card is its header alone
                        isAlone = isSectionDragging,
                        canMoveToTop = sectionIndex > 0,
                        canMoveToBottom = sectionIndex < sectionsState.lastIndex,
                        onDragStarted = { isSectionDragging = true },
                        onDragStopped = onDragStopped,
                        onMoveSeries = { toTop -> onMoveSeries(section.downloads.first(), toTop) },
                        onCancelSeries = { onCancelSeries(section.downloads.first()) },
                    )
                }
            }
            if (!isSectionDragging) {
                itemsIndexed(
                    items = section.downloads,
                    key = { _, download -> download.key },
                ) { index, download ->
                    ReorderableItem(
                        state = reorderableState,
                        key = download.key,
                        enabled = draggingSectionId == null || draggingSectionId == section.id,
                    ) { isDragging ->
                        DownloadQueueItem(
                            download = download,
                            isDragging = isDragging,
                            isFirst = index == 0,
                            isLast = index == section.downloads.lastIndex,
                            modifier = if (isDragging || download.key == lastDraggedKey) {
                                Modifier.keepInSection(
                                    lazyListState = lazyListState,
                                    listCoordinates = listCoordinates,
                                    sectionKey = section.key,
                                    lastDownloadKey = section.downloads.last().key,
                                )
                            } else {
                                Modifier
                            },
                            canMoveToTop = index > 0,
                            canMoveToBottom = index < section.downloads.lastIndex,
                            onDragStarted = {
                                draggingSectionId = section.id
                                lastDraggedKey = download.key
                            },
                            onDragStopped = onDragStopped,
                            onMove = { toTop -> onMove(download, toTop) },
                            onCancel = { onCancel(download) },
                        )
                    }
                }
            }
        }
    }
}

// Lazy list keys must fit in a Bundle
private const val SECTION_KEY = "section-"
private const val DOWNLOAD_KEY = "download-"
private val DownloadQueueSection.key get() = "$SECTION_KEY$id"
private val DownloadQueueEntry.key: String
    get() = when (this) {
        is DownloadQueueEntry.Manga -> "${DOWNLOAD_KEY}manga-${download.chapter.id}"
        is DownloadQueueEntry.Anime -> "${DOWNLOAD_KEY}anime-${download.episode.id}"
    }

private class CoordinatesHolder {
    var value: LayoutCoordinates? = null
}

/**
 * [lazyListState], with every scroll step passed through [limit] first.
 */
private class LimitedScrollableState(
    private val lazyListState: LazyListState,
    private val limit: (Float) -> Float,
) : ScrollableState by lazyListState {
    override suspend fun scroll(scrollPriority: MutatePriority, block: suspend ScrollScope.() -> Unit) {
        lazyListState.scroll(scrollPriority) {
            val scope = this
            object : ScrollScope {
                override fun scrollBy(pixels: Float) = scope.scrollBy(limit(pixels))
            }.block()
        }
    }
}

/**
 * Limits [pixels] so the list scrolls no further than the end of [section] reaching the bottom, or its header
 * reaching the top, while that edge is on screen.
 */
private fun LazyListState.limitScrollToSection(section: DownloadQueueSection, pixels: Float): Float {
    val items = layoutInfo.visibleItemsInfo
    return if (pixels > 0) {
        val last = items.firstOrNull { it.key == section.downloads.last().key } ?: return pixels
        val contentEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        pixels.coerceAtMost((last.offset + last.size - contentEnd).coerceAtLeast(0).toFloat())
    } else {
        val header = items.firstOrNull { it.key == section.key } ?: return pixels
        pixels.coerceAtLeast((header.offset).coerceAtMost(0).toFloat())
    }
}

/**
 * Stops a dragged download at the edges of its section, between the header and the section's last slot, instead of
 * letting it float over another source it can't be dropped in.
 */
@Composable
private fun Modifier.keepInSection(
    lazyListState: LazyListState,
    listCoordinates: CoordinatesHolder,
    sectionKey: String,
    lastDownloadKey: String,
): Modifier {
    var correction by remember { mutableFloatStateOf(0f) }
    return this
        // Before the correction layer, so it sees where the drag put the row
        .onGloballyPositioned { coordinates ->
            val list = listCoordinates.value?.takeIf { it.isAttached } ?: return@onGloballyPositioned
            val top = list.localPositionOf(coordinates, Offset.Zero).y
            val layoutInfo = lazyListState.layoutInfo
            val itemStart = layoutInfo.beforeContentPadding
            val header = layoutInfo.visibleItemsInfo.firstOrNull { it.key == sectionKey }
            val last = layoutInfo.visibleItemsInfo.firstOrNull { it.key == lastDownloadKey }
            val minTop = header?.let { (itemStart + it.offset + it.size).toFloat() } ?: Float.NEGATIVE_INFINITY
            val maxTop = last?.let { (itemStart + it.offset + it.size - coordinates.size.height).toFloat() }
                ?: Float.POSITIVE_INFINITY
            correction = top.coerceIn(minTop, maxOf(minTop, maxTop)) - top
        }
        .graphicsLayer { translationY = correction }
}

/*
 * AniTenshi: each title is a card, its header with the cover, the source and how many are left, then its episodes
 * or chapters. Only the one being downloaded has a bar; the others just say they are queued, or that they failed.
 */

private val CardRadius = 20.dp

@Composable
private fun ReorderableCollectionItemScope.DownloadQueueHeader(
    section: DownloadQueueSection,
    isDragging: Boolean,
    isAlone: Boolean,
    canMoveToTop: Boolean,
    canMoveToBottom: Boolean,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    onMoveSeries: (toTop: Boolean) -> Unit,
    onCancelSeries: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val first = section.downloads.first()
    val coverData = remember(first) { first.coverData }
    val count = section.downloads.size
    val countText = when (first) {
        is DownloadQueueEntry.Anime -> pluralStringResource(TLMR.plurals.anitenshi_downloads_episodes, count, count)
        is DownloadQueueEntry.Manga -> pluralStringResource(TLMR.plurals.anitenshi_downloads_chapters, count, count)
    }
    val shape = if (isAlone || isDragging) {
        RoundedCornerShape(CardRadius)
    } else {
        RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius)
    }
    UpdatesColumnItem(modifier = Modifier.padding(top = 14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = if (isDragging) {
                colors.surfaceContainerHighest
            } else {
                lerp(colors.surfaceContainer, colors.surfaceContainerHigh, 0.5f)
            },
            shadowElevation = listItemElevation(isDragging),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemCover.Book(
                    data = coverData,
                    modifier = Modifier.width(40.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = first.title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(first.sourceName)
                            append(" · ")
                            withStyle(SpanStyle(color = colors.secondary, fontWeight = FontWeight.Medium)) {
                                append(countText)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DragHandle(
                        onDragStarted = onDragStarted,
                        onDragStopped = onDragStopped,
                        tint = colors.onSurfaceVariant,
                    )
                    DownloadQueueMenu(
                        canMoveToTop = canMoveToTop,
                        canMoveToBottom = canMoveToBottom,
                        cancelText = stringResource(TLMR.strings.anitenshi_downloads_cancel_title),
                        onMove = onMoveSeries,
                        onCancel = onCancelSeries,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.DownloadQueueItem(
    download: DownloadQueueEntry,
    isDragging: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    canMoveToTop: Boolean,
    canMoveToBottom: Boolean,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    onMove: (toTop: Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val state by remember(download) { download.stateFlow() }.collectAsState(DownloadRowState.Queued)
    // The shape alone changes while rows are dragged, never the height, so the list doesn't jump
    val shape = when {
        isDragging -> RoundedCornerShape(14.dp)
        isLast -> RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
        else -> RectangleShape
    }
    UpdatesColumnItem(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = if (isDragging) colors.surfaceContainerHigh else colors.surfaceContainer,
            shadowElevation = listItemElevation(isDragging),
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 44.dp, end = 12.dp),
                    color = if (isFirst || isDragging) Color.Transparent else colors.surfaceContainerHigh,
                )
                Row(
                    modifier = Modifier
                        .heightIn(min = 60.dp)
                        .padding(start = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DragHandle(
                        onDragStarted = onDragStarted,
                        onDragStopped = onDragStopped,
                        tint = colors.outline,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = download.itemName,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        DownloadStateLine(download = download, state = state)
                    }
                    DownloadQueueMenu(
                        canMoveToTop = canMoveToTop,
                        canMoveToBottom = canMoveToBottom,
                        cancelText = stringResource(MR.strings.action_cancel),
                        onMove = onMove,
                        onCancel = onCancel,
                    )
                }
            }
        }
    }
}

/** What a row says under its name, with the gold bar while it downloads. */
@Composable
private fun DownloadStateLine(download: DownloadQueueEntry, state: DownloadRowState) {
    val colors = MaterialTheme.colorScheme
    when (state) {
        DownloadRowState.Queued -> Text(
            text = stringResource(TLMR.strings.anitenshi_downloads_state_queued),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        DownloadRowState.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.error,
            )
            Text(
                text = stringResource(TLMR.strings.anitenshi_downloads_state_failed),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colors.error,
            )
        }

        DownloadRowState.Starting -> {
            DownloadingLabel(stringResource(TLMR.strings.anitenshi_downloads_state_starting))
            IndeterminateBar()
        }

        is DownloadRowState.Downloading -> {
            DownloadingLabel(stringResource(TLMR.strings.anitenshi_downloads_state_downloading))
            if (state.fraction == null) {
                IndeterminateBar()
            } else {
                val fraction by animateFloatAsState(state.fraction, label = "progress")
                UpdatesProgress(
                    fraction = fraction,
                    text = when (download) {
                        is DownloadQueueEntry.Anime -> stringResource(
                            AYMR.strings.episode_download_progress,
                            (state.fraction * 100).toInt(),
                        )

                        is DownloadQueueEntry.Manga -> stringResource(
                            TLMR.strings.anitenshi_downloads_pages,
                            state.downloadedPages,
                            state.pageCount,
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun IndeterminateBar() {
    LinearProgressIndicator(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

@Composable
private fun ReorderableCollectionItemScope.DragHandle(
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    tint: Color,
) {
    Icon(
        imageVector = MaterialSymbols.Rounded.DragHandle,
        contentDescription = null,
        modifier = Modifier
            .draggableHandle(
                onDragStarted = { onDragStarted() },
                onDragStopped = onDragStopped,
            )
            .padding(MaterialTheme.padding.small),
        tint = tint,
    )
}

/** The ⋮ of a title (for all its downloads) or of a single download. */
@Composable
private fun DownloadQueueMenu(
    canMoveToTop: Boolean,
    canMoveToBottom: Boolean,
    cancelText: String,
    onMove: (toTop: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val closeMenu = { expanded = false }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.Rounded.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = closeMenu) {
            if (canMoveToTop) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_top)) },
                    onClick = {
                        closeMenu()
                        onMove(true)
                    },
                )
            }
            if (canMoveToBottom) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_bottom)) },
                    onClick = {
                        closeMenu()
                        onMove(false)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(cancelText) },
                onClick = {
                    closeMenu()
                    onCancel()
                },
            )
        }
    }
}

@Composable
private fun listItemElevation(isDragging: Boolean): Dp {
    val elevation = ListItemDefaults.elevation()
    return if (isDragging) elevation.draggedElevation else elevation.elevation
}

private val DownloadQueueEntry.title: String
    get() = when (this) {
        is DownloadQueueEntry.Manga -> download.manga.title
        is DownloadQueueEntry.Anime -> download.anime.title
    }

private val DownloadQueueEntry.sourceName: String
    get() = when (this) {
        is DownloadQueueEntry.Manga -> download.source.name
        is DownloadQueueEntry.Anime -> download.source.name
    }

private val DownloadQueueEntry.coverData: Any
    get() = when (this) {
        is DownloadQueueEntry.Manga -> download.manga.asMangaCover()
        is DownloadQueueEntry.Anime -> download.anime.asAnimeCover()
    }

private val DownloadQueueEntry.itemName: String
    get() = when (this) {
        is DownloadQueueEntry.Manga -> download.chapter.name
        is DownloadQueueEntry.Anime -> download.episode.name
    }

private sealed interface DownloadRowState {
    data object Queued : DownloadRowState
    data object Starting : DownloadRowState
    data object Failed : DownloadRowState

    /**
     * @param fraction how much is done, null when the size isn't known.
     * @param pageCount the chapter's pages, 0 for an episode.
     */
    data class Downloading(
        val fraction: Float?,
        val downloadedPages: Int = 0,
        val pageCount: Int = 0,
    ) : DownloadRowState
}

private class PagesProgress(
    val pageCount: Int,
    val downloadedPages: Int,
    val totalProgress: Int,
) {
    val fraction = if (pageCount == 0) 0f else totalProgress / (pageCount * 100f)
}

/**
 * Progress of the pages of this chapter, or null until its pages are known.
 */
private fun MangaDownload.pagesProgressFlow() = pagesFlow.flatMapLatest { pages ->
    if (pages.isNullOrEmpty()) {
        flowOf(null)
    } else {
        combine(pages.map { page -> combine(page.statusFlow, page.progressFlow, ::Pair) }) { states ->
            PagesProgress(
                pageCount = pages.size,
                downloadedPages = states.count { (status, _) -> status == Page.State.READY },
                totalProgress = states.sumOf { (_, progress) -> progress },
            )
        }
    }
}

private fun DownloadQueueEntry.stateFlow(): Flow<DownloadRowState> = when (this) {
    is DownloadQueueEntry.Manga -> combine(download.statusFlow, download.pagesProgressFlow()) { status, pages ->
        when (status) {
            MangaDownload.State.ERROR -> DownloadRowState.Failed

            MangaDownload.State.DOWNLOADING, MangaDownload.State.DOWNLOADED -> if (pages == null) {
                DownloadRowState.Starting
            } else {
                DownloadRowState.Downloading(pages.fraction, pages.downloadedPages, pages.pageCount)
            }

            else -> DownloadRowState.Queued
        }
    }

    is DownloadQueueEntry.Anime -> combine(download.progressFlow, download.statusFlow) { progress, status ->
        when (status) {
            AnimeDownload.State.ERROR -> DownloadRowState.Failed

            AnimeDownload.State.DOWNLOADING -> when {
                // -1: the size of the video isn't known, so there's no way to say how far it has got
                progress < 0 -> DownloadRowState.Downloading(fraction = null)
                progress == 0 -> DownloadRowState.Starting
                else -> DownloadRowState.Downloading(progress.coerceAtMost(100) / 100f)
            }

            AnimeDownload.State.DOWNLOADED -> DownloadRowState.Downloading(1f)

            else -> DownloadRowState.Queued
        }
    }
}
