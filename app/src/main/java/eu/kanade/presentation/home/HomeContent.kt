package eu.kanade.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.GlobalSearchResultItem
import eu.kanade.presentation.browse.anime.components.GlobalAnimeSearchCardRow
import eu.kanade.presentation.browse.manga.components.GlobalMangaSearchCardRow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.ui.home.HomeState
import eu.kanade.tachiyomi.ui.home.LatestRow
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.entries.EntryCover
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
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
    onClickContinueAnime: (AnimeHistoryWithRelations) -> Unit,
    onLongClickContinueAnime: (AnimeHistoryWithRelations) -> Unit,
    onClickContinueManga: (MangaHistoryWithRelations) -> Unit,
    onLongClickContinueManga: (MangaHistoryWithRelations) -> Unit,
    onClickAnimeSource: (LatestRow<Anime>) -> Unit,
    onClickMangaSource: (LatestRow<Manga>) -> Unit,
    onClickAnime: (Anime) -> Unit,
    onClickManga: (Manga) -> Unit,
    onClickRefresh: () -> Unit,
    onClickMore: () -> Unit,
) {
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
            // 1. Continue watching
            item(key = "continue_anime_title") {
                SectionTitle(stringResource(TLMR.strings.anitenshi_continue_watching))
            }
            item(key = "continue_anime") {
                if (state.continueAnime.isEmpty()) {
                    SectionHint(stringResource(TLMR.strings.anitenshi_nothing_to_continue_anime))
                } else {
                    ContinueRow(
                        entries = state.continueAnime,
                        key = { it.animeId },
                        title = { it.title },
                        badge = {
                            stringResource(
                                TLMR.strings.anitenshi_episode_short,
                                formatEpisodeNumber(it.episodeNumber),
                            )
                        },
                        cover = { it.coverData },
                        onClick = onClickContinueAnime,
                        onLongClick = onLongClickContinueAnime,
                    )
                }
            }

            // 2. Continue reading
            item(key = "continue_manga_title") {
                SectionTitle(stringResource(TLMR.strings.anitenshi_continue_reading))
            }
            item(key = "continue_manga") {
                if (state.continueManga.isEmpty()) {
                    SectionHint(stringResource(TLMR.strings.anitenshi_nothing_to_continue_manga))
                } else {
                    ContinueRow(
                        entries = state.continueManga,
                        key = { it.mangaId },
                        title = { it.title },
                        badge = {
                            stringResource(
                                TLMR.strings.anitenshi_chapter_short,
                                formatChapterNumber(it.chapterNumber),
                            )
                        },
                        cover = { it.coverData },
                        onClick = onClickContinueManga,
                        onLongClick = onLongClickContinueManga,
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
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(
            start = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.medium,
            top = MaterialTheme.padding.large,
            bottom = MaterialTheme.padding.small,
        ),
    )
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
private fun <T> ContinueRow(
    entries: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    badge: @Composable (T) -> String,
    cover: (T) -> EntryCover,
    onClick: (T) -> Unit,
    onLongClick: (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(entries, key = key) { item ->
            Box(modifier = Modifier.width(112.dp)) {
                EntryComfortableGridItem(
                    title = title(item),
                    titleMaxLines = 2,
                    coverData = cover(item),
                    coverBadgeEnd = { Badge(text = badge(item)) },
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                )
            }
        }
    }
}
