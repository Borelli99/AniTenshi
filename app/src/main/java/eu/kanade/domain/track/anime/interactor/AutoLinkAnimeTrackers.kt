package eu.kanade.domain.track.anime.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.items.episode.interactor.FindAnilistAnime
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks

/**
 * AniTenshi: when an anime is added to the library, links it by itself to AniList and MyAnimeList (the ones
 * you're signed in to), using the AniList entry found by its title. Like a manual link, this puts the anime
 * in your list there (as "Plan to watch", or "Watching" if you've seen episodes). Nothing is linked when no
 * entry clearly matches.
 */
@Inject
class AutoLinkAnimeTrackers(
    private val trackerManager: TrackerManager,
    private val getAnimeTracks: GetAnimeTracks,
    private val addTracks: AddAnimeTracks,
    private val findAnilistAnime: FindAnilistAnime,
) {

    /**
     * @property linked names of the sites just linked.
     * @property incomplete true when a site you're signed in to is still not linked (no clear match or an error).
     */
    data class Result(val linked: List<String>, val incomplete: Boolean)

    suspend fun await(anime: Anime): Result = withIOContext {
        val tracks = getAnimeTracks.await(anime.id)
        val toLink = listOf<Tracker>(trackerManager.aniList, trackerManager.myAnimeList)
            .filter { tracker -> tracker.isLoggedIn && tracks.none { it.trackerId == tracker.id } }
        if (toLink.isEmpty()) return@withIOContext Result(emptyList(), incomplete = false)

        val match = findAnilistAnime.await(anime) ?: return@withIOContext Result(emptyList(), incomplete = true)

        val linked = mutableListOf<String>()
        var incomplete = false
        toLink.forEach { tracker ->
            val remoteId = if (tracker.id == TrackerManager.ANILIST) match.id else match.malId
            if (remoteId == null) {
                incomplete = true
                return@forEach
            }
            try {
                // "id:" asks the site for that exact entry, with all the details a manual search gives
                val item = tracker.animeService.searchAnime("id:$remoteId")
                    .firstOrNull { it.remote_id == remoteId }
                if (item == null) {
                    incomplete = true
                    return@forEach
                }
                item.anime_id = anime.id
                addTracks.bind(tracker.animeService, item, anime)
                linked += tracker.name
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "AniTenshi: automatic ${tracker.name} link for anime ${anime.id}" }
                incomplete = true
            }
        }
        Result(linked, incomplete)
    }
}
