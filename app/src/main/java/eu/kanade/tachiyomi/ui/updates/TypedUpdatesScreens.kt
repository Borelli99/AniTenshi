package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.SingleTabScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.updates.anime.animeUpdatesTab
import eu.kanade.tachiyomi.ui.updates.manga.mangaUpdatesTab
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * New episodes of the anime in the library. Opened from the bell in the anime library.
 */
class AnimeUpdatesScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        SingleTabScreen(
            title = stringResource(TLMR.strings.anitenshi_updates_anime),
            tab = animeUpdatesTab(context, fromMore = true),
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}

/**
 * New chapters of the manga in the library. Opened from the bell in the manga library.
 */
class MangaUpdatesScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        SingleTabScreen(
            title = stringResource(TLMR.strings.anitenshi_updates_manga),
            tab = mangaUpdatesTab(context, fromMore = true),
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
