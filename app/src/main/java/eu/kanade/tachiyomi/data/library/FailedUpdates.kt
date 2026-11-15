package eu.kanade.tachiyomi.data.library

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.app.di.appGraph
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.domain.items.episode.model.NoEpisodesException
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.model.AnimeSourceNotInstalledException
import tachiyomi.domain.source.manga.model.SourceNotInstalledException
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** AniTenshi: why a title of the library couldn't be updated, in terms the app can explain. */
@Serializable
enum class FailedUpdateReason {
    /** The site's address doesn't exist (any more). */
    SITE_UNREACHABLE,

    /** The site exists but didn't answer in time or refused the connection. */
    SITE_NOT_RESPONDING,

    /** The site answered with an error. */
    SITE_ERROR,

    EXTENSION_MISSING,

    /** The site answered, with no chapters or episodes. */
    NOTHING_FOUND,

    OTHER,
}

/**
 * AniTenshi: a title of the library whose last update failed.
 *
 * @param detail what the reason is about: the site's host for [FailedUpdateReason.SITE_UNREACHABLE], "HTTP 403" for
 * [FailedUpdateReason.SITE_ERROR].
 * @param error the error as it came, for who wants the technical side.
 */
@Serializable
data class FailedUpdate(
    val id: Long,
    val title: String,
    val sourceId: Long,
    val sourceName: String,
    val reason: FailedUpdateReason,
    val detail: String? = null,
    val error: String? = null,
)

/** The failed updates of the anime or of the manga, and when the last of them happened. */
@Serializable
data class FailedUpdatesState(
    val time: Long = 0,
    val items: List<FailedUpdate> = emptyList(),
)

/**
 * AniTenshi: the titles the library update couldn't update, kept so that the app can show them (they used to go in a
 * text file opened from the notification) and retry them.
 *
 * Anime and manga each have their list, in [LibraryPreferences.failedAnimeUpdates] and
 * [LibraryPreferences.failedMangaUpdates]. A title leaves its list when a later update of it works, or when what
 * is written about it no longer holds (see [prune]).
 */
object FailedUpdates {

    private val json = Json { ignoreUnknownKeys = true }

    // The lists are read, changed and written back by the two update jobs and by [prune]
    private val lock = Any()

    fun decode(value: String): FailedUpdatesState {
        if (value.isBlank()) return FailedUpdatesState()
        return try {
            json.decodeFromString<FailedUpdatesState>(value)
        } catch (e: Exception) {
            FailedUpdatesState()
        }
    }

    fun read(preference: Preference<String>): FailedUpdatesState = decode(preference.get())

    /**
     * Records the outcome of an update that tried the titles [attemptedIds], of which [failures] failed: those
     * replace what was known of the titles tried, and the titles not tried stay as they were.
     */
    fun record(preference: Preference<String>, attemptedIds: Collection<Long>, failures: List<FailedUpdate>) {
        val attempted = attemptedIds.toSet()
        synchronized(lock) {
            val old = read(preference)
            val state = FailedUpdatesState(
                time = if (failures.isNotEmpty()) System.currentTimeMillis() else old.time,
                items = old.items.filter { it.id !in attempted } + failures.distinctBy { it.id },
            )
            preference.set(json.encodeToString(FailedUpdatesState.serializer(), state))
        }
    }

    /**
     * Takes off the lists what no longer holds:
     * - the titles that are no longer in the library. Removing a title, or moving it to another source (which
     *   makes a new title of it), leaves the old one out of every later update, so it would never leave its
     *   list by itself and would be reported as failed for good;
     * - "extension missing" for an extension that is there now.
     *
     * The titles the person silenced are forgotten too once they leave the library.
     */
    suspend fun prune(context: Context) {
        val graph = context.appGraph
        val preferences = graph.libraryPreferences
        val getAnime = graph.getAnime
        val getManga = graph.getManga
        val animeSources = graph.animeSourceManager
        val mangaSources = graph.mangaSourceManager

        // A season is updated with the title it belongs to, without being in the library itself
        suspend fun animeInLibrary(id: Long): Boolean {
            val anime = getAnime.await(id) ?: return false
            return anime.favorite || anime.parentId?.let { getAnime.await(it)?.favorite } == true
        }
        suspend fun mangaInLibrary(id: Long): Boolean = getManga.await(id)?.favorite == true

        val animeGone = read(preferences.failedAnimeUpdates).items
            .filter { failed ->
                !animeInLibrary(failed.id) ||
                    (
                        failed.reason == FailedUpdateReason.EXTENSION_MISSING &&
                            // Until the extensions have loaded every source looks missing
                            animeSources.isInitialized.value &&
                            animeSources.get(failed.sourceId) != null
                        )
            }
            .mapTo(HashSet()) { it.id }
        val mangaGone = read(preferences.failedMangaUpdates).items
            .filter { failed ->
                !mangaInLibrary(failed.id) ||
                    (
                        failed.reason == FailedUpdateReason.EXTENSION_MISSING &&
                            mangaSources.get(failed.sourceId) != null
                        )
            }
            .mapTo(HashSet()) { it.id }
        val mutedAnimeGone = preferences.mutedFailedAnimeUpdates.get()
            .filterTo(HashSet()) { id -> id.toLongOrNull()?.let { animeInLibrary(it) } != true }
        val mutedMangaGone = preferences.mutedFailedMangaUpdates.get()
            .filterTo(HashSet()) { id -> id.toLongOrNull()?.let { mangaInLibrary(it) } != true }

        synchronized(lock) {
            drop(preferences.failedAnimeUpdates, animeGone)
            drop(preferences.failedMangaUpdates, mangaGone)
            if (mutedAnimeGone.isNotEmpty()) {
                preferences.mutedFailedAnimeUpdates.set(preferences.mutedFailedAnimeUpdates.get() - mutedAnimeGone)
            }
            if (mutedMangaGone.isNotEmpty()) {
                preferences.mutedFailedMangaUpdates.set(preferences.mutedFailedMangaUpdates.get() - mutedMangaGone)
            }
        }
    }

    private fun drop(preference: Preference<String>, ids: Set<Long>) {
        if (ids.isEmpty()) return
        val old = read(preference)
        val items = old.items.filterNot { it.id in ids }
        if (items.size != old.items.size) {
            preference.set(json.encodeToString(FailedUpdatesState.serializer(), old.copy(items = items)))
        }
    }

    /** What went wrong with a title, from the exception its update ended with. */
    fun of(id: Long, title: String, sourceId: Long, sourceName: String, e: Throwable): FailedUpdate {
        val chain = generateSequence(e) { it.cause }.take(MAX_CAUSES).toList()
        val unknownHost = chain.firstOrNull { it is UnknownHostException }
        val http = chain.firstNotNullOfOrNull { it as? HttpException }
        val (reason, detail) = when {
            chain.any { it is SourceNotInstalledException || it is AnimeSourceNotInstalledException } ->
                FailedUpdateReason.EXTENSION_MISSING to null
            chain.any { it is NoChaptersException || it is NoEpisodesException } ->
                FailedUpdateReason.NOTHING_FOUND to null
            unknownHost != null -> {
                val host = HOST_IN_MESSAGE.find(unknownHost.message.orEmpty())?.groupValues?.get(1)
                FailedUpdateReason.SITE_UNREACHABLE to host
            }
            http != null ->
                FailedUpdateReason.SITE_ERROR to "HTTP ${http.code}"
            chain.any { it is InterruptedIOException || it is ConnectException || it is SSLException } ->
                FailedUpdateReason.SITE_NOT_RESPONDING to null
            else ->
                FailedUpdateReason.OTHER to null
        }
        return FailedUpdate(
            id = id,
            title = title,
            sourceId = sourceId,
            sourceName = sourceName,
            reason = reason,
            detail = detail,
            error = e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName,
        )
    }

    /** The failed updates the person wants to hear about: all but the titles they silenced. Anime first. */
    fun unmuted(preferences: LibraryPreferences): List<FailedUpdate> {
        val mutedAnime = preferences.mutedFailedAnimeUpdates.get()
        val mutedManga = preferences.mutedFailedMangaUpdates.get()
        return read(preferences.failedAnimeUpdates).items.filter { it.id.toString() !in mutedAnime } +
            read(preferences.failedMangaUpdates).items.filter { it.id.toString() !in mutedManga }
    }

    /** Updates again only the titles that failed. */
    fun retry(context: Context) {
        val preferences = context.appGraph.libraryPreferences
        if (read(preferences.failedAnimeUpdates).items.isNotEmpty()) {
            AnimeLibraryUpdateJob.startNow(context, onlyFailed = true)
        }
        if (read(preferences.failedMangaUpdates).items.isNotEmpty()) {
            MangaLibraryUpdateJob.startNow(context, onlyFailed = true)
        }
    }

    /**
     * Shows, updates or removes the "updates failed" notification, after an update or a change of what the person
     * wants to hear about. A tap opens the list in the app; it can retry, and switch itself off.
     */
    fun refreshNotification(context: Context) {
        val preferences = context.appGraph.libraryPreferences
        val failed = unmuted(preferences)
        if (failed.isEmpty() || !preferences.notifyFailedUpdates.get()) {
            NotificationReceiver.dismissNotification(context, Notifications.ID_LIBRARY_ERROR)
            return
        }

        val titles = failed.joinToString { it.title }
        context.notify(
            Notifications.ID_LIBRARY_ERROR,
            Notifications.CHANNEL_LIBRARY_ERROR,
        ) {
            setContentTitle(context.stringResource(MR.strings.notification_update_error, failed.size))
            setContentText(titles)
            setStyle(NotificationCompat.BigTextStyle().bigText(titles))
            setSmallIcon(R.drawable.ic_ani)
            setAutoCancel(true)
            setContentIntent(openPendingActivity(context))
            addAction(
                R.drawable.ic_refresh_24dp,
                context.stringResource(MR.strings.action_retry),
                NotificationReceiver.retryFailedUpdatesPendingBroadcast(context),
            )
            addAction(
                R.drawable.ic_close_24dp,
                context.stringResource(TLMR.strings.anitenshi_failed_updates_turn_off),
                NotificationReceiver.muteFailedUpdatesPendingBroadcast(context),
            )
        }
    }

    private fun openPendingActivity(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.INTENT_FAILED_UPDATES
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val MAX_CAUSES = 6

    /** `Unable to resolve host "example.com": No address associated with hostname` */
    private val HOST_IN_MESSAGE = Regex("\"([^\"]+)\"")
}
