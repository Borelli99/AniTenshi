package eu.kanade.tachiyomi.ui.updates.failed

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.library.FailedUpdate
import eu.kanade.tachiyomi.data.library.FailedUpdateReason
import eu.kanade.tachiyomi.data.library.FailedUpdates
import eu.kanade.tachiyomi.ui.browse.AnimeBrowseScreen
import eu.kanade.tachiyomi.ui.browse.MangaBrowseScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import mihon.app.di.appGraph
import mihon.feature.migration.config.AnimeMigrationConfigScreen
import mihon.feature.migration.config.MangaMigrationConfigScreen
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.collectAsState

/** One failed title as the screen shows it. */
@Immutable
private data class FailedEntry(
    val update: FailedUpdate,
    val isAnime: Boolean,
    val muted: Boolean,
)

/**
 * AniTenshi: the titles the library update couldn't update, opened from the "updates failed" notification (which
 * used to open a text file) and from the updates' list. Each title has its cover and opens its page; the reason is
 * said in plain words; one title, or the whole notification, can be silenced; "Retry" updates these titles alone.
 */
class FailedUpdatesScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember { context.appGraph.libraryPreferences }

        val animeJson by preferences.failedAnimeUpdates.collectAsState()
        val mangaJson by preferences.failedMangaUpdates.collectAsState()
        val mutedAnime by preferences.mutedFailedAnimeUpdates.collectAsState()
        val mutedManga by preferences.mutedFailedMangaUpdates.collectAsState()
        val notify by preferences.notifyFailedUpdates.collectAsState()

        val animeState = remember(animeJson) { FailedUpdates.decode(animeJson) }
        val mangaState = remember(mangaJson) { FailedUpdates.decode(mangaJson) }
        val entries = remember(animeState, mangaState, mutedAnime, mutedManga) {
            animeState.items.map { FailedEntry(it, isAnime = true, muted = it.id.toString() in mutedAnime) } +
                mangaState.items.map { FailedEntry(it, isAnime = false, muted = it.id.toString() in mutedManga) }
        }
        val active = entries.filterNot { it.muted }
        val muted = entries.filter { it.muted }
        val lastTime = maxOf(animeState.time, mangaState.time)

        // The covers come from the library; a title no longer there keeps the placeholder
        val animeCovers by produceState(emptyMap<Long, Any>(), animeState) {
            value = withIOContext {
                animeState.items.mapNotNull { item ->
                    context.appGraph.getAnime.await(item.id)?.let { item.id to it.asAnimeCover() }
                }.toMap()
            }
        }
        val mangaCovers by produceState(emptyMap<Long, Any>(), mangaState) {
            value = withIOContext {
                mangaState.items.mapNotNull { item ->
                    context.appGraph.getManga.await(item.id)?.let { item.id to it.asMangaCover() }
                }.toMap()
            }
        }

        val toggleMuted: (FailedEntry) -> Unit = { entry ->
            val preference = if (entry.isAnime) {
                preferences.mutedFailedAnimeUpdates
            } else {
                preferences.mutedFailedMangaUpdates
            }
            toggle(preference, entry.update.id.toString())
            FailedUpdates.refreshNotification(context)
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(TLMR.strings.anitenshi_failed_updates_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                BottomBar(
                    notify = notify,
                    onNotifyChange = {
                        preferences.notifyFailedUpdates.set(it)
                        FailedUpdates.refreshNotification(context)
                    },
                    retryCount = active.size,
                    onRetry = {
                        FailedUpdates.retry(context)
                        context.toast(MR.strings.updating_library)
                    },
                )
            },
        ) { contentPadding ->
            if (entries.isEmpty()) {
                EmptyScreen(
                    stringRes = TLMR.strings.anitenshi_failed_updates_none,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentPadding.calculateTopPadding() + 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.widthIn(max = MaxContentWidth),
                ) {
                    if (active.isNotEmpty()) {
                        item(key = "summary") {
                            val count = pluralStringResource(
                                TLMR.plurals.anitenshi_failed_updates_count,
                                count = active.size,
                                active.size,
                            )
                            val time = if (lastTime > 0) {
                                DateUtils.getRelativeTimeSpanString(lastTime).toString()
                            } else {
                                null
                            }
                            Text(
                                text = listOfNotNull(count, time).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // One card for each reason, in the order the reasons first appear
                    val groups = active.groupBy { it.update.reason to it.update.detail }.toList()
                    items(groups, key = { (reason, _) -> "reason-${reason.first}-${reason.second}" }) { (reason, group) ->
                        ReasonCard(
                            reason = reasonText(reason.first, reason.second),
                            technical = group.first().update.error,
                        ) {
                            group.forEach { entry ->
                                EntryRow(
                                    entry = entry,
                                    cover = (if (entry.isAnime) animeCovers else mangaCovers)[entry.update.id],
                                    onClick = { navigator.push(entryScreen(entry)) },
                                    onToggleMuted = { toggleMuted(entry) },
                                    action = entryAction(entry),
                                )
                            }
                        }
                    }

                    if (muted.isNotEmpty()) {
                        item(key = "muted-title") {
                            Text(
                                text = stringResource(TLMR.strings.anitenshi_failed_updates_muted),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        item(key = "muted") {
                            ReasonCard(reason = null, technical = null) {
                                muted.forEach { entry ->
                                    EntryRow(
                                        entry = entry,
                                        cover = (if (entry.isAnime) animeCovers else mangaCovers)[entry.update.id],
                                        onClick = { navigator.push(entryScreen(entry)) },
                                        onToggleMuted = { toggleMuted(entry) },
                                        action = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun entryScreen(entry: FailedEntry): Screen {
        return if (entry.isAnime) AnimeScreen(entry.update.id) else MangaScreen(entry.update.id)
    }

    /** What can be done about a title, by its reason: a label and where it leads. Null when there is nothing. */
    @Composable
    private fun entryAction(entry: FailedEntry): Pair<String, () -> Unit>? {
        val navigator = LocalNavigator.currentOrThrow
        val update = entry.update
        return when (update.reason) {
            // The site is gone: the title can move to another source
            FailedUpdateReason.SITE_UNREACHABLE -> stringResource(TLMR.strings.anitenshi_failed_updates_migrate) to {
                navigator.push(
                    if (entry.isAnime) AnimeMigrationConfigScreen(update.id) else MangaMigrationConfigScreen(update.id),
                )
            }
            FailedUpdateReason.EXTENSION_MISSING -> stringResource(TLMR.strings.anitenshi_extension_search) to {
                val search = update.sourceName.ifBlank { update.sourceId.toString() }
                navigator.push(
                    if (entry.isAnime) {
                        AnimeBrowseScreen(extensionSearch = search)
                    } else {
                        MangaBrowseScreen(extensionSearch = search)
                    },
                )
            }
            else -> null
        }
    }

    private fun toggle(preference: Preference<Set<String>>, id: String) {
        val current = preference.get()
        preference.set(if (id in current) current - id else current + id)
    }

    private companion object {
        val MaxContentWidth = 600.dp
    }
}

@Composable
private fun reasonText(reason: FailedUpdateReason, detail: String?): String {
    return when (reason) {
        FailedUpdateReason.SITE_UNREACHABLE -> if (detail.isNullOrBlank()) {
            stringResource(TLMR.strings.anitenshi_failed_reason_unreachable)
        } else {
            stringResource(TLMR.strings.anitenshi_failed_reason_unreachable_host, detail)
        }
        FailedUpdateReason.SITE_NOT_RESPONDING -> stringResource(TLMR.strings.anitenshi_failed_reason_not_responding)
        FailedUpdateReason.SITE_ERROR -> stringResource(TLMR.strings.anitenshi_failed_reason_site_error, detail.orEmpty())
        FailedUpdateReason.EXTENSION_MISSING -> stringResource(TLMR.strings.anitenshi_extension_missing_title)
        FailedUpdateReason.NOTHING_FOUND -> stringResource(TLMR.strings.anitenshi_failed_reason_nothing_found)
        FailedUpdateReason.OTHER -> stringResource(TLMR.strings.anitenshi_failed_reason_other)
    }
}

/** A card of titles that failed for the same [reason], said in plain words, with the [technical] error in small. */
@Composable
private fun ReasonCard(
    reason: String?,
    technical: String?,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            if (reason != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (!technical.isNullOrBlank()) {
                            Text(
                                text = technical,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun EntryRow(
    entry: FailedEntry,
    cover: Any?,
    onClick: () -> Unit,
    onToggleMuted: () -> Unit,
    action: Pair<String, () -> Unit>?,
) {
    val update = entry.update
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ItemCover.Book(
            data = cover,
            modifier = Modifier.width(44.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = update.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val kind = stringResource(if (entry.isAnime) MR.strings.label_anime else MR.strings.label_manga)
            Text(
                text = listOf(update.sourceName, kind).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (action != null) {
                OutlinedButton(
                    onClick = action.second,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.heightIn(min = 32.dp),
                ) {
                    Text(text = action.first, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        IconButton(onClick = onToggleMuted) {
            Icon(
                imageVector = if (entry.muted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                contentDescription = stringResource(
                    if (entry.muted) {
                        TLMR.strings.anitenshi_failed_updates_unmute_title
                    } else {
                        TLMR.strings.anitenshi_failed_updates_mute_title
                    },
                ),
                tint = if (entry.muted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** The switch for the notification and the gold "Retry", always in reach at the bottom. */
@Composable
private fun BottomBar(
    notify: Boolean,
    onNotifyChange: (Boolean) -> Unit,
    retryCount: Int,
    onRetry: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider()
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNotifyChange(!notify) },
                    ) {
                        Text(
                            text = stringResource(TLMR.strings.anitenshi_failed_updates_notify),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = notify, onCheckedChange = onNotifyChange)
                    }
                    if (retryCount > 0) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    TLMR.plurals.anitenshi_failed_updates_retry,
                                    count = retryCount,
                                    retryCount,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AniTenshi: at the top of the updates' list when titles failed to update and the notification about it is switched
 * off, so that they are still heard of. Opens [FailedUpdatesScreen]. Nothing otherwise.
 */
@Composable
fun FailedUpdatesRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val preferences = remember { context.appGraph.libraryPreferences }
    val notify by preferences.notifyFailedUpdates.collectAsState()
    val animeJson by preferences.failedAnimeUpdates.collectAsState()
    val mangaJson by preferences.failedMangaUpdates.collectAsState()
    val mutedAnime by preferences.mutedFailedAnimeUpdates.collectAsState()
    val mutedManga by preferences.mutedFailedMangaUpdates.collectAsState()
    val count = remember(animeJson, mangaJson, mutedAnime, mutedManga) {
        FailedUpdates.decode(animeJson).items.count { it.id.toString() !in mutedAnime } +
            FailedUpdates.decode(mangaJson).items.count { it.id.toString() !in mutedManga }
    }
    if (notify || count == 0) return

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { navigator.push(FailedUpdatesScreen()) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = pluralStringResource(TLMR.plurals.anitenshi_failed_updates_count, count = count, count),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
