package eu.kanade.presentation.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/** AniTenshi: what the merged sources & extensions list shows. */
enum class BrowseListFilter {
    /** Updates, installed sources, then the extensions to install. */
    All,

    /** Installed extensions (with their settings), updates and the ones that did not load. */
    Installed,

    /** Extensions available from the repos. */
    Available,
}

@Composable
fun BrowseListFilterChips(
    selected: BrowseListFilter,
    installedCount: Int,
    availableCount: Int,
    onSelect: (BrowseListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        FilterPill(
            text = stringResource(TLMR.strings.anitenshi_filter_all),
            selected = selected == BrowseListFilter.All,
            onClick = { onSelect(BrowseListFilter.All) },
        )
        FilterPill(
            text = stringResource(TLMR.strings.anitenshi_filter_installed, installedCount),
            selected = selected == BrowseListFilter.Installed,
            onClick = { onSelect(BrowseListFilter.Installed) },
        )
        FilterPill(
            text = stringResource(TLMR.strings.anitenshi_filter_available, availableCount),
            selected = selected == BrowseListFilter.Available,
            onClick = { onSelect(BrowseListFilter.Available) },
        )
    }
}

/** A title that separates the parts of the merged list ("Installed sources", "To install"). */
@Composable
fun BrowseSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(
            start = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.medium,
            top = MaterialTheme.padding.medium,
            bottom = MaterialTheme.padding.extraSmall,
        ),
    )
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.secondary else Color.Transparent,
        contentColor = if (selected) colors.onSecondary else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.semantics {
            role = Role.RadioButton
            this.selected = selected
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 36.dp)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
