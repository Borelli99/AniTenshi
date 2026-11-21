package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.updates.UpdatesColumnItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

/*
 * AniTenshi: the list of the "Categories" screen, the same for anime and manga. A line on what can be done, then
 * every category in one card; on wide screens it stays in a column in the middle.
 */

/**
 * @param titleCounts the titles of the library in each category, by category id; null until they are counted.
 */
@Composable
fun CategoryList(
    categories: List<Category>,
    titleCounts: Map<Long, Int>?,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
) {
    val categoriesState = remember { categories.toMutableStateList() }
    val reorderableState = rememberReorderableLazyListState(lazyListState, paddingValues) { from, to ->
        // By key, as the line at the top is an item of the list too
        val fromIndex = categoriesState.indexOfFirst { it.key == from.key }
        val toIndex = categoriesState.indexOfFirst { it.key == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        val item = categoriesState.removeAt(fromIndex)
        categoriesState.add(toIndex, item)
        onChangeOrder(item, toIndex)
    }

    LaunchedEffect(categories) {
        if (!reorderableState.isAnyItemDragging) {
            categoriesState.clear()
            categoriesState.addAll(categories)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        // Room at the foot for the button to add one
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    ) {
        item(key = HINT_KEY) {
            UpdatesColumnItem {
                Text(
                    text = stringResource(TLMR.strings.anitenshi_categories_hint),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(
            items = categoriesState,
            key = { _, category -> category.key },
        ) { index, category ->
            ReorderableItem(reorderableState, category.key) { isDragging ->
                CategoryListItem(
                    modifier = Modifier.animateItem(),
                    category = category,
                    titleCount = titleCounts?.let { it[category.id] ?: 0 },
                    isFirst = index == 0,
                    isLast = index == categoriesState.lastIndex,
                    isDragging = isDragging,
                    onRename = { onClickRename(category) },
                    onHide = { onClickHide(category) },
                    onDelete = { onClickDelete(category) },
                )
            }
        }
    }
}

/** AniTenshi: the gold button at the foot of the screen that adds a category. */
@Composable
fun CategoryNewButton(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        text = {
            Text(
                text = stringResource(TLMR.strings.anitenshi_categories_new),
                fontWeight = FontWeight.SemiBold,
            )
        },
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        onClick = onCreate,
        modifier = modifier,
        expanded = lazyListState.shouldExpandFAB(),
        containerColor = colors.secondary,
        contentColor = colors.onSecondary,
    )
}

private const val HINT_KEY = "category-hint"

private val Category.key inline get() = "category-$id"
