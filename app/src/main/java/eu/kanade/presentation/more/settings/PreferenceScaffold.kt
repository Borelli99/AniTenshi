package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Search
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

// AniTenshi: a page gets the index of its sections when it has at least this many of them with a name.
// On a shorter page the index would take room and save no scrolling.
private const val MinSectionsForIndex = 3

/**
 * A page of the settings.
 *
 * AniTenshi: the settings are drawn as sections, one card each (see [PreferenceScreen]). Under the title a
 * long page has an index, a row of chips with the names of its sections: a tap moves the page to that
 * section, and the chip of the section at the top of the screen is the lit one. The bar keeps the color of
 * the page while it scrolls, as on the first page of the settings.
 *
 * @param onSearchClick opens the search of the settings from the bar. Null where the search is on screen
 * already, as on tablets, with the list of the pages beside this one.
 */
@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val sections = itemsProvider().toPreferenceSections(
        generalTitle = stringResource(TLMR.strings.anitenshi_settings_section_general),
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Where each named section is on the list, and its name
    val named = sections.mapIndexedNotNull { index, section -> section.title?.let { index to it } }
    val showIndex = named.size >= MinSectionsForIndex

    // The chip that is lit is the one of the section at the top of the screen. The last sections of a page
    // never get there, so after a tap the tapped chip stays lit until the page is moved by hand.
    var tappedChip by remember { mutableStateOf<Int?>(null) }
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) tappedChip = null
        }
    }
    val chipInView = named.indexOfLast { it.first <= firstVisible }.coerceAtLeast(0)
    val currentChip = (tappedChip ?: chipInView).coerceIn(0, (named.size - 1).coerceAtLeast(0))

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                AppBar(
                    title = stringResource(titleRes),
                    navigateUp = onBackPressed,
                    actions = {
                        actions()
                        if (onSearchClick != null) {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Search,
                                    contentDescription = stringResource(MR.strings.action_search_settings),
                                )
                            }
                        }
                    },
                )
                if (showIndex) {
                    SectionIndex(
                        titles = named.map { it.second },
                        current = currentChip,
                        onClick = { chip ->
                            tappedChip = chip
                            scope.launch { listState.animateScrollToItem(named[chip].first) }
                        },
                    )
                }
                HorizontalDivider(
                    color = if (showIndex || listState.canScrollBackward) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Color.Transparent
                    },
                )
            }
        },
        content = { contentPadding ->
            PreferenceScreen(
                sections = sections,
                state = listState,
                contentPadding = contentPadding,
            )
        },
    )
}

@Composable
private fun SectionIndex(
    titles: List<String>,
    current: Int,
    onClick: (Int) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(current) {
        // Keeps the lit chip on screen, with the one before it still in sight
        state.animateScrollToItem((current - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count = titles.size) { index ->
            FilterChip(
                selected = index == current,
                onClick = { onClick(index) },
                label = { Text(text = titles[index]) },
            )
        }
    }
}
