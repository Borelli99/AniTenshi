package tachiyomi.domain.history.anime.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository

@Inject
class GetAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    suspend fun await(animeId: Long): List<AnimeHistory> {
        return repository.getHistoryByAnimeId(animeId)
    }

    /** AniTenshi: when each episode was last watched, for those watched from [since] (epoch milliseconds) on. */
    suspend fun awaitSeenTimesSince(since: Long): List<Long> {
        return repository.getSeenTimesSince(since)
    }

    fun subscribe(query: String): Flow<List<AnimeHistoryWithRelations>> {
        return repository.getAnimeHistory(query)
    }
}
