package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import eu.kanade.presentation.components.CardSearchBar
import eu.kanade.presentation.more.settings.screen.SettingsSearchScreen
import eu.kanade.presentation.more.settings.screen.player.custombutton.PlayerSettingsCustomButtonScreen
import eu.kanade.presentation.more.settings.screen.player.editor.PlayerSettingsEditorScreen
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import mihon.app.di.appGraph
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Magnet
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

class PlayerSettingsMainScreen(private val mainSettings: Boolean) : Screen() {
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
     * AniTenshi: the player's pages in named groups, one card each, as on the first page of the settings, with a
     * search bar under the title. A few entries say what they are set to.
     */
    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val state = rememberLazyListState()

        Scaffold(
            // The search bar stays under the title while the list scrolls, as on the first page of the settings
            topBar = {
                Column(modifier = Modifier.background(containerColor)) {
                    AppBar(
                        title = stringResource(
                            if (mainSettings) {
                                // AniTenshi: "Anime player", the name it has on the first page of the settings
                                TLMR.strings.anitenshi_settings_player_anime
                            } else {
                                AYMR.strings.label_player_settings
                            },
                        ),
                        backgroundColor = containerColor,
                        navigateUp = backPress::invoke,
                    )
                    CardSearchBar(
                        text = stringResource(TLMR.strings.anitenshi_player_search),
                        onClick = { navigator.navigate(SettingsSearchScreen(true), twoPane) },
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
                        CardGroup(title = stringResource(group.titleRes)) {
                            group.items.forEach { item ->
                                CardEntry(
                                    title = stringResource(item.titleRes),
                                    subtitle = item.subtitle(),
                                    icon = item.icon,
                                    onClick = { navigator.navigate(item.screen, twoPane) },
                                    // Phones open the page over this one; tablets show it beside the list
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

    private companion object {
        data class Group(
            val titleRes: StringResource,
            val items: List<Item>,
        )

        data class Item(
            val titleRes: StringResource,
            val icon: ImageVector,
            val screen: VoyagerScreen,
            val subtitle: @Composable () -> String?,
        )

        // "ita,eng" as the person reads it: "Preferred languages: ita, eng"
        @Composable
        fun languagesSubtitle(languages: String): String {
            val list = languages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return if (list.isEmpty()) {
                stringResource(TLMR.strings.anitenshi_player_no_language)
            } else {
                pluralStringResource(
                    TLMR.plurals.anitenshi_player_languages,
                    count = list.size,
                    list.joinToString(", "),
                )
            }
        }

        val groups = listOf(
            Group(
                titleRes = TLMR.strings.anitenshi_player_group_playback,
                items = listOf(
                    Item(
                        titleRes = AYMR.strings.pref_player_internal,
                        icon = Icons.Outlined.PlayCircleOutline,
                        screen = PlayerSettingsPlayerScreen,
                        subtitle = { stringResource(AYMR.strings.pref_player_internal_summary) },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_gestures,
                        icon = Icons.Outlined.Gesture,
                        screen = PlayerSettingsGesturesScreen,
                        subtitle = { stringResource(AYMR.strings.pref_player_gestures_summary) },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_decoder,
                        icon = Icons.Outlined.Memory,
                        screen = PlayerSettingsDecoderScreen,
                        subtitle = {
                            val context = LocalContext.current
                            val hardware by remember { context.appGraph.decoderPreferences.tryHWDecoding() }
                                .collectAsState()
                            stringResource(
                                if (hardware) {
                                    TLMR.strings.anitenshi_player_hwdec_on
                                } else {
                                    TLMR.strings.anitenshi_player_hwdec_off
                                },
                            )
                        },
                    ),
                ),
            ),
            Group(
                titleRes = TLMR.strings.anitenshi_player_group_audio_subtitles,
                items = listOf(
                    Item(
                        titleRes = AYMR.strings.pref_player_subtitle,
                        icon = Icons.Outlined.Subtitles,
                        screen = PlayerSettingsSubtitleScreen,
                        subtitle = {
                            val context = LocalContext.current
                            val languages by remember {
                                context.appGraph.subtitlePreferences.preferredSubLanguages()
                            }.collectAsState()
                            languagesSubtitle(languages)
                        },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_audio,
                        icon = Icons.Outlined.Audiotrack,
                        screen = PlayerSettingsAudioScreen,
                        subtitle = {
                            val context = LocalContext.current
                            val languages by remember {
                                context.appGraph.audioPreferences.preferredAudioLanguages()
                            }.collectAsState()
                            languagesSubtitle(languages)
                        },
                    ),
                ),
            ),
            Group(
                titleRes = TLMR.strings.anitenshi_player_group_expert,
                items = listOf(
                    Item(
                        titleRes = AYMR.strings.pref_player_custom_button,
                        icon = Icons.Outlined.Terminal,
                        screen = PlayerSettingsCustomButtonScreen,
                        subtitle = { stringResource(TLMR.strings.anitenshi_player_custom_button_summary) },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_editor,
                        icon = Icons.Outlined.EditNote,
                        screen = PlayerSettingsEditorScreen,
                        subtitle = { stringResource(TLMR.strings.anitenshi_player_editor_summary) },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_torrents,
                        icon = CustomIcons.Magnet,
                        screen = PlayerSettingsTorrentScreen,
                        subtitle = { stringResource(AYMR.strings.pref_player_torrents_summary) },
                    ),
                    Item(
                        titleRes = AYMR.strings.pref_player_advanced,
                        icon = Icons.Outlined.Code,
                        screen = PlayerSettingsAdvancedScreen,
                        subtitle = { stringResource(AYMR.strings.pref_player_advanced_summary) },
                    ),
                ),
            ),
        )
    }
}
