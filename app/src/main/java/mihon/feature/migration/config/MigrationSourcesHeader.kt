package mihon.feature.migration.config

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.PushPin
import mihon.icons.materialsymbols.roundedfilled.PushPin
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: which of the quick choices matches the sources ticked on the migration screen.
 * [Custom] is a choice made by hand, and highlights none of the chips.
 */
enum class MigrationSourcesSelection { All, Pinned, None, Custom }

fun migrationSourcesSelection(
    sourceIds: List<Long>,
    pinnedSourceIds: Set<Long>,
    includedSourceIds: List<Long>,
): MigrationSourcesSelection {
    val shown = sourceIds.toSet()
    val chosen = includedSourceIds.filterTo(HashSet()) { it in shown }
    val pinned = pinnedSourceIds.filterTo(HashSet()) { it in shown }
    return when {
        chosen.isEmpty() -> MigrationSourcesSelection.None
        chosen == shown -> MigrationSourcesSelection.All
        pinned.isNotEmpty() && chosen == pinned -> MigrationSourcesSelection.Pinned
        else -> MigrationSourcesSelection.Custom
    }
}

/**
 * AniTenshi: top of the screen where the sources for a migration are chosen. A plain title with how
 * many sources are ticked, and three labelled chips ("All", "Only pinned", "None") in place of the three
 * look-alike icons it had before. "Only pinned" is left out when no source on the list is pinned.
 */
@Composable
fun MigrationSourcesTopBar(
    chosenCount: Int,
    totalCount: Int,
    selection: MigrationSourcesSelection,
    hasPinnedSources: Boolean,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectPinned: () -> Unit,
    onSelectNone: () -> Unit,
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        AppBar(
            title = stringResource(TLMR.strings.anitenshi_migration_sources_title),
            subtitle = if (chosenCount == 0) {
                stringResource(TLMR.strings.anitenshi_migration_sources_none_chosen)
            } else {
                pluralStringResource(
                    TLMR.plurals.anitenshi_migration_sources_chosen,
                    count = chosenCount,
                    chosenCount,
                    totalCount,
                )
            },
            navigateUp = navigateUp,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.padding.small)
                .padding(bottom = MaterialTheme.padding.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MigrationSourcesChip(
                label = stringResource(TLMR.strings.anitenshi_migration_sources_all),
                icon = MaterialSymbols.Rounded.DoneAll,
                selected = selection == MigrationSourcesSelection.All,
                onClick = onSelectAll,
            )
            if (hasPinnedSources) {
                MigrationSourcesChip(
                    label = stringResource(TLMR.strings.anitenshi_migration_sources_pinned),
                    icon = MaterialSymbols.Rounded.PushPin,
                    selected = selection == MigrationSourcesSelection.Pinned,
                    onClick = onSelectPinned,
                )
            }
            MigrationSourcesChip(
                label = stringResource(TLMR.strings.anitenshi_migration_sources_none),
                icon = MaterialSymbols.Rounded.Close,
                selected = selection == MigrationSourcesSelection.None,
                onClick = onSelectNone,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun MigrationSourcesChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = { Text(text = label) },
    )
}

/** AniTenshi: source name on the migration list, with the gold pin after it when the source is pinned. */
@Composable
fun MigrationSourceName(name: String, isPinned: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isPinned) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.PushPin,
                contentDescription = stringResource(MR.strings.pinned_sources),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
