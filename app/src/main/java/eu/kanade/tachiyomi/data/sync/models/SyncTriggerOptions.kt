package eu.kanade.tachiyomi.data.sync.models

import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.tail.TLMR

data class SyncTriggerOptions(
    val syncOnChapterRead: Boolean = false,
    val syncOnChapterOpen: Boolean = false,
    val syncOnAppStart: Boolean = true,
    val syncOnAppResume: Boolean = true,
    val syncOnEpisodeSeen: Boolean = false,
    val syncOnEpisodeOpen: Boolean = false,
    // AniTenshi: sync before a library update started by hand (pull-to-refresh, "Update library")
    val syncOnLibraryUpdate: Boolean = true,
    // AniTenshi: sync after a chapter is read or an episode watched (not more often than every 10 minutes)
    val syncOnProgress: Boolean = true,
) {
    fun asBooleanArray() = booleanArrayOf(
        syncOnChapterRead,
        syncOnChapterOpen,
        syncOnAppStart,
        syncOnAppResume,
        syncOnEpisodeSeen,
        syncOnEpisodeOpen,
        syncOnLibraryUpdate,
        syncOnProgress,
    )

    fun anyEnabled() = syncOnChapterRead ||
        syncOnChapterOpen ||
        syncOnAppStart ||
        syncOnAppResume ||
        syncOnEpisodeSeen ||
        syncOnEpisodeOpen ||
        syncOnLibraryUpdate ||
        syncOnProgress

    companion object {
        /**
         * AniTenshi: only the triggers the app acts on. The old chapter and episode ones were listed but
         * nothing ever started a sync from them; [syncOnProgress] is the one that does.
         */
        val mainOptions = persistentListOf(
            Entry(
                label = TLMR.strings.anitenshi_sync_on_app_start,
                getter = SyncTriggerOptions::syncOnAppStart,
                setter = { options, enabled -> options.copy(syncOnAppStart = enabled) },
            ),
            Entry(
                label = TLMR.strings.anitenshi_sync_on_app_resume,
                getter = SyncTriggerOptions::syncOnAppResume,
                setter = { options, enabled -> options.copy(syncOnAppResume = enabled) },
            ),
            Entry(
                label = TLMR.strings.anitenshi_sync_on_library_update,
                getter = SyncTriggerOptions::syncOnLibraryUpdate,
                setter = { options, enabled -> options.copy(syncOnLibraryUpdate = enabled) },
            ),
            Entry(
                label = TLMR.strings.anitenshi_sync_on_progress,
                getter = SyncTriggerOptions::syncOnProgress,
                setter = { options, enabled -> options.copy(syncOnProgress = enabled) },
            ),
        )

        fun fromBooleanArray(array: BooleanArray) = SyncTriggerOptions(
            syncOnChapterRead = array[0],
            syncOnChapterOpen = array[1],
            syncOnAppStart = array[2],
            syncOnAppResume = array[3],
            syncOnEpisodeSeen = array[4],
            syncOnEpisodeOpen = array[5],
            syncOnLibraryUpdate = array.getOrElse(6) { true },
            syncOnProgress = array.getOrElse(7) { true },
        )
    }

    data class Entry(
        val label: StringResource,
        val getter: (SyncTriggerOptions) -> Boolean,
        val setter: (SyncTriggerOptions, Boolean) -> SyncTriggerOptions,
        val enabled: (SyncTriggerOptions) -> Boolean = { true },
    )
}
