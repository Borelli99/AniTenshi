package eu.kanade.presentation.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.CardEntry
import eu.kanade.presentation.components.CardGroup
import eu.kanade.presentation.components.CardSwitchEntry
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

// On wide screens the entries stay in a column in the middle instead of stretching edge to edge
private val MaxContentWidth = 640.dp

/**
 * The "More" page.
 *
 * AniTenshi: the settings are the first thing on it, in a card of their own. The rest is gathered in named
 * groups, as on the first page of the settings (see [CardGroup]), and entries say where things stand: what
 * is left to download, how many categories there are, when the last sync and the last backup were.
 */
@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickHistory: () -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickNetworkStream: () -> Unit,
    onClickStorage: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    navigateUp: (() -> Unit)? = null,
    /** AniTenshi: donation page; hidden while there are no donation links. */
    onClickSupport: (() -> Unit)? = null,
    /** AniTenshi: the sync status row (see [SyncStatusRow]); tapping it opens the sync settings. */
    syncStatus: SyncStatus = SyncStatus.Off,
    onClickSync: () -> Unit = {},
    onClickSyncNow: () -> Unit = {},
    /** AniTenshi: how many anime and manga categories there are; null while they are being counted. */
    categoryCounts: Pair<Int, Int>? = null,
    /** AniTenshi: when the last automatic backup was made, 0 if never. */
    lastAutoBackup: Long = 0L,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { scrollBehavior ->
            // More is opened from the Home button, so it gets a way back
            if (navigateUp != null) {
                AppBar(
                    title = stringResource(MR.strings.label_more),
                    navigateUp = navigateUp,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { contentPadding ->
        val block = Modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()

        ScrollbarLazyColumn(
            contentPadding = contentPadding + PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Icon(
                    painter = painterResource(R.drawable.ic_ani),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(56.dp),
                )
            }

            item {
                SettingsCard(
                    onClick = onClickSettings,
                    modifier = block,
                )
            }

            item {
                CardGroup(
                    title = stringResource(TLMR.strings.anitenshi_more_group_modes),
                    modifier = block,
                ) {
                    CardSwitchEntry(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(TLMR.strings.anitenshi_more_downloaded_only_summary),
                        icon = MaterialSymbols.Rounded.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChange = onDownloadedOnlyChange,
                    )
                    CardSwitchEntry(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(TLMR.strings.anitenshi_more_incognito_summary),
                        icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                        checked = incognitoMode,
                        onCheckedChange = onIncognitoModeChange,
                    )
                }
            }

            item {
                CardGroup(
                    title = stringResource(TLMR.strings.anitenshi_more_group_library),
                    modifier = block,
                ) {
                    CardEntry(
                        title = stringResource(MR.strings.history),
                        icon = Icons.Outlined.History,
                        onClick = onClickHistory,
                    )
                    val downloadQueueState = downloadQueueStateProvider()
                    CardEntry(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null

                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }

                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(
                                    MR.plurals.download_queue_summary,
                                    count = pending,
                                    pending,
                                )
                            }
                        },
                        icon = MaterialSymbols.Rounded.Download,
                        onClick = onClickDownloadQueue,
                    )
                    CardEntry(
                        title = stringResource(AYMR.strings.general_categories),
                        // Nothing to say while there are none
                        subtitle = categoryCounts
                            ?.takeIf { (anime, manga) -> anime + manga > 0 }
                            ?.let { (anime, manga) ->
                                stringResource(TLMR.strings.anitenshi_more_categories_summary, anime, manga)
                            },
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        onClick = onClickCategories,
                    )
                    CardEntry(
                        title = stringResource(MR.strings.label_stats),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        onClick = onClickStats,
                    )
                }
            }

            item {
                CardGroup(
                    title = stringResource(TLMR.strings.anitenshi_more_group_data),
                    modifier = block,
                ) {
                    // AniTenshi: automatic syncs are quiet; this row says when the last one was and when
                    // something is wrong
                    SyncStatusRow(
                        status = syncStatus,
                        onClick = onClickSync,
                        onClickSyncNow = onClickSyncNow,
                    )
                    CardEntry(
                        title = stringResource(MR.strings.label_data_storage),
                        subtitle = stringResource(
                            MR.strings.last_auto_backup_info,
                            relativeTimeSpanString(lastAutoBackup),
                        ),
                        icon = MaterialSymbols.Rounded.Storage,
                        onClick = onClickDataAndStorage,
                    )
                }
            }

            item {
                CardGroup(
                    title = stringResource(TLMR.strings.anitenshi_more_group_tools),
                    modifier = block,
                ) {
                    CardEntry(
                        title = stringResource(TLMR.strings.anitenshi_more_open_link),
                        subtitle = stringResource(TLMR.strings.anitenshi_more_open_link_summary),
                        icon = Icons.Outlined.Link,
                        onClick = onClickNetworkStream,
                    )
                }
            }

            item {
                CardGroup(
                    title = stringResource(TLMR.strings.anitenshi_more_group_app),
                    modifier = block,
                ) {
                    CardEntry(
                        title = stringResource(MR.strings.pref_category_about),
                        subtitle = "${stringResource(MR.strings.app_name)} ${
                            AboutScreen.getVersionName(withBuildDate = false)
                        }",
                        icon = MaterialSymbols.Rounded.Info,
                        onClick = onClickAbout,
                    )
                    if (onClickSupport != null) {
                        CardEntry(
                            title = stringResource(TLMR.strings.anitenshi_support_title),
                            icon = Icons.Outlined.FavoriteBorder,
                            onClick = onClickSupport,
                        )
                    }
                    CardEntry(
                        title = stringResource(MR.strings.label_help),
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }
        }
    }
}

/**
 * AniTenshi: the way into the settings, made to stand out from the entries below it: a card of its own with
 * an accent border, right under the logo.
 */
@Composable
private fun SettingsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.5.dp, colors.secondary),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Settings,
                    contentDescription = null,
                    tint = colors.onSecondary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.label_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(TLMR.strings.anitenshi_more_settings_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.secondary,
            )
        }
    }
}
