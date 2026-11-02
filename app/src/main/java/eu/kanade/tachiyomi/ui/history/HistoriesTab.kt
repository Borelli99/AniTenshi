package eu.kanade.tachiyomi.ui.history

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryViewModel
import eu.kanade.tachiyomi.ui.history.anime.animeHistoryTab
import eu.kanade.tachiyomi.ui.history.anime.resumeLastEpisodeSeenEvent
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryViewModel
import eu.kanade.tachiyomi.ui.history.manga.mangaHistoryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * History of anime and manga. In AniTenshi it is not in the bottom bar any more:
 * it opens from More (and from the history notification shortcut).
 */
data object HistoriesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 5u,
                title = stringResource(MR.strings.history),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastEpisodeSeenEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        // Hoisted for history tab's search bar
        val mangaHistoryViewModel = metroViewModel<MangaHistoryViewModel>()
        val mangaHistoryState by mangaHistoryViewModel.state.collectAsStateWithLifecycle()
        val mangaSearchQuery = mangaHistoryState.searchQuery
        val animeHistoryViewModel = metroViewModel<AnimeHistoryViewModel>()
        val animeHistoryState by animeHistoryViewModel.state.collectAsStateWithLifecycle()
        val animeSearchQuery = animeHistoryState.searchQuery

        TabbedScreen(
            titleRes = MR.strings.label_recent_manga,
            tabs = persistentListOf(
                animeHistoryTab(context, fromMore = true),
                mangaHistoryTab(context, fromMore = true),
            ),
            mangaSearchQuery = mangaSearchQuery,
            onChangeMangaSearchQuery = mangaHistoryViewModel::updateSearchQuery,
            animeSearchQuery = animeSearchQuery,
            onChangeAnimeSearchQuery = animeHistoryViewModel::search,
            animeExtensionsTabIndex = TAB_ANIME,
            mangaExtensionsTabIndex = TAB_MANGA,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}

private const val TAB_ANIME = 0
private const val TAB_MANGA = 1
