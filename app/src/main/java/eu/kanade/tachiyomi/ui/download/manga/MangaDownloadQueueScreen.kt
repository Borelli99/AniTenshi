package eu.kanade.tachiyomi.ui.download.manga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.download.DownloadQueueList
import eu.kanade.presentation.download.DownloadQueueStatusCard
import eu.kanade.tachiyomi.ui.download.DownloadQueueEntry
import eu.kanade.tachiyomi.ui.download.DownloadQueueSection
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun DownloadQueueScreen(
    contentPadding: PaddingValues,
    screenModel: MangaDownloadQueueViewModel,
    downloadList: List<DownloadQueueSection>,
    nestedScrollConnection: NestedScrollConnection,
) {
    if (downloadList.isEmpty()) {
        EmptyScreen(
            stringRes = MR.strings.information_no_downloads,
            modifier = Modifier.padding(contentPadding),
        )
    } else {
        val isRunning by screenModel.isDownloaderRunning.collectAsStateWithLifecycle()
        // AniTenshi: whether downloads are going, with the button that pauses or resumes them, stays at the top
        Column {
            DownloadQueueStatusCard(
                isRunning = isRunning,
                titleCount = downloadList.size,
                itemCount = downloadList.sumOf { it.downloads.size },
                itemsPlural = TLMR.plurals.anitenshi_downloads_chapters,
                onPause = { screenModel.pauseDownloads() },
                onResume = { screenModel.startDownloads() },
                modifier = Modifier.padding(top = 14.dp),
            )
            DownloadQueueList(
                sections = downloadList,
                lazyListState = rememberLazyListState(),
                contentPadding = contentPadding + PaddingValues(bottom = 16.dp),
                onReorder = { entries ->
                    screenModel.reorder(entries.map { (it as DownloadQueueEntry.Manga).download })
                },
                onMove = { entry, toTop -> screenModel.move((entry as DownloadQueueEntry.Manga).download, toTop) },
                onMoveSeries = { entry, toTop ->
                    screenModel.moveSeries((entry as DownloadQueueEntry.Manga).download, toTop)
                },
                onCancel = { screenModel.cancel(listOf((it as DownloadQueueEntry.Manga).download)) },
                onCancelSeries = { screenModel.cancelSeries((it as DownloadQueueEntry.Manga).download) },
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection),
            )
        }
    }
}
