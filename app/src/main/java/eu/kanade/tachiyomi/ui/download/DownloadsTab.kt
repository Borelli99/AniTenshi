package eu.kanade.tachiyomi.ui.download

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.presentation.components.settingsOverflowAction
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadQueueViewModel
import eu.kanade.tachiyomi.ui.download.anime.animeDownloadTab
import eu.kanade.tachiyomi.ui.download.manga.MangaDownloadQueueViewModel
import eu.kanade.tachiyomi.ui.download.manga.mangaDownloadTab
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data object DownloadsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 6u,
                title = stringResource(MR.strings.label_download_queue),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val animeViewModel = metroViewModel<AnimeDownloadQueueViewModel>()
        val mangaScreenModel = metroViewModel<MangaDownloadQueueViewModel>()
        val animeDownloadList by animeViewModel.composeState.collectAsStateWithLifecycle()
        val mangaDownloadList by mangaScreenModel.composeState.collectAsStateWithLifecycle()
        val animeDownloadCount by remember {
            derivedStateOf { animeDownloadList.sumOf { it.downloads.size } }
        }
        val mangaDownloadCount by remember {
            derivedStateOf { mangaDownloadList.sumOf { it.downloads.size } }
        }

        val state = rememberPagerState { 2 }
        val snackbarHostState = remember { SnackbarHostState() }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        // AniTenshi: no floating button any more (pause and resume are at the top of each list)
        val nestedScrollConnection = scrollBehavior.nestedScrollConnection

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.label_download_queue),
                    navigateUp = navigator::pop,
                    actions = {
                        when (state.currentPage) {
                            0 -> AnimeActions(animeViewModel, animeDownloadList)
                            1 -> MangaActions(mangaScreenModel, mangaDownloadList)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                PrimaryTabRow(
                    selectedTabIndex = state.currentPage,
                    modifier = Modifier.zIndex(1f),
                ) {
                    listOf(
                        Tab(
                            selected = state.currentPage == 0,
                            onClick = { scope.launch { state.animateScrollToPage(0) } },
                            text = {
                                DownloadTabText(
                                    text = stringResource(AYMR.strings.label_anime),
                                    count = animeDownloadCount,
                                    selected = state.currentPage == 0,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        Tab(
                            selected = state.currentPage == 1,
                            onClick = { scope.launch { state.animateScrollToPage(1) } },
                            text = {
                                DownloadTabText(
                                    text = stringResource(AYMR.strings.manga),
                                    count = mangaDownloadCount,
                                    selected = state.currentPage == 1,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    verticalAlignment = Alignment.Top,
                    pageNestedScrollConnection = nestedScrollConnection,
                ) { page ->
                    when (page) {
                        0 -> animeDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )

                        1 -> mangaDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )
                    }
                }
            }
        }
    }

    /** AniTenshi: last item of the ⋮ menu, which opens the downloads' page of the settings. */
    @Composable
    private fun downloadSettingsAction(): AppBar.OverflowAction {
        val navigator = LocalNavigator.currentOrThrow
        return settingsOverflowAction(
            title = stringResource(TLMR.strings.anitenshi_settings_downloads),
            onClick = { navigator.push(SettingsScreen(SettingsScreen.Destination.Downloads)) },
        )
    }

    @Composable
    private fun AnimeActions(
        animeViewModel: AnimeDownloadQueueViewModel,
        animeDownloadList: List<DownloadQueueSection>,
    ) {
        val settingsAction = downloadSettingsAction()
        if (animeDownloadList.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            val onDismissRequest = { sortExpanded = false }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = onDismissRequest,
            ) {
                NestedMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                            onClick = {
                                animeViewModel.reorderComposeQueue(
                                    {
                                        it.episode.let { e ->
                                            e.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: e.dateUpload
                                        }
                                    },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                animeViewModel.reorderComposeQueue(
                                    {
                                        it.episode.let { e ->
                                            e.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: e.dateUpload
                                        }
                                    },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
                NestedMenuItem(
                    text = {
                        Text(
                            text = stringResource(AYMR.strings.action_order_by_episode_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                animeViewModel.reorderComposeQueue(
                                    { it.episode.episodeNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                animeViewModel.reorderComposeQueue(
                                    { it.episode.episodeNumber },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
            }

            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_sort),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { sortExpanded = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_cancel_all),
                        onClick = { animeViewModel.clearQueue() },
                    ),
                    settingsAction,
                ),
            )
        } else {
            // AniTenshi: the settings are there with an empty queue too
            AppBarActions(persistentListOf(settingsAction))
        }
    }

    @Composable
    private fun MangaActions(
        mangaScreenModel: MangaDownloadQueueViewModel,
        mangaDownloadList: List<DownloadQueueSection>,
    ) {
        val settingsAction = downloadSettingsAction()
        if (mangaDownloadList.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            val onDismissRequest = { sortExpanded = false }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = onDismissRequest,
            ) {
                NestedMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                            onClick = {
                                mangaScreenModel.reorderComposeQueue(
                                    {
                                        it.chapter.let { c ->
                                            c.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: c.dateUpload
                                        }
                                    },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                mangaScreenModel.reorderComposeQueue(
                                    {
                                        it.chapter.let { c ->
                                            c.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: c.dateUpload
                                        }
                                    },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
                NestedMenuItem(
                    text = {
                        Text(
                            text = stringResource(MR.strings.action_order_by_chapter_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                mangaScreenModel.reorderComposeQueue(
                                    { it.chapter.chapterNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                mangaScreenModel.reorderComposeQueue(
                                    { it.chapter.chapterNumber },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
            }

            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_sort),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { sortExpanded = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_cancel_all),
                        onClick = { mangaScreenModel.clearQueue() },
                    ),
                    settingsAction,
                ),
            )
        } else {
            // AniTenshi: the settings are there with an empty queue too
            AppBarActions(persistentListOf(settingsAction))
        }
    }

    /** AniTenshi: a tab's name with how many are queued, in gold on the tab that is open. */
    @Composable
    private fun DownloadTabText(text: String, count: Int, selected: Boolean) {
        val colors = MaterialTheme.colorScheme
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 20.dp)
                        .widthIn(min = 20.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.secondary else colors.surfaceContainerHighest)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.onSecondary else colors.onSurface,
                    )
                }
            }
        }
    }
}
