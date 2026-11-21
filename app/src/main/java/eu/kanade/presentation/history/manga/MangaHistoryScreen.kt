package eu.kanade.presentation.history.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.history.manga.components.MangaHistoryContinueCard
import eu.kanade.presentation.history.manga.components.MangaHistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.updates.UpdatesCardRow
import eu.kanade.presentation.updates.UpdatesDayHeader
import eu.kanade.presentation.updates.updatesRowPlaces
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryViewModel
import kotlinx.datetime.LocalDate
import mihon.app.di.appGraph
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus

@Composable
fun MangaHistoryScreen(
    state: MangaHistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (MangaHistoryViewModel.Dialog?) -> Unit,
    searchQuery: String? = null,
) {
    Scaffold(

        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                MangaHistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    // AniTenshi: the "go on" card is for the whole history, not for search results
                    showContinue = searchQuery.isNullOrEmpty(),
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { item -> onDialogChange(MangaHistoryViewModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.mangaId) },
                )
            }
        }
    }
}

@Composable
private fun MangaHistoryScreenContent(
    history: List<MangaHistoryUiModel>,
    contentPadding: PaddingValues,
    showContinue: Boolean,
    onClickCover: (MangaHistoryWithRelations) -> Unit,
    onClickResume: (MangaHistoryWithRelations) -> Unit,
    onClickDelete: (MangaHistoryWithRelations) -> Unit,
    onClickFavorite: (MangaHistoryWithRelations) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current
    val is24HourSystem = rememberIs24HourSystem()
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val timeFormatter = remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale.platformLocale, is24HourSystem)
    }

    FastScrollLazyColumn(
        contentPadding = contentPadding + PaddingValues(top = 8.dp, bottom = 16.dp),
    ) {
        // AniTenshi: the last title opened comes first, in a card with a button to go on
        val last = history.firstNotNullOfOrNull { (it as? MangaHistoryUiModel.Item)?.item }
        if (showContinue && last != null) {
            item(key = "history-continue") {
                MangaHistoryContinueCard(
                    history = last,
                    onClick = { onClickResume(last) },
                    onClickCover = { onClickCover(last) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // AniTenshi: each day is a card, drawn row by row, as on the Updates screens
        val places = updatesRowPlaces(history) { it is MangaHistoryUiModel.Header }
        itemsIndexed(
            items = history,
            key = { _, it -> "history-${it.hashCode()}" },
            contentType = { _, it ->
                when (it) {
                    is MangaHistoryUiModel.Header -> "header"
                    is MangaHistoryUiModel.Item -> "item"
                }
            },
        ) { index, item ->
            when (item) {
                is MangaHistoryUiModel.Header -> {
                    UpdatesDayHeader(
                        date = item.date,
                        newCount = 0,
                        modifier = Modifier.animateItem(),
                    )
                }

                is MangaHistoryUiModel.Item -> {
                    val value = item.item
                    UpdatesCardRow(
                        place = places[index]!!,
                        modifier = Modifier.animateItem(),
                    ) {
                        MangaHistoryItem(
                            history = value,
                            timeFormatter = timeFormatter,
                            onClickCover = { onClickCover(value) },
                            onClickResume = { onClickResume(value) },
                            onClickDelete = { onClickDelete(value) },
                            onClickFavorite = { onClickFavorite(value) },
                        )
                    }
                }
            }
        }
    }
}

sealed interface MangaHistoryUiModel {
    data class Header(val date: LocalDate) : MangaHistoryUiModel
    data class Item(val item: MangaHistoryWithRelations) : MangaHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(MangaHistoryScreenModelStateProvider::class)
    historyState: MangaHistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        MangaHistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            searchQuery = null,
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
        )
    }
}
