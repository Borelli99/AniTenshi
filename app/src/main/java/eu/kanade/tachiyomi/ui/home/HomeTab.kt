package eu.kanade.tachiyomi.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.home.HomeContent
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.anime.interactor.GetRemoteAnime
import tachiyomi.domain.source.manga.interactor.GetRemoteManga
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * First entry of the bottom bar, shown with the AniTenshi logo.
 */
data object HomeTab : Tab {
    private fun readResolve(): Any = HomeTab

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_home),
                icon = painterResource(R.drawable.ic_ani),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<HomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // AniTenshi: a title from a source is saved before its page opens. A second tap while that happens is
        // ignored, and if saving fails a message says so instead of the app closing.
        var isOpening by remember { mutableStateOf(false) }
        fun openFromSource(open: suspend () -> Unit) {
            if (isOpening) return
            isOpening = true
            scope.launch {
                try {
                    open()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                    context.toast(TLMR.strings.anitenshi_home_open_failed)
                } finally {
                    isOpening = false
                }
            }
        }

        HomeContent(
            state = state,
            snackbarHostState = snackbarHostState,
            getAnime = { viewModel.anime(it) },
            getManga = { viewModel.manga(it) },
            onClickContinueAnime = { entry ->
                // Plays the first episode left; without one, opens the anime page
                scope.launch {
                    val episode = viewModel.nextEpisode(entry.id)
                    if (episode != null) {
                        val extPlayer = context.appGraph.playerPreferences.alwaysUseExternalPlayer().get()
                        MainActivity.startPlayerActivity(context, episode.animeId, episode.id, extPlayer)
                    } else {
                        navigator.push(AnimeScreen(entry.id))
                    }
                }
            },
            onOpenAnime = { navigator.push(AnimeScreen(it)) },
            onHideAnime = viewModel::hideAnime,
            onShowAnime = viewModel::showAnime,
            onClickContinueManga = { entry ->
                // Opens the first chapter left; without one, opens the manga page
                scope.launch {
                    val chapter = viewModel.nextChapter(entry.id)
                    if (chapter != null) {
                        context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                    } else {
                        navigator.push(MangaScreen(entry.id))
                    }
                }
            },
            onOpenManga = { navigator.push(MangaScreen(it)) },
            onHideManga = viewModel::hideManga,
            onShowManga = viewModel::showManga,
            onClickAnimeSource = { row ->
                navigator.push(
                    BrowseAnimeSourceScreen(
                        row.sourceId,
                        if (row.supportsLatest) GetRemoteAnime.QUERY_LATEST else GetRemoteAnime.QUERY_POPULAR,
                    ),
                )
            },
            onClickMangaSource = { row ->
                navigator.push(
                    BrowseMangaSourceScreen(
                        row.sourceId,
                        if (row.supportsLatest) GetRemoteManga.QUERY_LATEST else GetRemoteManga.QUERY_POPULAR,
                    ),
                )
            },
            onClickAnime = { anime ->
                openFromSource {
                    val local = viewModel.localAnime(anime)
                    navigator.push(AnimeScreen(local.id, true))
                }
            },
            onClickManga = { manga ->
                openFromSource {
                    val local = viewModel.localManga(manga)
                    navigator.push(MangaScreen(local.id, true))
                }
            },
            onClickRefresh = viewModel::refresh,
            onClickMore = { navigator.push(MoreTab) },
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
