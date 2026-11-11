package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.browse.anime.animeSourcesAndExtensionsTab
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.migrateAnimeSourceTab
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.manga.mangaSourcesAndExtensionsTab
import eu.kanade.tachiyomi.ui.browse.manga.migration.sources.migrateMangaSourceTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR

// AniTenshi: sources and extensions share the first page; the second is migration
private const val TAB_SOURCES = 0
private const val TAB_COUNT = 2

/**
 * Browse for anime only: sources, extensions and migration.
 * Opened from the + button of the anime library.
 */
class AnimeBrowseScreen(
    private val toExtensions: Boolean = false,
    // AniTenshi: what to look for in the list when it opens, for a title whose extension isn't installed
    private val extensionSearch: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // Hoisted for the extensions tab's search bar
        val extensionsViewModel = metroViewModel<AnimeExtensionsViewModel>()
        val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        // Only the first time: afterwards the search bar is the person's
        var extensionSearchDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!extensionSearchDone && !extensionSearch.isNullOrBlank()) {
                extensionsViewModel.search(extensionSearch)
            }
            extensionSearchDone = true
        }

        // Extensions now live on the sources page, so [toExtensions] opens it as well
        val state = rememberPagerState(initialPage = TAB_SOURCES) { TAB_COUNT }

        val tabs = listOf(
            animeSourcesAndExtensionsTab(extensionsViewModel),
            migrateAnimeSourceTab().copy(titleRes = MR.strings.label_migration),
        ).map { it.copy(navigateUp = { navigator.pop() }) }

        TabbedScreen(
            titleRes = TLMR.strings.anitenshi_browse_anime,
            tabs = tabs,
            state = state,
            animeSearchQuery = extensionsSearchQuery,
            onChangeAnimeSearchQuery = extensionsViewModel::search,
            animeExtensionsTabIndex = TAB_SOURCES,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
        }
    }
}

/**
 * Browse for manga only: sources, extensions and migration.
 * Opened from the + button of the manga library.
 */
class MangaBrowseScreen(
    private val toExtensions: Boolean = false,
    // AniTenshi: what to look for in the list when it opens, for a title whose extension isn't installed
    private val extensionSearch: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // Hoisted for the extensions tab's search bar
        val extensionsViewModel = metroViewModel<MangaExtensionsViewModel>()
        val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        // Only the first time: afterwards the search bar is the person's
        var extensionSearchDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!extensionSearchDone && !extensionSearch.isNullOrBlank()) {
                extensionsViewModel.search(extensionSearch)
            }
            extensionSearchDone = true
        }

        // Extensions now live on the sources page, so [toExtensions] opens it as well
        val state = rememberPagerState(initialPage = TAB_SOURCES) { TAB_COUNT }

        val tabs = listOf(
            mangaSourcesAndExtensionsTab(extensionsViewModel),
            migrateMangaSourceTab().copy(titleRes = MR.strings.label_migration),
        ).map { it.copy(navigateUp = { navigator.pop() }) }

        TabbedScreen(
            titleRes = TLMR.strings.anitenshi_browse_manga,
            tabs = tabs,
            state = state,
            mangaSearchQuery = extensionsSearchQuery,
            onChangeMangaSearchQuery = extensionsViewModel::search,
            mangaExtensionsTabIndex = TAB_SOURCES,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
        }
    }
}
