package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.updates.UpdatesProgress
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/*
 * AniTenshi: the look of the "Statistics" screens, anime and manga. Four big numbers in cards, the chart of the
 * last 7 days, the library split by how far you've got, then the other numbers in rows. On wide screens the four
 * numbers are in one row and the chart and the library side by side.
 */

private val MaxContentWidth = 840.dp
private val WideLayoutWidth = 720.dp

/** One of the four big numbers at the top, with how far it has got when [progress] is given. */
class StatsBigNumber(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val progress: Float? = null,
    val progressText: String? = null,
)

/** One part of the library bar, with its name, a line under it and how many titles it has. */
class StatsLibraryPart(
    val label: String,
    val hint: String?,
    val count: Int,
)

@Composable
fun StatsContent(
    paddingValues: PaddingValues,
    numbers: List<StatsBigNumber>,
    week: StatsData.Week,
    weekTotalText: String,
    libraryParts: List<StatsLibraryPart>,
    libraryTotalText: String,
    otherRows: List<Pair<String, String>>,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= WideLayoutWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                numbers.chunked(if (isWide) 4 else 2).forEach { row ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { number ->
                            StatsNumberCard(
                                number = number,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
                val weekCard = @Composable { modifier: Modifier ->
                    StatsSectionCard(
                        title = stringResource(TLMR.strings.anitenshi_stats_last_7_days),
                        trailing = weekTotalText,
                        modifier = modifier,
                    ) {
                        StatsWeekChart(week)
                    }
                }
                val libraryCard = @Composable { modifier: Modifier ->
                    StatsSectionCard(
                        title = stringResource(TLMR.strings.anitenshi_stats_your_library),
                        trailing = libraryTotalText,
                        modifier = modifier,
                    ) {
                        StatsLibraryBar(libraryParts)
                    }
                }
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        weekCard(Modifier.weight(1f))
                        libraryCard(Modifier.weight(1f))
                    }
                } else {
                    weekCard(Modifier)
                    libraryCard(Modifier)
                }
                StatsSectionCard(
                    title = stringResource(TLMR.strings.anitenshi_stats_more),
                    trailing = null,
                ) {
                    StatsRows(otherRows)
                }
            }
        }
    }
}

@Composable
private fun StatsNumberCard(
    number: StatsBigNumber,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = number.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = colors.secondary,
            )
            Text(
                text = number.value,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = number.label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (number.progress != null) {
                UpdatesProgress(
                    fraction = number.progress,
                    text = number.progressText,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    trailing: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.uppercase(),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = colors.secondary,
                )
                if (trailing != null) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

/** A gold bar for each of the last 7 days, today brighter and last, with the count above and the day under it. */
@Composable
private fun StatsWeekChart(week: StatsData.Week) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val highest = week.counts.maxOrNull()?.takeIf { it > 0 } ?: 1
    val numberFormat = NumberFormat.getIntegerInstance(locale)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        week.counts.forEachIndexed { index, count ->
            val day = week.firstDay.plusDays(index.toLong()).dayOfWeek
            val isToday = index == week.counts.lastIndex
            val dayName = day.getDisplayName(TextStyle.FULL, locale)
            val countText = numberFormat.format(count)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clearAndSetSemantics { contentDescription = "$dayName: $countText" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (count > 0) colors.onSurface else colors.outline,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .widthIn(max = 28.dp)
                        .fillMaxWidth()
                        .height(if (count > 0) 96.dp * count / highest else 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                count == 0 -> colors.surfaceContainerHighest
                                isToday -> colors.secondary
                                else -> colors.secondary.copy(alpha = 0.7f)
                            },
                        ),
                )
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** The library as one bar split in three (gold, lavender, grey), with what each part means and how many it has. */
@Composable
private fun StatsLibraryBar(parts: List<StatsLibraryPart>) {
    val colors = MaterialTheme.colorScheme
    val partColors = listOf(colors.secondary, colors.onSurfaceVariant, colors.outline)
    val numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceContainerHighest),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        parts.forEachIndexed { index, part ->
            if (part.count > 0) {
                Box(
                    modifier = Modifier
                        .weight(part.count.toFloat())
                        .fillMaxHeight()
                        .background(partColors.getOrElse(index) { Color.Transparent }),
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEachIndexed { index, part ->
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(partColors.getOrElse(index) { Color.Transparent }),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                    )
                    if (part.hint != null) {
                        Text(
                            text = part.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = numberFormat.format(part.count),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StatsRows(rows: List<Pair<String, String>>) {
    val colors = MaterialTheme.colorScheme
    Column {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                HorizontalDivider(color = colors.surfaceContainerHigh)
            }
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
        }
    }
}

/** A time spent, in its two largest units ("3 d 14 h", "2 h 5 min", "12 min"). */
@Composable
fun statsDurationText(millis: Long): String {
    return millis.milliseconds.toComponents { days, hours, minutes, _, _ ->
        when {
            days > 0 -> listOfNotNull(
                stringResource(TLMR.strings.anitenshi_stats_days, days),
                if (hours > 0) stringResource(TLMR.strings.anitenshi_stats_hours, hours) else null,
            ).joinToString(" ")

            hours > 0 -> listOfNotNull(
                stringResource(TLMR.strings.anitenshi_stats_hours, hours),
                if (minutes > 0) stringResource(TLMR.strings.anitenshi_stats_minutes, minutes) else null,
            ).joinToString(" ")

            else -> stringResource(TLMR.strings.anitenshi_stats_minutes, minutes)
        }
    }
}

/** Whole numbers written the way the phone's language writes them ("1.240" in Italian). */
fun statsNumber(value: Number): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
