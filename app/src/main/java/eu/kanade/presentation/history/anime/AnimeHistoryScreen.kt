package eu.kanade.presentation.history.anime

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
import eu.kanade.presentation.history.anime.components.AnimeHistoryContinueCard
import eu.kanade.presentation.history.anime.components.AnimeHistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.updates.UpdatesCardRow
import eu.kanade.presentation.updates.UpdatesDayHeader
import eu.kanade.presentation.updates.updatesRowPlaces
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryViewModel
import kotlinx.datetime.LocalDate
import mihon.app.di.appGraph
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus

@Composable
fun AnimeHistoryScreen(
    state: AnimeHistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onClickFavorite: (animeId: Long) -> Unit,
    onDialogChange: (AnimeHistoryViewModel.Dialog?) -> Unit,
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
                    AYMR.strings.information_no_recent_anime
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                AnimeHistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    // AniTenshi: the "go on" card is for the whole history, not for search results
                    showContinue = searchQuery.isNullOrEmpty(),
                    onClickCover = { history -> onClickCover(history.animeId) },
                    onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                    onClickDelete = { item -> onDialogChange(AnimeHistoryViewModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.animeId) },
                )
            }
        }
    }
}

@Composable
private fun AnimeHistoryScreenContent(
    history: List<AnimeHistoryUiModel>,
    contentPadding: PaddingValues,
    showContinue: Boolean,
    onClickCover: (AnimeHistoryWithRelations) -> Unit,
    onClickResume: (AnimeHistoryWithRelations) -> Unit,
    onClickDelete: (AnimeHistoryWithRelations) -> Unit,
    onClickFavorite: (AnimeHistoryWithRelations) -> Unit,
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
        val last = history.firstNotNullOfOrNull { (it as? AnimeHistoryUiModel.Item)?.item }
        if (showContinue && last != null) {
            item(key = "history-continue") {
                AnimeHistoryContinueCard(
                    history = last,
                    onClick = { onClickResume(last) },
                    onClickCover = { onClickCover(last) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // AniTenshi: each day is a card, drawn row by row, as on the Updates screens
        val places = updatesRowPlaces(history) { it is AnimeHistoryUiModel.Header }
        itemsIndexed(
            items = history,
            key = { _, it -> "history-${it.hashCode()}" },
            contentType = { _, it ->
                when (it) {
                    is AnimeHistoryUiModel.Header -> "header"
                    is AnimeHistoryUiModel.Item -> "item"
                }
            },
        ) { index, item ->
            when (item) {
                is AnimeHistoryUiModel.Header -> {
                    UpdatesDayHeader(
                        date = item.date,
                        newCount = 0,
                        modifier = Modifier.animateItem(),
                    )
                }

                is AnimeHistoryUiModel.Item -> {
                    val value = item.item
                    UpdatesCardRow(
                        place = places[index]!!,
                        modifier = Modifier.animateItem(),
                    ) {
                        AnimeHistoryItem(
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

sealed interface AnimeHistoryUiModel {
    data class Header(val date: LocalDate) : AnimeHistoryUiModel
    data class Item(val item: AnimeHistoryWithRelations) : AnimeHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(AnimeHistoryViewModelStateProvider::class)
    historyState: AnimeHistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        AnimeHistoryScreen(
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
