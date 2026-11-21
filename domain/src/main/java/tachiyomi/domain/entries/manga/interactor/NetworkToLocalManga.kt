package tachiyomi.domain.entries.manga.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.MangaRemoteUpdate
import tachiyomi.domain.entries.manga.repository.MangaRepository
import kotlin.coroutines.cancellation.CancellationException

@Inject
class NetworkToLocalManga(
    private val mangaRepository: MangaRepository,
) {

    // AniTenshi: one save at a time. The same title opened twice at once (a double tap) found it missing both times
    // and was added twice: the second broke the rule of one entry per source and address, and the app closed.
    suspend fun await(manga: Manga): Manga = saveLock.withLock {
        val localManga = getManga(manga.url, manga.source)
        when {
            localManga == null -> {
                val id = try {
                    insertManga(manga)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Saved meanwhile by something that doesn't go through here (a restore): take that one
                    return@withLock getManga(manga.url, manga.source) ?: throw e
                }
                manga.copy(id = id!!)
            }

            manga.source == LOCAL_MANGA_SOURCE_ID -> {
                val mergedManga = mergeLocalManga(localManga, manga)
                if (mergedManga != localManga) {
                    mangaRepository.updateRemote(mergedManga.toDatabaseUpdate())
                }
                mergedManga
            }

            !localManga.favorite -> {
                // if the manga isn't a favorite, set its display title from source
                // if it later becomes a favorite, updated title will go to db
                localManga.copy(ogTitle = manga.title)
            }

            else -> {
                localManga
            }
        }
    }

    // KMK -->
    suspend fun getLocal(manga: Manga): Manga = if (manga.id <= 0) {
        await(manga)
    } else {
        manga
    }
    // KMK <--

    private suspend fun getManga(url: String, sourceId: Long): Manga? {
        return mangaRepository.getMangaByUrlAndSourceId(url, sourceId)
    }

    private suspend fun insertManga(manga: Manga): Long? {
        return mangaRepository.insertManga(manga)
    }

    private fun mergeLocalManga(
        localManga: Manga,
        sourceManga: Manga,
    ): Manga {
        return localManga.copy(
            ogTitle = sourceManga.ogTitle,
            ogArtist = sourceManga.ogArtist ?: localManga.ogArtist,
            ogAuthor = sourceManga.ogAuthor ?: localManga.ogAuthor,
            ogDescription = sourceManga.ogDescription ?: localManga.ogDescription,
            ogGenre = sourceManga.ogGenre ?: localManga.ogGenre,
            ogStatus = sourceManga.ogStatus,
            thumbnailUrl = sourceManga.thumbnailUrl ?: localManga.thumbnailUrl,
            initialized = sourceManga.initialized || localManga.initialized,
        )
    }

    private fun Manga.toDatabaseUpdate(): MangaRemoteUpdate {
        return MangaRemoteUpdate(
            id = id,
            title = ogTitle,
            artist = ogArtist,
            author = ogAuthor,
            description = ogDescription,
            genre = ogGenre,
            status = ogStatus,
            thumbnailUrl = thumbnailUrl,
            updateStrategy = updateStrategy,
            memo = memo,
            initialized = initialized,
            coverLastModified = coverLastModified,
        )
    }

    private companion object {
        const val LOCAL_MANGA_SOURCE_ID = 0L

        val saveLock = Mutex()
    }
}
