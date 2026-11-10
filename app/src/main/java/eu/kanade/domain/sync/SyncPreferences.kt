package eu.kanade.domain.sync

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.sync.models.SyncTriggerOptions
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.util.UUID

/** Made by @kaiserbh. Huge thx for making this feature. **/

@Inject
@SingleIn(AppScope::class)
@Suppress("TooManyFunctions")
class SyncPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun clientHost() = preferenceStore.getString("sync_client_host", "https://sync.tachiyomi.org")
    fun clientAPIKey() = preferenceStore.getString("sync_client_api_key", "")
    fun lastSyncTimestamp() = preferenceStore.getLong(Preference.appStateKey("last_sync_timestamp"), 0L)

    /**
     * AniTenshi: why the latest sync failed, blank when it went well. Automatic syncs fail quietly, so the
     * status row in "More" and the sync settings show it.
     */
    fun lastSyncError() = preferenceStore.getString(Preference.appStateKey("last_sync_error"), "")

    fun lastSyncEtag() = preferenceStore.getString("sync_etag", "")

    fun syncInterval() = preferenceStore.getInt("sync_interval", 0)
    fun syncService() = preferenceStore.getInt("sync_service", 0)

    /**
     * AniTenshi: Google Drive is connected through Google Play services. The app keeps no token: this only says
     * that the person signed in on this device (see GoogleDriveService).
     */
    fun googleDriveConnected() = preferenceStore.getBoolean(
        Preference.appStateKey("google_drive_connected"),
        false,
    )

    /** AniTenshi: the Google account Drive is connected with (its email), blank when unknown or not connected. */
    fun googleDriveAccount() = preferenceStore.getString(
        Preference.appStateKey("google_drive_account"),
        "",
    )

    // AniTenshi: tokens of the old sign-in through the browser (up to 1.0.16). Only read by the migration to the
    // Play services sign-in, which deletes them.
    fun googleDriveAccessToken() = preferenceStore.getString(
        Preference.appStateKey("google_drive_access_token"),
        "",
    )

    fun googleDriveRefreshToken() = preferenceStore.getString(
        Preference.appStateKey("google_drive_refresh_token"),
        "",
    )

    fun uniqueDeviceID(): String {
        val uniqueIDPreference = preferenceStore.getString(Preference.appStateKey("unique_device_id"), "")

        // Retrieve the current value of the preference
        var uniqueID = uniqueIDPreference.get()
        if (uniqueID.isBlank()) {
            uniqueID = UUID.randomUUID().toString()
            uniqueIDPreference.set(uniqueID)
        }

        return uniqueID
    }

    fun isSyncEnabled(): Boolean {
        return syncService().get() != 0
    }

    /**
     * AniTenshi: true when the chosen service can sync without asking anything: signed in to Google Drive,
     * or a SyncYomi key set. Automatic syncs wait for this, so they don't fail on every app open.
     */
    fun isSyncReady(): Boolean {
        return when (syncService().get()) {
            SYNC_SERVICE_SYNCYOMI -> clientAPIKey().get().isNotBlank()
            SYNC_SERVICE_GOOGLE_DRIVE -> googleDriveConnected().get()
            else -> false
        }
    }

    fun getSyncSettings(): SyncSettings {
        return SyncSettings(
            libraryEntries = preferenceStore.getBoolean("library_entries", true).get(),
            animelibEntries = preferenceStore.getBoolean("anime_lib_entries", true).get(),
            categories = preferenceStore.getBoolean("categories", true).get(),
            animeCategories = preferenceStore.getBoolean("anime_categories", true).get(),
            chapters = preferenceStore.getBoolean("chapters", true).get(),
            episodes = preferenceStore.getBoolean("episodes", true).get(),
            tracking = preferenceStore.getBoolean("tracking", true).get(),
            animeTracking = preferenceStore.getBoolean("anime_tracking", true).get(),
            history = preferenceStore.getBoolean("history", true).get(),
            animeHistory = preferenceStore.getBoolean("anime_history", true).get(),
            appSettings = preferenceStore.getBoolean("appSettings", true).get(),
            sourceSettings = preferenceStore.getBoolean("sourceSettings", true).get(),
            privateSettings = preferenceStore.getBoolean("privateSettings", true).get(),
        )
    }

    fun setSyncSettings(syncSettings: SyncSettings) {
        preferenceStore.getBoolean("library_entries", true).set(syncSettings.libraryEntries)
        preferenceStore.getBoolean("anime_lib_entries", true).set(syncSettings.animelibEntries)
        preferenceStore.getBoolean("categories", true).set(syncSettings.categories)
        preferenceStore.getBoolean("anime_categories", true).set(syncSettings.animeCategories)
        preferenceStore.getBoolean("chapters", true).set(syncSettings.chapters)
        preferenceStore.getBoolean("episodes", true).set(syncSettings.episodes)
        preferenceStore.getBoolean("tracking", true).set(syncSettings.tracking)
        preferenceStore.getBoolean("anime_tracking", true).set(syncSettings.animeTracking)
        // AniTenshi: the manga history was saved in the anime key and never in its own
        preferenceStore.getBoolean("history", true).set(syncSettings.history)
        preferenceStore.getBoolean("anime_history", true).set(syncSettings.animeHistory)
        preferenceStore.getBoolean("appSettings", true).set(syncSettings.appSettings)
        preferenceStore.getBoolean("sourceSettings", true).set(syncSettings.sourceSettings)
        preferenceStore.getBoolean("privateSettings", true).set(syncSettings.privateSettings)
    }

    fun getSyncTriggerOptions(): SyncTriggerOptions {
        return SyncTriggerOptions(
            // AniTenshi: on by default, so a connected account stays in sync without setting anything
            syncOnAppStart = preferenceStore.getBoolean("sync_on_app_start", true).get(),
            syncOnAppResume = preferenceStore.getBoolean("sync_on_app_resume", true).get(),
            syncOnLibraryUpdate = preferenceStore.getBoolean("sync_on_library_update", true).get(),
            syncOnProgress = preferenceStore.getBoolean("sync_on_progress", true).get(),

            // Manga
            syncOnChapterRead = preferenceStore.getBoolean("sync_on_chapter_read", false).get(),
            syncOnChapterOpen = preferenceStore.getBoolean("sync_on_chapter_open", false).get(),

            // Anime
            syncOnEpisodeSeen = preferenceStore.getBoolean("sync_on_episode_seen", false).get(),
            syncOnEpisodeOpen = preferenceStore.getBoolean("sync_on_episode_open", false).get(),
        )
    }

    fun setSyncTriggerOptions(syncTriggerOptions: SyncTriggerOptions) {
        preferenceStore.getBoolean("sync_on_app_start", true)
            .set(syncTriggerOptions.syncOnAppStart)
        preferenceStore.getBoolean("sync_on_app_resume", true)
            .set(syncTriggerOptions.syncOnAppResume)
        preferenceStore.getBoolean("sync_on_library_update", true)
            .set(syncTriggerOptions.syncOnLibraryUpdate)
        preferenceStore.getBoolean("sync_on_progress", true)
            .set(syncTriggerOptions.syncOnProgress)

        // Manga
        preferenceStore.getBoolean("sync_on_chapter_read", false)
            .set(syncTriggerOptions.syncOnChapterRead)
        preferenceStore.getBoolean("sync_on_chapter_open", false)
            .set(syncTriggerOptions.syncOnChapterOpen)

        // Anime (AniTenshi: these saved the manga values)
        preferenceStore.getBoolean("sync_on_episode_seen", false)
            .set(syncTriggerOptions.syncOnEpisodeSeen)
        preferenceStore.getBoolean("sync_on_episode_open", false)
            .set(syncTriggerOptions.syncOnEpisodeOpen)
    }

    private companion object {
        // Same values as SyncManager.SyncService
        const val SYNC_SERVICE_SYNCYOMI = 1
        const val SYNC_SERVICE_GOOGLE_DRIVE = 2
    }
}
