package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.entries.manga.model.toDomainManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import kotlin.math.max
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.entries.EntryCover
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.asMangaCover
import tachiyomi.domain.history.anime.interactor.GetNextEpisodes
import tachiyomi.domain.history.manga.interactor.GetNextChapters
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager

/**
 * AniTenshi Home: what to continue (anime and manga history) and the latest releases
 * of the pinned anime and manga sources.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HomeViewModel(
    private val getLibraryAnime: GetLibraryAnime,
    private val getLibraryManga: GetLibraryManga,
    private val getNextEpisodes: GetNextEpisodes,
    private val getNextChapters: GetNextChapters,
    private val getAnime: GetAnime,
    private val getManga: GetManga,
    private val networkToLocalAnime: NetworkToLocalAnime,
    private val networkToLocalManga: NetworkToLocalManga,
    private val animeSourceManager: AnimeSourceManager,
    private val mangaSourceManager: MangaSourceManager,
    private val sourcePreferences: SourcePreferences,
    private val uiPreferences: UiPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** Bumped by [refresh] to fetch the latest releases again. */
    private val refreshTrigger = MutableStateFlow(0)

    init {
        // Continue watching: library anime with episodes left, unless hidden by the user.
        // An anime leaves the row when everything is watched and comes back with a new episode.
        viewModelScope.launch {
            combine(
                getLibraryAnime.subscribe(),
                uiPreferences.homeHiddenContinueAnime.changes(),
            ) { library, hidden ->
                // The library has one row per category: keep one per anime
                val entries = library.distinctBy { it.id }
                val visible = entries
                    .filter { it.unseenCount > 0 && it.id.toString() !in hidden }
                    .sortedByDescending { max(it.lastSeen, it.episodeFetchedAt) }
                    .take(MAX_CONTINUE_ITEMS)
                    .map { it.toContinueEntry(withNext = true) }
                val hiddenEntries = entries
                    .filter { it.id.toString() in hidden }
                    .sortedBy { it.anime.title.lowercase() }
                    .map { it.toContinueEntry() }
                visible to hiddenEntries
            }
                .collectLatest { (visible, hiddenEntries) ->
                    _state.update { it.copy(continueAnime = visible, hiddenAnime = hiddenEntries) }
                }
        }
        // Continue reading: the same for manga and unread chapters
        viewModelScope.launch {
            combine(
                getLibraryManga.subscribe(),
                uiPreferences.homeHiddenContinueManga.changes(),
            ) { library, hidden ->
                val entries = library.distinctBy { it.id }
                val visible = entries
                    .filter { it.unreadCount > 0 && it.id.toString() !in hidden }
                    .sortedByDescending { max(it.lastRead, it.chapterFetchedAt) }
                    .take(MAX_CONTINUE_ITEMS)
                    .map { it.toContinueEntry(withNext = true) }
                val hiddenEntries = entries
                    .filter { it.id.toString() in hidden }
                    .sortedBy { it.manga.title.lowercase() }
                    .map { it.toContinueEntry() }
                visible to hiddenEntries
            }
                .collectLatest { (visible, hiddenEntries) ->
                    _state.update { it.copy(continueManga = visible, hiddenManga = hiddenEntries) }
                }
        }
        observeLatestAnime()
        observeLatestManga()
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    private fun observeLatestAnime() {
        viewModelScope.launch {
            combine(
                animeSourceManager.isInitialized,
                animeSourceManager.sources,
                sourcePreferences.pinnedAnimeSources.changes(),
                sourcePreferences.disabledAnimeSources.changes(),
                refreshTrigger,
            ) { initialized, sources, pinned, disabled, refresh ->
                // Wait for the extensions to be loaded, or every pinned source would look missing
                if (!initialized) return@combine null
                val pinnedSources = sources
                    .filterIsInstance<AnimeCatalogueSource>()
                    .filter { it.id.toString() in pinned && it.id.toString() !in disabled }
                    .sortedBy { it.name.lowercase() }
                pinnedSources to refresh
            }
                .filterNotNull()
                .distinctUntilChanged { old, new ->
                    old.second == new.second && old.first.map { it.id } == new.first.map { it.id }
                }
                .collectLatest { (sources, _) ->
                    _state.update { state ->
                        state.copy(latestAnime = sources.map { it.toLoadingRow<Anime>() })
                    }
                    coroutineScope {
                        sources.map { source ->
                            async {
                                val row = loadAnimeRow(source)
                                _state.update { state ->
                                    state.copy(
                                        latestAnime = state.latestAnime?.map {
                                            if (it.sourceId == row.sourceId) row else it
                                        },
                                    )
                                }
                            }
                        }.awaitAll()
                    }
                }
        }
    }

    private fun observeLatestManga() {
        viewModelScope.launch {
            combine(
                mangaSourceManager.catalogueSources,
                sourcePreferences.pinnedMangaSources.changes(),
                sourcePreferences.disabledMangaSources.changes(),
                refreshTrigger,
            ) { sources, pinned, disabled, refresh ->
                val pinnedSources = sources
                    .filter { it.id.toString() in pinned && it.id.toString() !in disabled }
                    .sortedBy { it.name.lowercase() }
                pinnedSources to refresh
            }
                .distinctUntilChanged { old, new ->
                    old.second == new.second && old.first.map { it.id } == new.first.map { it.id }
                }
                .collectLatest { (sources, _) ->
                    _state.update { state ->
                        state.copy(latestManga = sources.map { it.toLoadingRow<Manga>() })
                    }
                    coroutineScope {
                        sources.map { source ->
                            async {
                                val row = loadMangaRow(source)
                                _state.update { state ->
                                    state.copy(
                                        latestManga = state.latestManga?.map {
                                            if (it.sourceId == row.sourceId) row else it
                                        },
                                    )
                                }
                            }
                        }.awaitAll()
                    }
                }
        }
    }

    private suspend fun loadAnimeRow(source: AnimeCatalogueSource): LatestRow<Anime> {
        val loading = source.toLoadingRow<Anime>()
        return try {
            val page = withIOContext {
                if (source.supportsLatest) source.getLatestUpdates(1) else source.getPopularAnime(1)
            }
            loading.copy(items = page.animes.map { it.toDomainAnime(source.id) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Home: could not load the latest anime of ${source.name}" }
            loading.copy(items = emptyList(), error = e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun loadMangaRow(source: CatalogueSource): LatestRow<Manga> {
        val loading = source.toLoadingRow<Manga>()
        return try {
            val page = withIOContext {
                if (source.supportsLatest) source.getLatestUpdates(1) else source.getPopularManga(1)
            }
            loading.copy(items = page.mangas.map { it.toDomainManga(source.id) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Home: could not load the latest manga of ${source.name}" }
            loading.copy(items = emptyList(), error = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun <T> AnimeCatalogueSource.toLoadingRow(): LatestRow<T> = LatestRow(
        sourceId = id,
        name = name,
        language = LocaleHelper.getLocalizedDisplayName(lang),
        supportsLatest = supportsLatest,
    )

    private fun <T> CatalogueSource.toLoadingRow(): LatestRow<T> = LatestRow(
        sourceId = id,
        name = name,
        language = LocaleHelper.getLocalizedDisplayName(lang),
        supportsLatest = supportsLatest,
    )

    /** Keeps a title from a source in sync with the database (in library, cover). */
    @Composable
    fun anime(initial: Anime): State<Anime> {
        return produceState(initialValue = initial) {
            getAnime.subscribe(initial.url, initial.source)
                .collectLatest { value = it ?: initial }
        }
    }

    @Composable
    fun manga(initial: Manga): State<Manga> {
        return produceState(initialValue = initial) {
            getManga.subscribe(initial.url, initial.source)
                .collectLatest { value = it ?: initial }
        }
    }

    /** Saves a title from a source in the database, so its page can be opened. */
    suspend fun localAnime(anime: Anime): Anime = withIOContext { networkToLocalAnime.getLocal(anime) }

    suspend fun localManga(manga: Manga): Manga = withIOContext { networkToLocalManga.getLocal(manga) }

    /** The first episode left to watch, or null when there is nothing left. */
    suspend fun nextEpisode(animeId: Long): Episode? = withIOContext {
        getNextEpisodes.await(animeId, onlyUnseen = true).firstOrNull()
    }

    /** The first chapter left to read, or null when there is nothing left. */
    suspend fun nextChapter(mangaId: Long): Chapter? = withIOContext {
        getNextChapters.await(mangaId, onlyUnread = true).firstOrNull()
    }

    fun hideAnime(animeId: Long) {
        uiPreferences.homeHiddenContinueAnime.getAndSet { it + animeId.toString() }
    }

    fun showAnime(animeId: Long) {
        uiPreferences.homeHiddenContinueAnime.getAndSet { it - animeId.toString() }
    }

    fun hideManga(mangaId: Long) {
        uiPreferences.homeHiddenContinueManga.getAndSet { it + mangaId.toString() }
    }

    fun showManga(mangaId: Long) {
        uiPreferences.homeHiddenContinueManga.getAndSet { it - mangaId.toString() }
    }

    /** [withNext]: also look up the number of the episode to watch next (only for the visible row). */
    private suspend fun LibraryAnime.toContinueEntry(withNext: Boolean = false): ContinueEntry {
        val next = if (withNext) nextEpisode(id) else null
        return ContinueEntry(
            id = id,
            title = anime.title,
            cover = anime.asAnimeCover(),
            remaining = unseenCount,
            nextNumber = next?.episodeNumber,
            progress = next?.takeIf { it.totalSeconds > 0 && it.lastSecondSeen > 0 }
                ?.let { (it.lastSecondSeen.toFloat() / it.totalSeconds).coerceIn(0f, 1f) },
        )
    }

    /** [withNext]: also look up the number of the chapter to read next (only for the visible row). */
    private suspend fun LibraryManga.toContinueEntry(withNext: Boolean = false) = ContinueEntry(
        id = id,
        title = manga.title,
        cover = manga.asMangaCover(),
        remaining = unreadCount,
        nextNumber = if (withNext) nextChapter(id)?.chapterNumber else null,
    )

    companion object {
        private const val MAX_CONTINUE_ITEMS = 30
    }
}

@Immutable
data class HomeState(
    val continueAnime: List<ContinueEntry> = emptyList(),
    val continueManga: List<ContinueEntry> = emptyList(),
    /** Library anime the user hid from "Continue watching". */
    val hiddenAnime: List<ContinueEntry> = emptyList(),
    /** Library manga the user hid from "Continue reading". */
    val hiddenManga: List<ContinueEntry> = emptyList(),
    /** One row per pinned anime source; null until the sources are loaded. */
    val latestAnime: List<LatestRow<Anime>>? = null,
    /** One row per pinned manga source; null until the sources are loaded. */
    val latestManga: List<LatestRow<Manga>>? = null,
)

/** The latest releases of one source. [items] is null while loading. */
@Immutable
data class LatestRow<T>(
    val sourceId: Long,
    val name: String,
    val language: String,
    val supportsLatest: Boolean,
    val items: List<T>? = null,
    val error: String? = null,
)

/** A library title in "Continue watching" or "Continue reading". */
@Immutable
data class ContinueEntry(
    val id: Long,
    val title: String,
    val cover: EntryCover,
    /** Episodes left to watch or chapters left to read. */
    val remaining: Long,
    /** Number of the next episode or chapter (as the source numbers it), when known. */
    val nextNumber: Double? = null,
    /** How far into the next episode the user got (0..1), when it was started. */
    val progress: Float? = null,
)
