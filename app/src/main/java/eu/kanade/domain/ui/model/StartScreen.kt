package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.home.HomeTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

/**
 * The bottom bar entry the app opens on. AniTenshi's bottom bar is Home, Anime and Manga.
 * Values saved by older versions (updates, history, browse) fall back to Home.
 */
enum class StartScreen(val titleRes: StringResource, val tab: Tab) {
    HOME(MR.strings.label_home, HomeTab),
    ANIME(AYMR.strings.label_anime, AnimeLibraryTab),
    MANGA(AYMR.strings.manga, MangaLibraryTab),
}
