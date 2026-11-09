package eu.kanade.presentation.more.settings.screen.data

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSignIn

/**
 * AniTenshi: the "connect Google Drive" action for a screen (settings, first launch). Calling it starts the
 * sign-in; when Google has to show its own screen (account choice and permission) it opens on top of the app and
 * its answer finishes the sign-in.
 */
@Composable
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun rememberGoogleDriveSignIn(): () -> Unit {
    val context = LocalContext.current
    val googleScreen = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        GoogleDriveSignIn.finish(context, allowed = result.resultCode == Activity.RESULT_OK, data = result.data)
    }

    return remember(context, googleScreen) {
        {
            GoogleDriveSignIn.start(context) { screen ->
                try {
                    googleScreen.launch(IntentSenderRequest.Builder(screen.intentSender).build())
                } catch (_: Exception) {
                    // The person left this screen before Google answered: nothing to open its screen from.
                    // Tapping "connect" again starts over.
                }
            }
        }
    }
}
