package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.CardEntry
import eu.kanade.presentation.components.CardEntryArrow
import eu.kanade.presentation.components.CardGroup
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.setting.PlayerSettingsScreen
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import mihon.icons.materialsymbols.rounded.Code
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Security
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.rounded.Sync
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {
    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    /**
     * AniTenshi: the first page of the settings. A search bar that is always there (it opens the settings
     * search, which used to hide behind a small magnifier), and the pages gathered in a few named groups,
     * each in its own card. Under each page is what it is set to now, where one setting sums the page up,
     * or a few words on what is inside.
     */
    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val state = rememberLazyListState()

        Scaffold(
            // The search bar stays under the title while the list scrolls. The bar keeps the color of the
            // page (no scroll behavior), and a thin line shows up under it once the list has moved.
            topBar = {
                Column(modifier = Modifier.background(containerColor)) {
                    AppBar(
                        title = stringResource(MR.strings.label_settings),
                        backgroundColor = containerColor,
                        navigateUp = backPress::invoke,
                    )
                    SettingsSearchBar(
                        onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    )
                    HorizontalDivider(
                        color = if (state.canScrollBackward) {
                            MaterialTheme.colorScheme.outlineVariant
                        } else {
                            Color.Transparent
                        },
                    )
                }
            },
            containerColor = containerColor,
            content = { contentPadding ->
                // On tablets the page shown on the right is marked on this list
                val openScreen = if (twoPane) navigator.items.first()::class else null
                if (twoPane) {
                    LaunchedEffect(Unit) {
                        val groupIndex = groups.indexOfFirst { group ->
                            group.items.any { it.screen::class == openScreen }
                        }
                        if (groupIndex > 0) state.animateScrollToItem(groupIndex)
                    }
                }

                LazyColumn(
                    state = state,
                    contentPadding = contentPadding +
                        PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(
                        count = groups.size,
                        key = { it },
                    ) { index ->
                        val group = groups[index]
                        CardGroup(
                            title = stringResource(group.titleRes),
                        ) {
                            group.items.forEach { item ->
                                CardEntry(
                                    title = stringResource(item.titleRes),
                                    subtitle = item.subtitle(),
                                    icon = item.icon,
                                    onClick = { navigator.navigate(item.screen, twoPane) },
                                    // Phones open the page over this one; tablets show it beside the list,
                                    // so there the entry of the open page is marked and the arrow is left out
                                    selected = if (twoPane) item.screen::class == openScreen else null,
                                    trailing = if (twoPane) null else ({ CardEntryArrow() }),
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    @Composable
    private fun SettingsSearchBar(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(TLMR.strings.anitenshi_settings_search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    private data class Group(
        val titleRes: StringResource,
        val items: List<Item>,
    )

    private data class Item(
        val titleRes: StringResource,
        val icon: ImageVector,
        val screen: VoyagerScreen,
        val subtitle: @Composable () -> String?,
    )

    private val groups = listOf(
        Group(
            titleRes = TLMR.strings.anitenshi_settings_group_app,
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_appearance,
                    icon = MaterialSymbols.Rounded.Palette,
                    screen = SettingsAppearanceScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val theme by remember { context.appGraph.uiPreferences.appTheme }.collectAsState()
                        theme.titleRes
                            ?.let { stringResource(TLMR.strings.anitenshi_settings_value_theme, stringResource(it)) }
                            ?: stringResource(MR.strings.pref_appearance_summary)
                    },
                ),
                Item(
                    titleRes = MR.strings.pref_category_security,
                    icon = MaterialSymbols.Rounded.Security,
                    screen = SettingsSecurityScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val locked by remember { context.appGraph.securityPreferences.useAuthenticator }
                            .collectAsState()
                        stringResource(
                            if (locked) {
                                TLMR.strings.anitenshi_settings_value_lock_on
                            } else {
                                TLMR.strings.anitenshi_settings_value_lock_off
                            },
                        )
                    },
                ),
            ),
        ),
        Group(
            titleRes = TLMR.strings.anitenshi_settings_group_library,
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_library,
                    icon = MaterialSymbols.Rounded.CollectionsBookmark,
                    screen = SettingsLibraryScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val interval by remember { context.appGraph.libraryPreferences.autoUpdateInterval }
                            .collectAsState()
                        val every = when (interval) {
                            0 -> MR.strings.update_never
                            12 -> MR.strings.update_12hour
                            24 -> MR.strings.update_24hour
                            48 -> MR.strings.update_48hour
                            72 -> MR.strings.update_72hour
                            168 -> MR.strings.update_weekly
                            else -> null
                        }
                        every
                            ?.let {
                                stringResource(TLMR.strings.anitenshi_settings_value_auto_update, stringResource(it))
                            }
                            ?: stringResource(AYMR.strings.pref_library_summary)
                    },
                ),
                Item(
                    titleRes = MR.strings.browse,
                    icon = MaterialSymbols.Rounded.Explore,
                    screen = SettingsBrowseScreen,
                    subtitle = { stringResource(MR.strings.pref_browse_summary) },
                ),
                Item(
                    titleRes = MR.strings.pref_category_downloads,
                    icon = MaterialSymbols.Rounded.Download,
                    screen = SettingsDownloadScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val onlyWifi by remember { context.appGraph.downloadPreferences.downloadOnlyOverWifi }
                            .collectAsState()
                        stringResource(
                            if (onlyWifi) {
                                TLMR.strings.anitenshi_settings_value_wifi_on
                            } else {
                                TLMR.strings.anitenshi_settings_value_wifi_off
                            },
                        )
                    },
                ),
            ),
        ),
        Group(
            titleRes = TLMR.strings.anitenshi_settings_group_reading,
            items = listOf(
                Item(
                    // "Manga reader" and "Anime player": in some languages the two were both just "Reader"
                    titleRes = TLMR.strings.anitenshi_settings_reader_manga,
                    icon = MaterialSymbols.AutoMirroredRounded.ChromeReaderMode,
                    screen = SettingsReaderScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val mode by remember { context.appGraph.readerPreferences.defaultReadingMode }
                            .collectAsState()
                        stringResource(
                            TLMR.strings.anitenshi_settings_value_reading_mode,
                            stringResource(ReadingMode.fromPreference(mode).stringRes),
                        )
                    },
                ),
                Item(
                    titleRes = TLMR.strings.anitenshi_settings_player_anime,
                    icon = Icons.Outlined.VideoSettings,
                    screen = PlayerSettingsScreen(mainSettings = true),
                    subtitle = { stringResource(TLMR.strings.anitenshi_settings_player_summary) },
                ),
            ),
        ),
        Group(
            titleRes = TLMR.strings.anitenshi_settings_group_account,
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_tracking,
                    icon = MaterialSymbols.Rounded.Sync,
                    screen = SettingsTrackingScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val trackerManager = remember { context.appGraph.trackerManager }
                        val loggedIn by remember { trackerManager.loggedInTrackersFlow() }
                            .collectAsState(initial = trackerManager.loggedInTrackers())
                        if (loggedIn.isEmpty()) {
                            stringResource(TLMR.strings.anitenshi_settings_value_trackers_none)
                        } else {
                            pluralStringResource(
                                TLMR.plurals.anitenshi_settings_value_trackers,
                                count = loggedIn.size,
                                loggedIn.size,
                            )
                        }
                    },
                ),
                // AM (CONNECTIONS) -->
                Item(
                    titleRes = TLMR.strings.pref_category_connections,
                    icon = Icons.Outlined.Link,
                    screen = SettingsConnectionsScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val username by remember {
                            context.appGraph.connectionsPreferences
                                .connectionsUsername(context.appGraph.connectionsManager.discord)
                        }.collectAsState()
                        stringResource(
                            if (username.isNotEmpty()) {
                                TLMR.strings.anitenshi_settings_value_discord_on
                            } else {
                                TLMR.strings.anitenshi_settings_value_discord_off
                            },
                        )
                    },
                ),
                // <-- AM (CONNECTIONS)
                Item(
                    titleRes = MR.strings.label_data_storage,
                    icon = MaterialSymbols.Rounded.Storage,
                    screen = SettingsDataScreen,
                    subtitle = {
                        val context = LocalContext.current
                        val lastBackup by remember { context.appGraph.backupPreferences.lastAutoBackupTimestamp }
                            .collectAsState()
                        stringResource(MR.strings.last_auto_backup_info, relativeTimeSpanString(lastBackup))
                    },
                ),
            ),
        ),
        Group(
            titleRes = TLMR.strings.anitenshi_settings_group_other,
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_advanced,
                    icon = MaterialSymbols.Rounded.Code,
                    screen = SettingsAdvancedScreen,
                    subtitle = { stringResource(TLMR.strings.anitenshi_settings_advanced_summary) },
                ),
                Item(
                    titleRes = MR.strings.pref_category_about,
                    icon = MaterialSymbols.Rounded.Info,
                    screen = AboutScreen,
                    subtitle = {
                        "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(
                            withBuildDate = false,
                        )}"
                    },
                ),
            ),
        ),
    )
}
