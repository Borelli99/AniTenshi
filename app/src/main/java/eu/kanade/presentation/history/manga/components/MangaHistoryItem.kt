package eu.kanade.presentation.history.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.updates.UpdatesNextCard
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Favorite
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * AniTenshi: the last title read, first in the history, with a button to go on from where it was left.
 */
@Composable
fun MangaHistoryContinueCard(
    history: MangaHistoryWithRelations,
    onClick: () -> Unit,
    onClickCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastPage = history.lastPageRead.takeIf { it > 0L }
    UpdatesNextCard(
        label = stringResource(TLMR.strings.anitenshi_history_continue_manga),
        title = history.title,
        subtitle = if (history.chapterNumber > -1) {
            stringResource(MR.strings.display_mode_chapter, formatChapterNumber(history.chapterNumber))
        } else {
            ""
        },
        coverData = history.coverData,
        // The number of pages of a chapter is not known here: the page reached, without a bar
        progress = null,
        progressText = lastPage?.let { stringResource(MR.strings.chapter_progress, (it + 1).toInt()) },
        buttonText = stringResource(MR.strings.action_resume),
        buttonIcon = Icons.AutoMirrored.Outlined.MenuBook,
        onClick = onClick,
        onClickCover = onClickCover,
        modifier = modifier,
    )
}

@Composable
fun MangaHistoryItem(
    history: MangaHistoryWithRelations,
    timeFormatter: DateTimeFormatter,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClickResume)
            .heightIn(min = 76.dp)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier.width(44.dp),
            data = history.coverData,
            shape = RoundedCornerShape(8.dp),
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = history.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            val readAt = remember(timeFormatter, history.readAt) {
                history.readAt?.toTimestampString(timeFormatter) ?: ""
            }
            val lastPage = history.lastPageRead.takeIf { it > 0L }
            val chapterAndTime = if (history.chapterNumber > -1) {
                stringResource(
                    MR.strings.recent_manga_time,
                    formatChapterNumber(history.chapterNumber),
                    readAt,
                )
            } else {
                readAt
            }
            Text(
                text = if (lastPage != null) {
                    "$chapterAndTime · ${stringResource(MR.strings.chapter_progress, (lastPage + 1).toInt())}"
                } else {
                    chapterAndTime
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!history.coverData.isMangaFavorite) {
            IconButton(onClick = onClickFavorite) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Favorite,
                    contentDescription = stringResource(MR.strings.add_to_library),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun HistoryItemPreviews(
    @PreviewParameter(MangaHistoryWithRelationsProvider::class)
    historyWithRelations: MangaHistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            MangaHistoryItem(
                history = historyWithRelations,
                timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
