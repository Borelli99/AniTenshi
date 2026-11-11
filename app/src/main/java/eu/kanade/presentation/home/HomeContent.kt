package eu.kanade.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.home.ContinueEntry
import eu.kanade.tachiyomi.ui.home.HomeState
import eu.kanade.tachiyomi.ui.home.LatestRow
import eu.kanade.tachiyomi.ui.home.LocalBottomNavPadding
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.AniTenshiMark
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

private val ContinueCoverWidth = 108.dp

// The gold pill on those covers: its paddings, icon and gap, and what they leave for the label
private val ContinuePillMargin = 6.dp
private val ContinuePillPadding = 9.dp
private val ContinuePillIconSize = 15.dp
private val ContinuePillGap = 4.dp
private val ContinuePillLabelMaxWidth = ContinueCoverWidth -
    ContinuePillMargin * 2 - ContinuePillPadding * 2 - ContinuePillIconSize - ContinuePillGap

// The pill label shrinks down to this to fit a long number
private val ContinuePillMinFontSize = 9.sp
private val ContinuePillMaxFontSize = 12.sp
private val LatestCoverWidth = 96.dp

/**
 * AniTenshi Home ("Aureola" look): continue watching, continue reading, latest anime and latest manga
 * from the pinned sources. On tablets anime and manga stand side by side.
 * The button at the bottom right opens More.
 */
@Composable
fun HomeContent(
    state: HomeState,
    snackbarHostState: SnackbarHostState,
    getAnime: @Composable (Anime) -> State<Anime>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickContinueAnime: (ContinueEntry) -> Unit,
    onOpenAnime: (Long) -> Unit,
    onHideAnime: (Long) -> Unit,
    onShowAnime: (Long) -> Unit,
    onClickContinueManga: (ContinueEntry) -> Unit,
    onOpenManga: (Long) -> Unit,
    onHideManga: (Long) -> Unit,
    onShowManga: (Long) -> Unit,
    onClickAnimeSource: (LatestRow<Anime>) -> Unit,
    onClickMangaSource: (LatestRow<Manga>) -> Unit,
    onClickAnime: (Anime) -> Unit,
    onClickManga: (Manga) -> Unit,
    onClickRefresh: () -> Unit,
    onClickMore: () -> Unit,
) {
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                titleContent = { HomeTitle() },
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_webview_refresh),
                                icon = Icons.Outlined.Refresh,
                                onClick = onClickRefresh,
                            ),
                        ),
                    )
                    // AniTenshi: on phones "More" is a labelled pill up here, so nothing covers the covers
                    if (!isTabletUi()) {
                        MorePill(onClick = onClickMore)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            // On tablets there is room: "More" stays the floating button at the bottom right
            if (isTabletUi()) {
                // Gold in the dark theme, night blue (with gold dots) in the light one
                val colors = MaterialTheme.colorScheme
                ExtendedFloatingActionButton(
                    // Above the round buttons of the main screens
                    modifier = Modifier.padding(bottom = LocalBottomNavPadding.current),
                    text = { Text(text = stringResource(MR.strings.label_more), fontWeight = FontWeight.Bold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = null,
                            tint = if (colors.primary == AniTenshiPalette.Navy) AniTenshiPalette.Gold else colors.onPrimary,
                        )
                    },
                    onClick = onClickMore,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        val continueAnime: @Composable () -> Unit = {
            ContinueSection(
                title = stringResource(TLMR.strings.anitenshi_continue_watching),
                emptyText = stringResource(TLMR.strings.anitenshi_nothing_to_continue_anime),
                entries = state.continueAnime,
                hiddenCount = state.hiddenAnime.size,
                isAnime = true,
                onClickHidden = { dialog = HomeDialog.Hidden(anime = true) },
                onClick = onClickContinueAnime,
                onLongClick = { dialog = HomeDialog.Actions(entry = it, anime = true) },
            )
        }
        val continueManga: @Composable () -> Unit = {
            ContinueSection(
                title = stringResource(TLMR.strings.anitenshi_continue_reading),
                emptyText = stringResource(TLMR.strings.anitenshi_nothing_to_continue_manga),
                entries = state.continueManga,
                hiddenCount = state.hiddenManga.size,
                isAnime = false,
                onClickHidden = { dialog = HomeDialog.Hidden(anime = false) },
                onClick = onClickContinueManga,
                onLongClick = { dialog = HomeDialog.Actions(entry = it, anime = false) },
            )
        }
        val latestAnime: @Composable () -> Unit = {
            LatestSection(
                title = stringResource(TLMR.strings.anitenshi_latest_anime),
                emptyText = stringResource(TLMR.strings.anitenshi_no_pinned_anime_sources),
                rows = state.latestAnime,
                onClickSource = onClickAnimeSource,
            ) { titles ->
                LatestRowCovers(titles) { item ->
                    val anime by getAnime(item)
                    LatestCard(
                        title = anime.title,
                        cover = anime.asAnimeCover(),
                        onClick = { onClickAnime(anime) },
                    )
                }
            }
        }
        val latestManga: @Composable () -> Unit = {
            LatestSection(
                title = stringResource(TLMR.strings.anitenshi_latest_manga),
                emptyText = stringResource(TLMR.strings.anitenshi_no_pinned_manga_sources),
                rows = state.latestManga,
                onClickSource = onClickMangaSource,
            ) { titles ->
                LatestRowCovers(titles) { item ->
                    val manga by getManga(item)
                    LatestCard(
                        title = manga.title,
                        cover = manga.asMangaCover(),
                        onClick = { onClickManga(manga) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                // Room for the round buttons floating at the bottom
                .padding(bottom = LocalBottomNavPadding.current + MaterialTheme.padding.medium),
        ) {
            if (isTabletUi()) {
                // Tablet: anime on the left, manga on the right
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large)) {
                    Column(modifier = Modifier.weight(1f)) {
                        continueAnime()
                        latestAnime()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        continueManga()
                        latestManga()
                    }
                }
            } else {
                continueAnime()
                continueManga()
                latestAnime()
                latestManga()
            }
        }
    }

    when (val current = dialog) {
        null -> {}

        is HomeDialog.Actions -> ContinueActionsDialog(
            entry = current.entry,
            hideLabel = stringResource(
                if (current.anime) {
                    TLMR.strings.anitenshi_continue_hide_anime
                } else {
                    TLMR.strings.anitenshi_continue_hide_manga
                },
            ),
            onOpen = {
                dialog = null
                if (current.anime) onOpenAnime(current.entry.id) else onOpenManga(current.entry.id)
            },
            onHide = {
                dialog = null
                if (current.anime) onHideAnime(current.entry.id) else onHideManga(current.entry.id)
            },
            onDismissRequest = { dialog = null },
        )

        is HomeDialog.Hidden -> {
            val entries = if (current.anime) state.hiddenAnime else state.hiddenManga
            LaunchedEffect(entries.isEmpty()) {
                if (entries.isEmpty()) dialog = null
            }
            HiddenEntriesDialog(
                title = stringResource(
                    if (current.anime) {
                        TLMR.strings.anitenshi_continue_hidden_anime_title
                    } else {
                        TLMR.strings.anitenshi_continue_hidden_manga_title
                    },
                ),
                entries = entries,
                onShow = { if (current.anime) onShowAnime(it.id) else onShowManga(it.id) },
                onDismissRequest = { dialog = null },
            )
        }
    }
}

/** The logo (A in the text color, halo in gold) and the app name. */
@Composable
private fun HomeTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(30.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_logo_halo_back),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                painter = painterResource(R.drawable.ic_logo_a),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(R.drawable.ic_logo_halo_front),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(MR.strings.app_name),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    hiddenCount: Int = 0,
    onClickHidden: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.small,
                top = MaterialTheme.padding.small,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            modifier = Modifier.weight(1f),
        )
        if (hiddenCount > 0 && onClickHidden != null) {
            TextButton(onClick = onClickHidden) {
                Text(
                    text = stringResource(TLMR.strings.anitenshi_continue_hidden_count, hiddenCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** AniTenshi: "More" in the Home top bar on phones, as an outlined pill with its name. */
@Composable
private fun MorePill(onClick: () -> Unit) {
    // Gold in the dark theme, night blue in the light one
    val color = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = color,
        border = BorderStroke(1.5.dp, color),
        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(start = 10.dp, end = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(MR.strings.label_more),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHint(text: String, isError: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        modifier = Modifier.padding(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.small,
        ),
    ) {
        if (isError) {
            AniTenshiMark(isError = true, size = 32.dp)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContinueSection(
    title: String,
    emptyText: String,
    entries: List<ContinueEntry>,
    hiddenCount: Int,
    isAnime: Boolean,
    onClickHidden: () -> Unit,
    onClick: (ContinueEntry) -> Unit,
    onLongClick: (ContinueEntry) -> Unit,
) {
    SectionTitle(text = title, hiddenCount = hiddenCount, onClickHidden = onClickHidden)
    if (entries.isEmpty()) {
        SectionHint(emptyText)
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            ContinueCard(
                entry = entry,
                isAnime = isAnime,
                onClick = { onClick(entry) },
                onLongClick = { onLongClick(entry) },
            )
        }
    }
}

/** A title to continue: cover with the gold play (or book) button, name, and how much is left. */
@Composable
private fun ContinueCard(
    entry: ContinueEntry,
    isAnime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val remaining = entry.remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    Column(
        modifier = Modifier
            .width(ContinueCoverWidth)
            .combinedClickableCard(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            ItemCover.Book(
                data = entry.cover,
                modifier = Modifier.width(ContinueCoverWidth),
                shape = RoundedCornerShape(12.dp),
            )
            // Gold line along the bottom of the cover: how far into the next episode you got
            entry.progress?.let { progress ->
                Box(
                    contentAlignment = Alignment.BottomStart,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(AniTenshiPalette.Night.copy(alpha = 0.6f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(AniTenshiPalette.Gold),
                        )
                    }
                }
            }
            // Gold pill: play (or book) and the episode / chapter that a tap opens
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ContinuePillGap),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ContinuePillMargin)
                    .heightIn(min = 30.dp)
                    .background(AniTenshiPalette.Gold, CircleShape)
                    .padding(horizontal = ContinuePillPadding, vertical = 6.dp),
            ) {
                Icon(
                    painter = painterResource(if (isAnime) R.drawable.ic_nav_anime else R.drawable.ic_nav_manga),
                    contentDescription = null,
                    tint = AniTenshiPalette.Navy,
                    modifier = Modifier.size(ContinuePillIconSize),
                )
                entry.nextNumber?.let { number ->
                    val numberText = if (isAnime) formatEpisodeNumber(number) else formatChapterNumber(number)
                    val label = stringResource(
                        if (isAnime) TLMR.strings.anitenshi_episode_short else TLMR.strings.anitenshi_chapter_short,
                        numberText,
                    )
                    val style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AniTenshiPalette.Navy,
                    )
                    // AniTenshi: a four-digit number ("Cap. 1165") was too wide for the pill: the text wrapped and
                    // only "Cap." showed. The label now stays on one line and shrinks a little to fit. When it
                    // wouldn't fit even at its smallest (big system fonts), the number shows alone: the icon
                    // already says what it is, and a number cut short would be a wrong number.
                    val textMeasurer = rememberTextMeasurer()
                    val density = LocalDensity.current
                    val labelFits = remember(label, style, density) {
                        val smallest = textMeasurer.measure(
                            text = label,
                            style = style.copy(fontSize = ContinuePillMinFontSize),
                            softWrap = false,
                            maxLines = 1,
                        )
                        smallest.size.width <= with(density) { ContinuePillLabelMaxWidth.roundToPx() }
                    }
                    BasicText(
                        text = if (labelFits) label else numberText,
                        style = style,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = ContinuePillMinFontSize,
                            maxFontSize = ContinuePillMaxFontSize,
                        ),
                    )
                }
            }
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = pluralStringResource(
                if (isAnime) TLMR.plurals.anitenshi_episodes_left else TLMR.plurals.anitenshi_chapters_left,
                count = remaining,
                remaining,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun <T> LatestSection(
    title: String,
    emptyText: String,
    rows: List<LatestRow<T>>?,
    onClickSource: (LatestRow<T>) -> Unit,
    covers: @Composable (List<T>) -> Unit,
) {
    SectionTitle(text = title)
    when {
        rows == null -> LoadingRow()

        rows.isEmpty() -> SectionHint(emptyText)

        else -> rows.forEach { row ->
            SourceHeader(
                name = row.name,
                language = row.language,
                onClick = { onClickSource(row) },
            )
            val titles = row.items
            when {
                titles == null -> LoadingRow()
                row.error != null -> SectionHint(row.error, isError = true)
                titles.isEmpty() -> SectionHint(stringResource(MR.strings.no_results_found))
                else -> covers(titles)
            }
        }
    }
}

@Composable
private fun SourceHeader(
    name: String,
    language: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = language,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> LatestRowCovers(
    titles: List<T>,
    card: @Composable (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(titles) { item -> card(item) }
    }
}

@Composable
private fun LatestCard(
    title: String,
    cover: Any,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(LatestCoverWidth)
            .combinedClickableCard(onClick = onClick, onLongClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ItemCover.Book(
            data = cover,
            modifier = Modifier.width(LatestCoverWidth),
            shape = RoundedCornerShape(10.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.medium),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

private sealed interface HomeDialog {
    /** Long press on a title of a continue row. */
    data class Actions(val entry: ContinueEntry, val anime: Boolean) : HomeDialog

    /** The titles hidden from a continue row. */
    data class Hidden(val anime: Boolean) : HomeDialog
}

@Composable
private fun ContinueActionsDialog(
    entry: ContinueEntry,
    hideLabel: String,
    onOpen: () -> Unit,
    onHide: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                TextButton(onClick = onOpen) {
                    Text(text = stringResource(TLMR.strings.anitenshi_continue_open_entry))
                }
                TextButton(onClick = onHide) {
                    Text(text = hideLabel)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun HiddenEntriesDialog(
    title: String,
    entries: List<ContinueEntry>,
    onShow: (ContinueEntry) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            // A plain scrolling column: lazy lists are not safe inside dialogs
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onShow(entry) }) {
                            Text(text = stringResource(TLMR.strings.anitenshi_continue_show_again))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_close))
            }
        },
    )
}

/** Tap and long press on a whole card, with the ripple clipped to the card's corners. */
private fun Modifier.combinedClickableCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
