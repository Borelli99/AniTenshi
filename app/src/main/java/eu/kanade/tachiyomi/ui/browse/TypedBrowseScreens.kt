package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.anime.extension.animeExtensionsTab
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.migrateAnimeSourceTab
import eu.kanade.tachiyomi.ui.browse.anime.source.animeSourcesTab
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.mangaExtensionsTab
import eu.kanade.tachiyomi.ui.browse.manga.migration.sources.migrateMangaSourceTab
import eu.kanade.tachiyomi.ui.browse.manga.source.mangaSourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR

private const val TAB_SOURCES = 0
private const val TAB_EXTENSIONS = 1

/**
 * Browse for anime only: sources, extensions and migration.
 * Opened from the + button of the anime library.
 */
class AnimeBrowseScreen(private val toExtensions: Boolean = false) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // Hoisted for the extensions tab's search bar
        val extensionsViewModel = metroViewModel<AnimeExtensionsViewModel>()
        val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        val tabs = listOf(
            animeSourcesTab().copy(titleRes = MR.strings.label_sources),
            animeExtensionsTab(extensionsViewModel).copy(titleRes = MR.strings.label_extensions),
            migrateAnimeSourceTab().copy(titleRes = MR.strings.label_migration),
        ).map { it.copy(navigateUp = { navigator.pop() }) }
        val state = rememberPagerState(initialPage = if (toExtensions) TAB_EXTENSIONS else TAB_SOURCES) {
            tabs.size
        }

        TabbedScreen(
            titleRes = TLMR.strings.anitenshi_browse_anime,
            tabs = tabs,
            state = state,
            animeSearchQuery = extensionsSearchQuery,
            onChangeAnimeSearchQuery = extensionsViewModel::search,
            animeExtensionsTabIndex = TAB_EXTENSIONS,
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
class MangaBrowseScreen(private val toExtensions: Boolean = false) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // Hoisted for the extensions tab's search bar
        val extensionsViewModel = metroViewModel<MangaExtensionsViewModel>()
        val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        val tabs = listOf(
            mangaSourcesTab().copy(titleRes = MR.strings.label_sources),
            mangaExtensionsTab(extensionsViewModel).copy(titleRes = MR.strings.label_extensions),
            migrateMangaSourceTab().copy(titleRes = MR.strings.label_migration),
        ).map { it.copy(navigateUp = { navigator.pop() }) }
        val state = rememberPagerState(initialPage = if (toExtensions) TAB_EXTENSIONS else TAB_SOURCES) {
            tabs.size
        }

        TabbedScreen(
            titleRes = TLMR.strings.anitenshi_browse_manga,
            tabs = tabs,
            state = state,
            mangaSearchQuery = extensionsSearchQuery,
            onChangeMangaSearchQuery = extensionsViewModel::search,
            mangaExtensionsTabIndex = TAB_EXTENSIONS,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
        }
    }
}
