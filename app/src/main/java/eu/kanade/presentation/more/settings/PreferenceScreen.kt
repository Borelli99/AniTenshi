package eu.kanade.presentation.more.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.util.plus
import kotlin.time.Duration.Companion.seconds

/**
 * AniTenshi: a block of settings drawn as one card, with its title above it. A [Preference.PreferenceGroup]
 * is a section, and so is a run of settings that belong to no group.
 */
class PreferenceSection(
    val title: String?,
    val items: List<Preference.PreferenceItem<*>>,
)

/**
 * Splits the settings of a page into the sections shown on it. Settings outside any group are gathered in a
 * section of their own; the ones at the top of the page get [generalTitle] when the page has named
 * sections too, so that nothing floats without a name. Sections with nothing to show are left out.
 */
fun List<Preference>.toPreferenceSections(generalTitle: String): List<PreferenceSection> {
    val sections = mutableListOf<PreferenceSection>()
    var loose = mutableListOf<Preference.PreferenceItem<*>>()
    fun flushLoose() {
        if (loose.isNotEmpty()) {
            sections += PreferenceSection(title = null, items = loose)
            loose = mutableListOf()
        }
    }
    forEach { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> {
                flushLoose()
                if (preference.enabled) {
                    sections += PreferenceSection(
                        title = preference.title.takeIf { it.isNotBlank() },
                        items = preference.preferenceItems,
                    )
                }
            }
            is Preference.PreferenceItem<*> -> loose.add(preference)
        }
    }
    flushLoose()

    // A setting that is not enabled is hidden, see StatusWrapper
    val shown = sections.filter { section -> section.items.any { it.enabled } }
    val first = shown.firstOrNull() ?: return shown
    return if (first.title == null && shown.any { it.title != null }) {
        listOf(PreferenceSection(title = generalTitle, items = first.items)) + shown.drop(1)
    } else {
        shown
    }
}

/**
 * Preference Screen composable which contains the [sections] of a settings page, one card each
 * @param sections the sections which should be displayed on the preference screen, see [toPreferenceSections]
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    sections: List<PreferenceSection>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val highlightKey = SearchableSettings.highlightKey
    // A section is one item of the list, so the list is first moved to the section and then, from there,
    // to the setting itself
    val highlightRequester = remember { BringIntoViewRequester() }
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = sections.indexOfFirst { section ->
                section.items.any { it.enabled && it.title == highlightKey }
            }
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
                highlightRequester.bringIntoView()
            }
            SearchableSettings.highlightKey = null
        }
    }

    // Inside a card the titles are those of the first page of the settings
    CompositionLocalProvider(LocalPreferenceTitleStyle provides MaterialTheme.typography.titleMedium) {
        ScrollbarLazyColumn(
            modifier = modifier,
            state = state,
            // The side margins are on the cards: the scrollbar of this list is placed from this padding
            contentPadding = contentPadding + PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(count = sections.size) { index ->
                PreferenceSectionCard(
                    section = sections[index],
                    highlightKey = highlightKey,
                    highlightRequester = highlightRequester,
                )
            }
        }
    }
}

@Composable
private fun PreferenceSectionCard(
    section: PreferenceSection,
    highlightKey: String?,
    highlightRequester: BringIntoViewRequester,
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (section.title != null) {
            Text(
                text = section.title,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                val firstShown = section.items.indexOfFirst { it.enabled }
                section.items.forEachIndexed { index, item ->
                    key(index) {
                        // The line above a setting comes and goes with it
                        AnimatedVisibility(
                            visible = item.enabled && index != firstShown,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Box(
                            modifier = if (highlightKey != null && item.title == highlightKey) {
                                Modifier.bringIntoViewRequester(highlightRequester)
                            } else {
                                Modifier
                            },
                        ) {
                            PreferenceItem(
                                item = item,
                                highlightKey = highlightKey,
                            )
                        }
                    }
                }
            }
        }
    }
}
