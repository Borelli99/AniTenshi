package eu.kanade.tachiyomi.ui.browse.manga

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.manga.MangaSourceOptionsDialog
import eu.kanade.presentation.browse.manga.MangaSourcesAndExtensionsScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.settingsOverflowAction
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionFilterScreen
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionUninstallConfirmation
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.details.MangaExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.MangaSourcesFilterScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.MangaSourcesViewModel
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: the "Sources" page of manga Browse, with sources and extensions in a single list.
 * The search bar filters both; the badge counts the extensions waiting for an update.
 */
@Composable
fun Screen.mangaSourcesAndExtensionsTab(
    extensionsViewModel: MangaExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val sourcesViewModel = metroViewModel<MangaSourcesViewModel>()
    val sourcesState by sourcesViewModel.state.collectAsStateWithLifecycle()
    val updatesCount by extensionsViewModel.updatesCount.collectAsStateWithLifecycle()
    val repoCount: Int? by remember {
        context.appGraph.getMangaExtensionStoreCountAsFlow.subscribe().map { it.toInt() }
    }.collectAsState(initial = null)
    var privateExtensionToUninstall by remember { mutableStateOf<MangaExtension.Installed?>(null) }

    return TabContent(
        titleRes = MR.strings.label_sources,
        badgeNumber = updatesCount.takeIf { it > 0 },
        searchEnabled = true,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_global_search),
                icon = Icons.Outlined.TravelExplore,
                onClick = { navigator.push(GlobalMangaSearchScreen()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.FilterList,
                onClick = { navigator.push(MangaSourcesFilterScreen()) },
            ),
            AppBar.OverflowAction(
                title = stringResource(TLMR.strings.anitenshi_extension_languages),
                onClick = { navigator.push(MangaExtensionFilterScreen()) },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.extensionStores),
                onClick = { navigator.push(ExtensionStoresScreen(isManga = true)) },
            ),
            // AniTenshi: last item of the ⋮ menu
            settingsOverflowAction(
                title = stringResource(TLMR.strings.anitenshi_settings_browse),
                onClick = { navigator.push(SettingsScreen(SettingsScreen.Destination.Browse)) },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            val extensionsState by extensionsViewModel.state.collectAsStateWithLifecycle()

            BackHandler(enabled = extensionsState.searchQuery != null) {
                extensionsViewModel.search(null)
            }

            MangaSourcesAndExtensionsScreen(
                sourcesState = sourcesState,
                extensionsState = extensionsState,
                contentPadding = contentPadding,
                repoCount = repoCount,
                onClickRepos = { navigator.push(ExtensionStoresScreen(isManga = true)) },
                onAddRepo = { navigator.push(ExtensionStoresScreen(isManga = true, openCreate = true)) },
                onClickSource = { source, listing ->
                    navigator.push(BrowseMangaSourceScreen(source.id, listing.query))
                },
                onClickPin = sourcesViewModel::togglePin,
                onLongClickSource = sourcesViewModel::showSourceDialog,
                onLongClickExtension = { extension ->
                    when (extension) {
                        is MangaExtension.Available -> extensionsViewModel.installExtension(extension)

                        is MangaExtension.Installed -> {
                            if (context.isPackageInstalled(extension.pkgName)) {
                                extensionsViewModel.uninstallExtension(extension)
                            } else {
                                privateExtensionToUninstall = extension
                            }
                        }
                    }
                },
                onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsViewModel::installExtension,
                onUninstallExtension = { extensionsViewModel.uninstallExtension(it) },
                onUpdateExtension = extensionsViewModel::updateExtension,
                onTrustExtension = { extensionsViewModel.trustExtension(it) },
                onOpenExtension = { navigator.push(MangaExtensionDetailsScreen(it.pkgName)) },
                onClickUpdateAll = extensionsViewModel::updateAllExtensions,
                onRefresh = extensionsViewModel::findAvailableExtensions,
            )

            sourcesState.dialog?.let { dialog ->
                val source = dialog.source
                MangaSourceOptionsDialog(
                    source = source,
                    onClickPin = {
                        sourcesViewModel.togglePin(source)
                        sourcesViewModel.closeDialog()
                    },
                    onClickDisable = {
                        sourcesViewModel.toggleSource(source)
                        sourcesViewModel.closeDialog()
                    },
                    onClickToggleDataSaver = {
                        sourcesViewModel.toggleExcludeFromMangaDataSaver(source)
                        sourcesViewModel.closeDialog()
                    }.takeIf { sourcesState.dataSaverEnabled },
                    onDismiss = sourcesViewModel::closeDialog,
                )
            }

            privateExtensionToUninstall?.let { extension ->
                MangaExtensionUninstallConfirmation(
                    extensionName = extension.name,
                    onClickConfirm = {
                        extensionsViewModel.uninstallExtension(extension)
                    },
                    onDismissRequest = {
                        privateExtensionToUninstall = null
                    },
                )
            }

            val internalErrString = stringResource(MR.strings.internal_error)
            LaunchedEffect(Unit) {
                sourcesViewModel.events.collectLatest { event ->
                    when (event) {
                        MangaSourcesViewModel.Event.FailedFetchingSources -> {
                            launch { snackbarHostState.showSnackbar(internalErrString) }
                        }
                    }
                }
            }
        },
    )
}
