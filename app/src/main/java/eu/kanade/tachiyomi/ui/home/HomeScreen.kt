package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.AnimeBrowseScreen
import eu.kanade.tachiyomi.ui.browse.MangaBrowseScreen
import eu.kanade.tachiyomi.ui.download.DownloadsTab
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.cast.CastMiniController
import eu.kanade.tachiyomi.ui.updates.AnimeUpdatesScreen
import eu.kanade.tachiyomi.ui.updates.MangaUpdatesScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 200
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = context.appGraph.uiPreferences
        val navigator = LocalNavigator.currentOrThrow
        // SY -->
        val scope = rememberCoroutineScope()
        val alwaysShowLabel by remember {
            uiPreferences.bottomBarLabels.asState(scope)
        }
        // SY <--
        val activity = context as? ComponentActivity
        val preferences = context.appGraph.preferenceStore
        val castManager = remember { CastManager(activity!!, preferences) }

        LaunchedEffect(Unit) {
            castManager.startDeviceDiscovery()
            castManager.reconnect()
        }

        // AniTenshi: the bottom bar is always Home, Anime and Manga.
        // Everything else opens from Home (More button) or from the libraries (+ and bell).
        val tabs = remember { listOf<eu.kanade.presentation.util.Tab>(HomeTab, AnimeLibraryTab, MangaLibraryTab) }
        val startScreenPref by uiPreferences.startScreen.collectAsState()
        val effectiveStartTab = remember(startScreenPref) { startScreenPref.tab }

        TabNavigator(
            tab = effectiveStartTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isTabletUi()) {
                        NavigationRail {
                            tabs.fastForEach {
                                NavigationRailItem(it, alwaysShowLabel)
                            }
                        }
                    }
                    Scaffold(
                        bottomBar = {
                            if (!isTabletUi()) {
                                Column {
                                    val isConnected =
                                        castManager.castState.collectAsState().value == CastManager.CastState.CONNECTED
                                    AnimatedVisibility(
                                        visible = isConnected,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                    ) {
                                        CastMiniController(
                                            castManager = castManager,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                        )
                                    }

                                    val bottomNavVisible by produceState(initialValue = true) {
                                        showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                                    }

                                    AnimatedVisibility(
                                        visible = bottomNavVisible,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                    ) {
                                        NavigationBar {
                                            tabs.fastForEach {
                                                NavigationBarItem(it, alwaysShowLabel)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        contentWindowInsets = WindowInsets(0),
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier
                                .padding(contentPadding)
                                .consumeWindowInsets(contentPadding),
                        ) {
                            AnimatedContent(
                                targetState = tabNavigator.current,
                                transitionSpec = {
                                    materialFadeThroughIn(
                                        initialScale = 1f,
                                        durationMillis = TAB_FADE_DURATION,
                                    ) togetherWith
                                        materialFadeThroughOut(durationMillis = TAB_FADE_DURATION)
                                },
                                label = "tabContent",
                            ) {
                                tabNavigator.saveableState(key = "currentTab", it) {
                                    it.Content()
                                }
                            }
                        }
                    }
                }
            }

            val goToStartScreen = {
                tabNavigator.current = effectiveStartTab
            }
            BackHandler(
                enabled = tabNavigator.current != effectiveStartTab,
                onBack = goToStartScreen,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToStartScreen()
                        when (effectiveStartTab) {
                            AnimeLibraryTab -> AnimeLibraryTab.search(it)
                            MangaLibraryTab -> MangaLibraryTab.search(it)
                            else -> {}
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Home -> HomeTab
                            is Tab.AnimeLib -> AnimeLibraryTab
                            is Tab.Library -> MangaLibraryTab
                            is Tab.Updates -> if (it.anime) AnimeLibraryTab else MangaLibraryTab
                            is Tab.History -> HomeTab
                            is Tab.Browse -> if (it.anime) AnimeLibraryTab else MangaLibraryTab
                            is Tab.More -> HomeTab
                        }

                        when (it) {
                            is Tab.AnimeLib -> it.animeIdToOpen?.let { id -> navigator.push(AnimeScreen(id)) }
                            is Tab.Library -> it.mangaIdToOpen?.let { id -> navigator.push(MangaScreen(id)) }
                            is Tab.Updates -> navigator.push(if (it.anime) AnimeUpdatesScreen() else MangaUpdatesScreen())
                            is Tab.History -> navigator.push(HistoriesTab)
                            is Tab.Browse -> navigator.push(
                                if (it.anime) {
                                    AnimeBrowseScreen(toExtensions = it.toExtensions)
                                } else {
                                    MangaBrowseScreen(toExtensions = it.toExtensions)
                                },
                            )
                            is Tab.More -> navigator.push(if (it.toDownloads) DownloadsTab else MoreTab)
                            is Tab.Home -> {}
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun RowScope.NavigationBarItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = {
                    if (!selected) {
                        tabNavigator.current = tab
                    } else {
                        scope.launch { tab.onReselect(navigator) }
                    }
                },
                onLongClick = {
                    if (!selected) {
                        tabNavigator.current = tab
                    }
                    scope.launch { tab.onReselect(navigator) }
                },
            ),
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = alwaysShowLabel,
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = {
                    if (!selected) {
                        tabNavigator.current = tab
                    } else {
                        scope.launch { tab.onReselect(navigator) }
                    }
                },
                onLongClick = {
                    if (!selected) {
                        tabNavigator.current = tab
                    }
                    scope.launch { tab.onReselect(navigator) }
                },
            ),
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = alwaysShowLabel,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        val context = LocalContext.current
        BadgedBox(
            badge = {
                // New episodes / chapters of the library: shown on the library they belong to
                val isAnimeLibrary = AnimeLibraryTab::class.isInstance(tab)
                val isMangaLibrary = MangaLibraryTab::class.isInstance(tab)
                if (isAnimeLibrary || isMangaLibrary) {
                    val count by produceState(initialValue = 0, isAnimeLibrary) {
                        val pref = context.appGraph.libraryPreferences
                        val counter = if (isAnimeLibrary) pref.newAnimeUpdatesCount else pref.newMangaUpdatesCount
                        counter.changes()
                            .collectLatest { value = if (pref.newShowUpdatesCount.get()) it else 0 }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                MR.plurals.notification_chapters_generic,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                // TODO: https://issuetracker.google.com/u/0/issues/316327367
                tint = LocalContentColor.current,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data object Home : Tab
        data class AnimeLib(val animeIdToOpen: Long? = null) : Tab
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data class Updates(val anime: Boolean) : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
