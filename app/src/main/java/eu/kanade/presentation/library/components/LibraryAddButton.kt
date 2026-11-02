package eu.kanade.presentation.library.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The + button of the libraries: opens Browse (sources, extensions, migration) for that library.
 * The badge counts the extensions waiting for an update.
 */
@Composable
fun LibraryAddButton(
    visible: Boolean,
    extensionUpdates: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier,
    ) {
        BadgedBox(
            badge = {
                if (extensionUpdates > 0) {
                    Badge { Text(text = extensionUpdates.toString()) }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(TLMR.strings.anitenshi_add_entries),
            )
        }
    }
}
