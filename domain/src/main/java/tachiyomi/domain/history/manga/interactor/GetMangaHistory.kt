package tachiyomi.domain.history.manga.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.manga.model.MangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.history.manga.repository.MangaHistoryRepository

@Inject
class GetMangaHistory(
    private val repository: MangaHistoryRepository,
) {

    suspend fun await(mangaId: Long): List<MangaHistory> {
        return repository.getHistoryByMangaId(mangaId)
    }

    /** AniTenshi: when each chapter was last read, for those read from [since] (epoch milliseconds) on. */
    suspend fun awaitReadTimesSince(since: Long): List<Long> {
        return repository.getReadTimesSince(since)
    }

    fun subscribe(query: String): Flow<List<MangaHistoryWithRelations>> {
        return repository.getMangaHistory(query)
    }
}
