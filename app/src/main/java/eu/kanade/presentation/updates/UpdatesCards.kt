package eu.kanade.presentation.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.util.relativeTimeSpanString
import kotlinx.datetime.LocalDate
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/*
 * AniTenshi: the look of the "Updates" screens, anime and manga. The latest title not yet watched or read is
 * shown first, in a card of its own; then each day is a card with its name in gold and how many are new.
 * On wide screens everything stays in a column in the middle.
 */

private val MaxContentWidth = 720.dp

/** Where a row sits in its day's card, so that the card is drawn round at its two ends. */
class UpdatesRowPlace(val isFirst: Boolean, val isLast: Boolean)

/**
 * For each entry of a list of day headers and rows: the place of a row in its day, null for a header.
 */
fun <T> updatesRowPlaces(models: List<T>, isHeader: (T) -> Boolean): List<UpdatesRowPlace?> {
    return models.mapIndexed { index, model ->
        if (isHeader(model)) {
            null
        } else {
            UpdatesRowPlace(
                isFirst = index == 0 || isHeader(models[index - 1]),
                isLast = index == models.lastIndex || isHeader(models[index + 1]),
            )
        }
    }
}

/**
 * For each day header of the list, how many of the rows under it are new (not watched or read yet); 0 for rows.
 */
fun <T> updatesNewCounts(models: List<T>, isHeader: (T) -> Boolean, isNew: (T) -> Boolean): IntArray {
    val counts = IntArray(models.size)
    var header = -1
    models.forEachIndexed { index, model ->
        if (isHeader(model)) {
            header = index
        } else if (header >= 0 && isNew(model)) {
            counts[header]++
        }
    }
    return counts
}

/** An entry of the list, centred and kept to a readable width on wide screens. */
@Composable
fun UpdatesColumnItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

/** One row of a day's card: the card is drawn row by row, round at the top of the first and the foot of the last. */
@Composable
fun UpdatesCardRow(
    place: UpdatesRowPlace,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val radius = 20.dp
    val shape = RoundedCornerShape(
        topStart = if (place.isFirst) radius else 0.dp,
        topEnd = if (place.isFirst) radius else 0.dp,
        bottomStart = if (place.isLast) radius else 0.dp,
        bottomEnd = if (place.isLast) radius else 0.dp,
    )
    UpdatesColumnItem(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(
                    start = 6.dp,
                    end = 6.dp,
                    top = if (place.isFirst) 6.dp else 1.dp,
                    bottom = if (place.isLast) 6.dp else 1.dp,
                ),
        ) {
            content()
        }
    }
}

/** The name of a day, in gold, and how many of its rows are new. */
@Composable
fun UpdatesDayHeader(
    date: LocalDate,
    newCount: Int,
    modifier: Modifier = Modifier,
) {
    UpdatesColumnItem(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
                .semantics(mergeDescendants = true) { heading() },
        ) {
            Text(
                text = relativeDateText(date),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (newCount > 0) {
                Text(
                    text = " · " + pluralStringResource(TLMR.plurals.anitenshi_updates_new_count, newCount, newCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** When the library was last updated, in a small line at the top. */
@Composable
fun UpdatesLastUpdatedLine(
    lastUpdated: Long,
    modifier: Modifier = Modifier,
) {
    UpdatesColumnItem(modifier = modifier) {
        Text(
            text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A thin gold bar of how far an episode or a chapter has got, with the point reached beside it. */
@Composable
fun UpdatesProgress(
    fraction: Float,
    text: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * The latest episode or chapter not watched or read yet, first on the screen, with a button to start or go on.
 *
 * @param progress how far it has got, as a fraction, when that is known; [progressText] alone otherwise.
 */
@Composable
fun UpdatesNextCard(
    label: String,
    title: String,
    subtitle: String,
    coverData: Any?,
    progress: Float?,
    progressText: String?,
    buttonText: String,
    buttonIcon: ImageVector,
    onClick: () -> Unit,
    onClickCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    UpdatesColumnItem(modifier = modifier.padding(top = 4.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ItemCover.Book(
                    data = coverData,
                    modifier = Modifier.width(76.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onClickCover,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = colors.secondary,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (progress != null) {
                        UpdatesProgress(fraction = progress, text = progressText)
                    } else if (progressText != null) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondary,
                            contentColor = colors.onSecondary,
                        ),
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = buttonText,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
