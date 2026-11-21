package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload

sealed interface DownloadQueueEntry {
    data class Manga(val download: MangaDownload) : DownloadQueueEntry
    data class Anime(val download: AnimeDownload) : DownloadQueueEntry
}

/**
 * AniTenshi: the downloads of one title, in queue order. [id] is the title's id; its name, cover and source are read
 * from the downloads themselves.
 */
data class DownloadQueueSection(
    val id: Long,
    val downloads: List<DownloadQueueEntry>,
)

internal fun <Item, Key> reconcileQueueOrder(
    requested: List<Item>,
    current: List<Item>,
    key: (Item) -> Key,
): List<Item> {
    val currentByKey = current.associateBy(key)
    val requestedKeys = requested.map(key).distinct()
    val ordered = requestedKeys.mapNotNull(currentByKey::get)
    val orderedKeys = requestedKeys.toSet()
    return ordered + current.filter { key(it) !in orderedKeys }
}
