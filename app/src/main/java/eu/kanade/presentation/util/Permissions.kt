package eu.kanade.presentation.util

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberRequestPackageInstallsPermissionState(initialValue: Boolean = false): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // AniTenshi: the screen's own lifecycle doesn't always report coming back from the system settings, so the
    // activity's lifecycle is followed too
    val activityLifecycle = (LocalActivity.current as? ComponentActivity)?.lifecycle

    var installGranted by remember { mutableStateOf(initialValue) }

    DisposableEffect(lifecycleOwner.lifecycle, activityLifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                installGranted = context.packageManager.canRequestPackageInstalls()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        activityLifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activityLifecycle?.removeObserver(observer)
        }
    }

    // AniTenshi: and until it is granted, look again every second. On some phones the answer arrives a moment
    // after the app is back, or the settings open in a window on top and the app never notices it is back; the
    // permission was given but the screen kept asking for it.
    LaunchedEffect(installGranted) {
        while (!installGranted) {
            delay(1.seconds)
            installGranted = context.packageManager.canRequestPackageInstalls()
        }
    }

    return installGranted
}
