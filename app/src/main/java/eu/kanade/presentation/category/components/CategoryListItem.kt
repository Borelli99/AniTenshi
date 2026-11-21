package eu.kanade.presentation.category.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.updates.UpdatesColumnItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: one category, as a row of the card that holds them all: the handle to drag it, its name with how many
 * titles it has (and whether it is hidden), the eye to hide or show it in the library, and a ⋮ to rename or delete
 * it. Tapping the name renames it too.
 *
 * @param titleCount the titles of the library in it, null until they are counted.
 */
@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    titleCount: Int?,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val radius = 20.dp
    // The shape alone changes while rows are dragged, never the height, so the list doesn't jump
    val shape = if (isDragging) {
        RoundedCornerShape(16.dp)
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) radius else 0.dp,
            topEnd = if (isFirst) radius else 0.dp,
            bottomStart = if (isLast) radius else 0.dp,
            bottomEnd = if (isLast) radius else 0.dp,
        )
    }
    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "elevation")
    val subtitle = listOfNotNull(
        when (titleCount) {
            null -> null
            0 -> stringResource(TLMR.strings.anitenshi_categories_no_titles)
            else -> pluralStringResource(TLMR.plurals.anitenshi_categories_titles, titleCount, titleCount)
        },
        stringResource(TLMR.strings.anitenshi_categories_hidden).takeIf { category.hidden },
    ).joinToString(" · ")

    UpdatesColumnItem(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = if (isDragging) colors.surfaceContainerHigh else colors.surfaceContainer,
            shadowElevation = elevation,
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp, end = 12.dp),
                    color = if (isFirst || isDragging) Color.Transparent else colors.surfaceContainerHigh,
                )
                Row(
                    modifier = Modifier
                        .heightIn(min = 64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.DragHandle,
                        contentDescription = null,
                        modifier = Modifier
                            .draggableHandle()
                            .padding(12.dp),
                        tint = colors.outline,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onRename)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .alpha(if (category.hidden) 0.55f else 1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onHide) {
                        Icon(
                            imageVector = if (category.hidden) {
                                MaterialSymbols.Rounded.VisibilityOff
                            } else {
                                MaterialSymbols.Rounded.Visibility
                            },
                            contentDescription = if (category.hidden) {
                                stringResource(TLMR.strings.anitenshi_categories_show)
                            } else {
                                stringResource(TLMR.strings.anitenshi_categories_hide)
                            },
                            tint = if (category.hidden) colors.onSurfaceVariant else colors.onSurface,
                        )
                    }
                    CategoryItemMenu(onRename = onRename, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun CategoryItemMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val closeMenu = { expanded = false }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.Rounded.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = closeMenu) {
            DropdownMenuItem(
                text = { Text(stringResource(TLMR.strings.anitenshi_categories_rename)) },
                leadingIcon = { Icon(imageVector = MaterialSymbols.Rounded.Edit, contentDescription = null) },
                onClick = {
                    closeMenu()
                    onRename()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(TLMR.strings.anitenshi_categories_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    closeMenu()
                    onDelete()
                },
            )
        }
    }
}
