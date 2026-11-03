package eu.kanade.domain.items.episode.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.AnilistCandidate
import eu.kanade.tachiyomi.util.EpisodeAirDatesApi
import eu.kanade.tachiyomi.util.alternativeTitles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.model.Episode
import kotlin.math.floor

/**
 * AniTenshi: the AniList entry of an anime that isn't linked in Tracking, found by its titles (other titles in
 * its description, the custom one and the source's). Null when no entry clearly matches. Results, including "no match", are kept for the app
 * session so the same anime isn't searched again; a failed connection is not kept and is retried next time.
 * Read-only public search: no login needed.
 */
@Inject
class FindAnilistAnime(
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {

    suspend fun await(anime: Anime): AnilistCandidate? = mutex.withLock {
        // Other titles from the description first: Italian sites write the Japanese title there
        // ("Titolo Alternativo: Koori no Jouheki 2"), the one AniList knows best
        val titles = alternativeTitles(anime.description) + listOf(anime.title, anime.ogTitle)
        val key = "${anime.id}|${titles.joinToString("|")}"
        if (key in results) return@withLock results[key]

        val episodeCount = getEpisodesByAnimeId.await(anime.id).mapNotNull { it.wholeEpisodeNumber() }.maxOrNull() ?: 0
        try {
            api.findAnime(titles, episodeCount).also { results[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "AniTenshi: AniList search for anime ${anime.id}" }
            null
        }
    }

    companion object {
        private val api = EpisodeAirDatesApi()
        private val mutex = Mutex()
        private val results = mutableMapOf<String, AnilistCandidate?>()
    }
}

/** The episode number when it's a whole number above 0 (12, not 12.5), as used by AniList and MyAnimeList. */
internal fun Episode.wholeEpisodeNumber(): Int? =
    episodeNumber.takeIf { it > 0 && it == floor(it) }?.toInt()
