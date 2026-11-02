package eu.kanade.presentation.browse.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BrowseListFilter
import eu.kanade.presentation.browse.components.BrowseListFilterChips
import eu.kanade.presentation.browse.components.BrowseSectionTitle
import eu.kanade.presentation.browse.components.ExtensionStoresCard
import eu.kanade.presentation.browse.components.NoExtensionStoresBlock
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.manga.source.MangaSourcesViewModel
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceViewModel.Listing
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus

/**
 * AniTenshi: sources and extensions in one list.
 *
 * "All" shows pending updates, the installed sources, then the extensions to install;
 * "Installed" the installed extensions (to open their settings or remove them); "To install" the rest.
 */
@Composable
fun MangaSourcesAndExtensionsScreen(
    sourcesState: MangaSourcesViewModel.State,
    extensionsState: MangaExtensionsViewModel.State,
    contentPadding: PaddingValues,
    repoCount: Int?,
    onClickRepos: () -> Unit,
    onAddRepo: () -> Unit,
    onClickSource: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickSource: (Source) -> Unit,
    onLongClickExtension: (MangaExtension) -> Unit,
    onClickItemCancel: (MangaExtension) -> Unit,
    onOpenWebView: (MangaExtension.Available) -> Unit,
    onInstallExtension: (MangaExtension.Available) -> Unit,
    onUninstallExtension: (MangaExtension.Installed) -> Unit,
    onUpdateExtension: (MangaExtension.Installed) -> Unit,
    onTrustExtension: (MangaExtension.NotLoaded) -> Unit,
    onOpenExtension: (MangaExtension.Loaded) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    if (sourcesState.isLoading && extensionsState.isLoading) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }

    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(BrowseListFilter.All) }
    var notLoadedState by remember { mutableStateOf<MangaExtension.NotLoaded?>(null) }
    var installErrorState by remember { mutableStateOf<InstallStep.Error?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)

    val query = extensionsState.searchQuery
    val groups = extensionsState.items
    // Updates and extensions that did not load (e.g. waiting to be trusted) always come first
    val attentionGroups = remember(groups) {
        groups.filterKeys {
            it is MangaExtensionUiModel.Header.Resource && it.textRes != MR.strings.ext_installed
        }
    }
    val installedGroups = remember(groups) { groups.filterKeys { it is MangaExtensionUiModel.Header.Resource } }
    val availableGroups = remember(groups) { groups.filterKeys { it is MangaExtensionUiModel.Header.Text } }
    val sourceItems = remember(sourcesState.items, query) {
        if (query.isNullOrBlank()) {
            sourcesState.items
        } else {
            sourcesState.items
                .filterIsInstance<MangaSourceUiModel.Item>()
                .filter { it.source.name.contains(query, ignoreCase = true) }
                .distinctBy { it.source.id }
        }
    }
    val installedCount = installedGroups.values.sumOf { it.size }
    val availableCount = availableGroups.values.sumOf { it.size }
    val noRepos = repoCount == 0 && groups.isEmpty() && query.isNullOrEmpty() && !extensionsState.isLoading

    PullRefresh(
        refreshing = extensionsState.isRefreshing,
        onRefresh = onRefresh,
        enabled = !extensionsState.isLoading,
    ) {
        FastScrollLazyColumn(
            contentPadding = contentPadding + topSmallPaddingValues,
        ) {
            if (noRepos) {
                item(key = "no-repos", contentType = "no-repos") {
                    NoExtensionStoresBlock(onAddRepo = onAddRepo)
                }
            } else if (repoCount != null && query.isNullOrEmpty()) {
                item(key = "extension-stores-card", contentType = "stores") {
                    ExtensionStoresCard(
                        repoCount = repoCount,
                        onClick = if (repoCount == 0) onAddRepo else onClickRepos,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (!installGranted && extensionsState.installer?.requiresSystemPermission == true) {
                item(key = "extension-permissions-warning") {
                    WarningBanner(
                        textRes = MR.strings.ext_permission_install_apps_warning,
                        modifier = Modifier.clickable {
                            context.launchRequestPackageInstallsPermission()
                        },
                    )
                }
            }

            if (!noRepos) {
                item(key = "browse-filters", contentType = "filters") {
                    BrowseListFilterChips(
                        selected = filter,
                        installedCount = installedCount,
                        availableCount = availableCount,
                        onSelect = { filter = it },
                    )
                }
            }

            val showGroups = when (filter) {
                BrowseListFilter.All -> attentionGroups
                BrowseListFilter.Installed -> installedGroups
                BrowseListFilter.Available -> availableGroups
            }
            mangaExtensionGroups(
                groups = showGroups,
                onLongClickItem = onLongClickExtension,
                onOpenWebView = onOpenWebView,
                onClickItemCancel = onClickItemCancel,
                onInstallExtension = onInstallExtension,
                onUpdateExtension = onUpdateExtension,
                onOpenExtension = onOpenExtension,
                onClickUpdateAll = onClickUpdateAll,
                onShowNotLoaded = { notLoadedState = it },
                onShowInstallError = { installErrorState = it },
            )

            if (filter == BrowseListFilter.All) {
                if (sourceItems.isNotEmpty()) {
                    item(key = "section-installed-sources", contentType = "section") {
                        BrowseSectionTitle(
                            text = stringResource(TLMR.strings.anitenshi_installed_sources),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    mangaSourceItems(
                        sourceItems = sourceItems,
                        onClickItem = onClickSource,
                        onClickPin = onClickPin,
                        onLongClickItem = onLongClickSource,
                    )
                }
                if (availableGroups.isNotEmpty()) {
                    item(key = "section-to-install", contentType = "section") {
                        BrowseSectionTitle(
                            text = stringResource(TLMR.strings.anitenshi_to_install),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    mangaExtensionGroups(
                        groups = availableGroups,
                        onLongClickItem = onLongClickExtension,
                        onOpenWebView = onOpenWebView,
                        onClickItemCancel = onClickItemCancel,
                        onInstallExtension = onInstallExtension,
                        onUpdateExtension = onUpdateExtension,
                        onOpenExtension = onOpenExtension,
                        onClickUpdateAll = onClickUpdateAll,
                        onShowNotLoaded = { notLoadedState = it },
                        onShowInstallError = { installErrorState = it },
                    )
                }
            }

            val nothingShown = when (filter) {
                BrowseListFilter.All -> attentionGroups.isEmpty() && sourceItems.isEmpty() && availableGroups.isEmpty()
                BrowseListFilter.Installed -> installedGroups.isEmpty()
                BrowseListFilter.Available -> availableGroups.isEmpty()
            }
            if (nothingShown && !noRepos && !extensionsState.isLoading) {
                item(key = "nothing-found", contentType = "nothing") {
                    Text(
                        text = stringResource(
                            if (query.isNullOrEmpty()) MR.strings.empty_screen else MR.strings.no_results_found,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(MaterialTheme.padding.medium),
                    )
                }
            }
        }
    }

    MangaExtensionDialogs(
        notLoaded = notLoadedState,
        installError = installErrorState,
        onDismissNotLoaded = { notLoadedState = null },
        onDismissInstallError = { installErrorState = null },
        onTrustExtension = onTrustExtension,
        onUninstallExtension = onUninstallExtension,
    )
}
