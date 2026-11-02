package eu.kanade.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchCardRow
import eu.kanade.presentation.browse.manga.components.GlobalMangaSearchCardRow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.ui.home.ContinueEntry
import eu.kanade.tachiyomi.ui.home.HomeState
import eu.kanade.tachiyomi.ui.home.LatestRow
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi Home: continue watching, continue reading, latest anime and latest manga
 * from the pinned sources. The button at the bottom right opens More.
 */
@Composable
fun HomeContent(
    state: HomeState,
    snackbarHostState: SnackbarHostState,
    getAnime: @Composable (Anime) -> State<Anime>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickContinueAnime: (ContinueEntry) -> Unit,
    onOpenAnime: (Long) -> Unit,
    onHideAnime: (Long) -> Unit,
    onShowAnime: (Long) -> Unit,
    onClickContinueManga: (ContinueEntry) -> Unit,
    onOpenManga: (Long) -> Unit,
    onHideManga: (Long) -> Unit,
    onShowManga: (Long) -> Unit,
    onClickAnimeSource: (LatestRow<Anime>) -> Unit,
    onClickMangaSource: (LatestRow<Manga>) -> Unit,
    onClickAnime: (Anime) -> Unit,
    onClickManga: (Manga) -> Unit,
    onClickRefresh: () -> Unit,
    onClickMore: () -> Unit,
) {
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_home),
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_webview_refresh),
                                icon = Icons.Outlined.Refresh,
                                onClick = onClickRefresh,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(MR.strings.label_more)) },
                icon = { Icon(imageVector = Icons.Outlined.MoreHoriz, contentDescription = null) },
                onClick = onClickMore,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            // 1. Continue watching: library anime with episodes left
            item(key = "continue_anime_title") {
                SectionTitle(
                    text = stringResource(TLMR.strings.anitenshi_continue_watching),
                    hiddenCount = state.hiddenAnime.size,
                    onClickHidden = { dialog = HomeDialog.Hidden(anime = true) },
                )
            }
            item(key = "continue_anime") {
                if (state.continueAnime.isEmpty()) {
                    SectionHint(stringResource(TLMR.strings.anitenshi_nothing_to_continue_anime))
                } else {
                    ContinueRow(
                        entries = state.continueAnime,
                        onClick = onClickContinueAnime,
                        onLongClick = { dialog = HomeDialog.Actions(entry = it, anime = true) },
                    )
                }
            }

            // 2. Continue reading: library manga with chapters left
            item(key = "continue_manga_title") {
                SectionTitle(
                    text = stringResource(TLMR.strings.anitenshi_continue_reading),
                    hiddenCount = state.hiddenManga.size,
                    onClickHidden = { dialog = HomeDialog.Hidden(anime = false) },
                )
            }
            item(key = "continue_manga") {
                if (state.continueManga.isEmpty()) {
                    SectionHint(stringResource(TLMR.strings.anitenshi_nothing_to_continue_manga))
                } else {
                    ContinueRow(
                        entries = state.continueManga,
                        onClick = onClickContinueManga,
                        onLongClick = { dialog = HomeDialog.Actions(entry = it, anime = false) },
                    )
                }
            }

            // 3. Latest anime from the pinned anime sources
            item(key = "latest_anime_title") {
                SectionTitle(stringResource(TLMR.strings.anitenshi_latest_anime))
            }
            val latestAnime = state.latestAnime
            when {
                latestAnime == null -> item(key = "latest_anime_loading") { GlobalSearchLoadingResultItem() }

                latestAnime.isEmpty() -> item(key = "latest_anime_hint") {
                    SectionHint(stringResource(TLMR.strings.anitenshi_no_pinned_anime_sources))
                }

                else -> items(latestAnime, key = { "latest_anime_${it.sourceId}" }) { row ->
                    GlobalSearchResultItem(
                        title = row.name,
                        subtitle = row.language,
                        onClick = { onClickAnimeSource(row) },
                    ) {
                        val titles = row.items
                        when {
                            titles == null -> GlobalSearchLoadingResultItem()
                            row.error != null -> GlobalSearchErrorResultItem(message = row.error)
                            else -> GlobalAnimeSearchCardRow(
                                titles = titles,
                                getAnime = getAnime,
                                onClick = onClickAnime,
                                onLongClick = onClickAnime,
                            )
                        }
                    }
                }
            }

            // 4. Latest manga from the pinned manga sources
            item(key = "latest_manga_title") {
                SectionTitle(stringResource(TLMR.strings.anitenshi_latest_manga))
            }
            val latestManga = state.latestManga
            when {
                latestManga == null -> item(key = "latest_manga_loading") { GlobalSearchLoadingResultItem() }

                latestManga.isEmpty() -> item(key = "latest_manga_hint") {
                    SectionHint(stringResource(TLMR.strings.anitenshi_no_pinned_manga_sources))
                }

                else -> items(latestManga, key = { "latest_manga_${it.sourceId}" }) { row ->
                    GlobalSearchResultItem(
                        title = row.name,
                        subtitle = row.language,
                        onClick = { onClickMangaSource(row) },
                    ) {
                        val titles = row.items
                        when {
                            titles == null -> GlobalSearchLoadingResultItem()
                            row.error != null -> GlobalSearchErrorResultItem(message = row.error)
                            else -> GlobalMangaSearchCardRow(
                                titles = titles,
                                getManga = getManga,
                                onClick = onClickManga,
                                onLongClick = onClickManga,
                            )
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> {}

        is HomeDialog.Actions -> ContinueActionsDialog(
            entry = current.entry,
            hideLabel = stringResource(
                if (current.anime) {
                    TLMR.strings.anitenshi_continue_hide_anime
                } else {
                    TLMR.strings.anitenshi_continue_hide_manga
                },
            ),
            onOpen = {
                dialog = null
                if (current.anime) onOpenAnime(current.entry.id) else onOpenManga(current.entry.id)
            },
            onHide = {
                dialog = null
                if (current.anime) onHideAnime(current.entry.id) else onHideManga(current.entry.id)
            },
            onDismissRequest = { dialog = null },
        )

        is HomeDialog.Hidden -> {
            val entries = if (current.anime) state.hiddenAnime else state.hiddenManga
            LaunchedEffect(entries.isEmpty()) {
                if (entries.isEmpty()) dialog = null
            }
            HiddenEntriesDialog(
                title = stringResource(
                    if (current.anime) {
                        TLMR.strings.anitenshi_continue_hidden_anime_title
                    } else {
                        TLMR.strings.anitenshi_continue_hidden_manga_title
                    },
                ),
                entries = entries,
                onShow = { if (current.anime) onShowAnime(it.id) else onShowManga(it.id) },
                onDismissRequest = { dialog = null },
            )
        }
    }
}

private sealed interface HomeDialog {
    /** Long press on a title of a continue row. */
    data class Actions(val entry: ContinueEntry, val anime: Boolean) : HomeDialog

    /** The titles hidden from a continue row. */
    data class Hidden(val anime: Boolean) : HomeDialog
}

@Composable
private fun ContinueActionsDialog(
    entry: ContinueEntry,
    hideLabel: String,
    onOpen: () -> Unit,
    onHide: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                TextButton(onClick = onOpen) {
                    Text(text = stringResource(TLMR.strings.anitenshi_continue_open_entry))
                }
                TextButton(onClick = onHide) {
                    Text(text = hideLabel)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun HiddenEntriesDialog(
    title: String,
    entries: List<ContinueEntry>,
    onShow: (ContinueEntry) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            // A plain scrolling column: lazy lists are not safe inside dialogs
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onShow(entry) }) {
                            Text(text = stringResource(TLMR.strings.anitenshi_continue_show_again))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_close))
            }
        },
    )
}

@Composable
private fun SectionTitle(
    text: String,
    hiddenCount: Int = 0,
    onClickHidden: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.small,
                top = MaterialTheme.padding.large,
                bottom = MaterialTheme.padding.small,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (hiddenCount > 0 && onClickHidden != null) {
            TextButton(onClick = onClickHidden) {
                Text(text = stringResource(TLMR.strings.anitenshi_continue_hidden_count, hiddenCount))
            }
        }
    }
}

@Composable
private fun SectionHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.small,
        ),
    )
}

@Composable
private fun ContinueRow(
    entries: List<ContinueEntry>,
    onClick: (ContinueEntry) -> Unit,
    onLongClick: (ContinueEntry) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(entries, key = { it.id }) { entry ->
            Box(modifier = Modifier.width(112.dp)) {
                EntryComfortableGridItem(
                    title = entry.title,
                    titleMaxLines = 2,
                    coverData = entry.cover,
                    // Episodes left to watch / chapters left to read
                    coverBadgeStart = { Badge(text = entry.remaining.toString()) },
                    onClick = { onClick(entry) },
                    onLongClick = { onLongClick(entry) },
                )
            }
        }
    }
}
