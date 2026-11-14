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
 * [LibraryPreferences.failedMangaUpdates]. A title leaves its list when a later update of it works.
 */
object FailedUpdates {

    private val json = Json { ignoreUnknownKeys = true }

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
        val old = read(preference)
        val attempted = attemptedIds.toSet()
        val state = FailedUpdatesState(
            time = if (failures.isNotEmpty()) System.currentTimeMillis() else old.time,
            items = old.items.filter { it.id !in attempted } + failures.distinctBy { it.id },
        )
        preference.set(json.encodeToString(FailedUpdatesState.serializer(), state))
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
