package eu.kanade.domain.items.episode.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.items.isUnknownUploadDate
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.EpisodeAirDatesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.interactor.UpdateEpisode
import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks

/**
 * AniTenshi: gives the episodes whose extension sends no release date their original (Japanese) air date,
 * from AniList / MyAnimeList. The anime's entry there is the one linked in Tracking, or else the one found
 * by its title (only when it clearly matches). Read-only public data: no login, nothing added to any list.
 * Episodes with a date from the extension, or a date set by the user, are never touched.
 */
@Inject
class FillEpisodeAirDates(
    private val getAnime: GetAnime,
    private val findAnilistAnime: FindAnilistAnime,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    private val getAnimeTracks: GetAnimeTracks,
    private val updateEpisode: UpdateEpisode,
) {

    suspend fun await(animeId: Long) = withIOContext {
        mutex.withLock {
            try {
                fill(animeId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "AniTenshi: air dates for anime $animeId" }
            }
        }
    }

    private suspend fun fill(animeId: Long) {
        val episodes = getEpisodesByAnimeId.await(animeId)
        val missing = episodes.filter {
            it.dateUploadOverride <= 0L && isUnknownUploadDate(it.dateUpload, it.dateFetch) && it.wholeEpisodeNumber() != null
        }
        if (missing.isEmpty()) return

        val tracks = getAnimeTracks.await(animeId)
        var anilistId = tracks.firstOrNull { it.trackerId == TrackerManager.ANILIST }?.remoteId?.takeIf { it > 0 }
        var malId = tracks.firstOrNull { it.trackerId == MYANIMELIST_TRACKER_ID }?.remoteId?.takeIf { it > 0 }

        if (anilistId == null && malId == null) {
            // Not linked in Tracking: the AniList entry found by the anime's titles
            val anime = getAnime.await(animeId) ?: return
            val match = findAnilistAnime.await(anime) ?: return
            anilistId = match.id
            malId = match.malId
        }

        // Ask the sites once per app session for the same episodes and entry
        val key = "$animeId|$anilistId|$malId|${missing.map { it.id }.sorted().hashCode()}"
        if (!attempted.add(key)) return

        val dates = api.load(anilistId = anilistId, malId = malId)
        if (dates.isEmpty()) return

        val updates = missing.mapNotNull { episode ->
            dates[episode.wholeEpisodeNumber()]?.let { EpisodeUpdate(id = episode.id, dateUpload = it) }
        }
        if (updates.isNotEmpty()) updateEpisode.awaitAll(updates)
    }

    companion object {
        /** MyAnimeList's id in [TrackerManager] (`MyAnimeList(1L)`). */
        private const val MYANIMELIST_TRACKER_ID = 1L

        private val api = EpisodeAirDatesApi()
        private val mutex = Mutex()
        private val attempted = mutableSetOf<String>()
    }
}
