package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import kotlinx.coroutines.delay
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import kotlin.time.Duration.Companion.seconds

internal class PermissionStep : OnboardingStep {

    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val privacyPreferences = remember { context.appGraph.privacyPreferences }
        val lifecycleOwner = LocalLifecycleOwner.current

        val installGranted = rememberRequestPackageInstallsPermissionState()

        // AniTenshi: check again right away, after each answer and whenever the app comes back.
        // The screen's own lifecycle doesn't always report coming back from the system dialogs,
        // so the activity's lifecycle is used too.
        val refresh = {
            notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            batteryGranted = context.getSystemService<PowerManager>()!!
                .isIgnoringBatteryOptimizations(context.packageName)
        }
        LaunchedEffect(Unit) { refresh() }
        // Until both are granted, look again every second: on some phones the answer arrives a moment late
        LaunchedEffect(notificationGranted, batteryGranted) {
            while (!(notificationGranted && batteryGranted)) {
                delay(1.seconds)
                refresh()
            }
        }

        val activityLifecycle = (LocalActivity.current as? ComponentActivity)?.lifecycle
        DisposableEffect(lifecycleOwner.lifecycle, activityLifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    refresh()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            activityLifecycle?.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                activityLifecycle?.removeObserver(observer)
            }
        }

        val batteryRequester = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { refresh() },
        )

        Column {
            PermissionItem(
                title = stringResource(MR.strings.onboarding_permission_install_apps),
                subtitle = stringResource(MR.strings.onboarding_permission_install_apps_description),
                granted = installGranted,
                onButtonClick = {
                    context.launchRequestPackageInstallsPermission()
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { refresh() },
                )
                PermissionItem(
                    title = stringResource(MR.strings.onboarding_permission_notifications),
                    subtitle = stringResource(MR.strings.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onButtonClick = { permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            PermissionItem(
                title = stringResource(MR.strings.onboarding_permission_ignore_battery_opts),
                subtitle = stringResource(MR.strings.onboarding_permission_ignore_battery_opts_description),
                granted = batteryGranted,
                onButtonClick = {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    batteryRequester.launch(intent)
                },
            )
        }
    }

    @Composable
    private fun SectionHeader(
        text: String,
        modifier: Modifier = Modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier
                .padding(horizontal = 16.dp)
                .secondaryItemAlpha(),
        )
    }

    @Composable
    private fun PermissionItem(
        title: String,
        subtitle: String,
        granted: Boolean,
        modifier: Modifier = Modifier,
        onButtonClick: () -> Unit,
    ) {
        ListItem(
            modifier = modifier,
            trailingContent = {
                OutlinedButton(
                    enabled = !granted,
                    onClick = onButtonClick,
                ) {
                    if (granted) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(stringResource(MR.strings.onboarding_permission_action_grant))
                    }
                }
            },
            supportingContent = { Text(text = subtitle) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            content = { Text(text = title) },
        )
    }

    @Composable
    private fun PermissionSwitch(
        title: String,
        subtitle: String,
        granted: Boolean,
        modifier: Modifier = Modifier,
        onToggleChange: (Boolean) -> Unit,
    ) {
        ListItem(
            modifier = modifier,
            trailingContent = {
                Switch(
                    checked = granted,
                    onCheckedChange = onToggleChange,
                )
            },
            supportingContent = { Text(text = subtitle) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            content = { Text(text = title) },
        )
    }
}
