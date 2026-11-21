package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.CardEntry
import eu.kanade.presentation.components.CardGroup
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isNightlyBuildType
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.icons.simpleicons.Github
import mihon.icons.simpleicons.SimpleIcons
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import kotlin.time.Instant

object AboutScreen : Screen() {

    // On wide screens the page stays in a column in the middle instead of stretching edge to edge
    private val MaxContentWidth = 640.dp

    /**
     * AniTenshi: the "About" page. A card at the top says which version this is, with the button to look for
     * a newer one; under it, named groups as on the first page of the settings. Copying the device
     * information, which used to happen by tapping the version, is an entry of its own.
     */
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }
        val crashLogUtil = remember { context.appGraph.crashLogUtil }
        val uiPreferences = remember { context.appGraph.uiPreferences }
        val is24HourSystem = rememberIs24HourSystem()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val block = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()

            ScrollbarLazyColumn(
                contentPadding = contentPadding + PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    VersionCard(
                        version = getVersionLabel(),
                        buildTime = remember(is24HourSystem) {
                            getFormattedBuildTime(uiPreferences.dateFormat.get())
                        },
                        isCheckingUpdates = isCheckingUpdates,
                        onCheckUpdates = if (updaterEnabled) {
                            {
                                if (!isCheckingUpdates) {
                                    scope.launch {
                                        isCheckingUpdates = true
                                        checkVersion(
                                            context = context,
                                            onAvailableUpdate = { result ->
                                                val updateScreen = NewUpdateScreen(
                                                    versionName = result.release.version,
                                                    changelogInfo = result.release.info,
                                                    releaseLink = result.release.releaseLink,
                                                    downloadLink = result.release.downloadLink,
                                                )
                                                navigator.push(updateScreen)
                                            },
                                            onFinish = {
                                                isCheckingUpdates = false
                                            },
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        },
                        onWhatsNew = if (!BuildConfig.DEBUG) {
                            { uriHandler.openUri(RELEASE_URL) }
                        } else {
                            null
                        },
                        modifier = block,
                    )
                }

                item {
                    CardGroup(
                        title = stringResource(TLMR.strings.anitenshi_about_group_help),
                        modifier = block,
                    ) {
                        CardEntry(
                            title = stringResource(TLMR.strings.anitenshi_about_copy_info),
                            subtitle = stringResource(TLMR.strings.anitenshi_about_copy_info_summary),
                            icon = Icons.Outlined.ContentCopy,
                            onClick = {
                                val deviceInfo = crashLogUtil.getDebugInfo()
                                context.copyToClipboard("Debug information", deviceInfo)
                            },
                        )
                    }
                }

                item {
                    CardGroup(
                        title = stringResource(TLMR.strings.anitenshi_about_group_project),
                        modifier = block,
                    ) {
                        CardEntry(
                            title = stringResource(TLMR.strings.anitenshi_about_github),
                            subtitle = Constants.URL_GITHUB.removePrefix("https://"),
                            icon = SimpleIcons.Github,
                            onClick = { uriHandler.openUri(Constants.URL_GITHUB) },
                        )
                        CardEntry(
                            title = stringResource(TLMR.strings.anitenshi_about_releases),
                            subtitle = stringResource(TLMR.strings.anitenshi_about_releases_summary),
                            icon = Icons.Outlined.NewReleases,
                            onClick = { uriHandler.openUri(Constants.URL_RELEASES) },
                        )
                    }
                }

                item {
                    CardGroup(
                        title = stringResource(TLMR.strings.anitenshi_about_group_legal),
                        modifier = block,
                    ) {
                        CardEntry(
                            title = stringResource(MR.strings.licenses),
                            icon = Icons.Outlined.Description,
                            onClick = { navigator.push(OpenSourceLicensesScreen()) },
                        )
                        CardEntry(
                            title = stringResource(MR.strings.privacy_policy),
                            icon = Icons.Outlined.Lock,
                            onClick = { uriHandler.openUri(Constants.URL_PRIVACY) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun VersionCard(
        version: String,
        buildTime: String,
        isCheckingUpdates: Boolean,
        onCheckUpdates: (() -> Unit)?,
        onWhatsNew: (() -> Unit)?,
        modifier: Modifier = Modifier,
    ) {
        val colors = MaterialTheme.colorScheme
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ani),
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = stringResource(MR.strings.app_name),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(TLMR.strings.anitenshi_about_version, version),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(TLMR.strings.anitenshi_about_built, buildTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (onCheckUpdates != null) {
                    Button(
                        onClick = onCheckUpdates,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondary,
                            contentColor = colors.onSecondary,
                        ),
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.onSecondary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = stringResource(TLMR.strings.anitenshi_about_check_updates),
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (onWhatsNew != null) {
                    TextButton(
                        onClick = onWhatsNew,
                        modifier = Modifier.padding(top = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary),
                    ) {
                        Text(
                            text = stringResource(TLMR.strings.anitenshi_about_whats_new),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = context.appGraph.updateChecker
        withUIContext {
            try {
                when (val result = withIOContext { updateChecker.checkForUpdate(forceCheck = true) }) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }

                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }

                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    /**
     * AniTenshi: the version without the app's name in front, for the card where the name is written above it:
     * "1.0.20 (r10037)" for the builds people install.
     */
    fun getVersionLabel(): String {
        return when {
            BuildConfig.DEBUG -> "Debug ${BuildConfig.COMMIT_SHA}"
            isNightlyBuildType -> "Nightly r${BuildConfig.COMMIT_COUNT} (${BuildConfig.COMMIT_SHA})"
            else -> "${BuildConfig.VERSION_NAME} (r${BuildConfig.COMMIT_COUNT})"
        }
    }

    fun getVersionName(withBuildDate: Boolean, dateFormat: String? = null): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime(dateFormat)})"
                    } else {
                        it
                    }
                }
            }

            isNightlyBuildType -> {
                "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                    if (withBuildDate) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime(dateFormat)})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }

            else -> {
                // Version name plus the GitHub release tag (r<commit count>), e.g. "AniTenshi 1.0.1 (r9969)"
                "AniTenshi ${BuildConfig.VERSION_NAME} (r${BuildConfig.COMMIT_COUNT})".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime(dateFormat)})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(dateFormat: String? = null): String {
        return try {
            val context = Injekt.get<Context>()
            val uiPreferences = context.appGraph.uiPreferences
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(
                    UiPreferences.dateTimeFormat(
                        dateFormat ?: uiPreferences.dateFormat.get(),
                        uiPreferences.timeFormat.get(),
                        Locale.getDefault(),
                        DateFormat.is24HourFormat(context),
                    ),
                )
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
