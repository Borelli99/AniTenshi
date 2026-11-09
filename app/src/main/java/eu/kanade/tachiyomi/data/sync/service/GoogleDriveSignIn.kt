package eu.kanade.tachiyomi.data.sync.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.tail.TLMR

/**
 * AniTenshi: connecting Google Drive with the sign-in built into Android (Google Play services).
 *
 * [start] asks for access. The first time Google shows its own screen on top of the app, where the person picks
 * the account and allows it; its answer comes back through [finish]. When access was already given on this device
 * there is nothing to show and Drive is connected right away.
 *
 * It replaces the sign-in through the browser, which needed the app's client secret inside the APK.
 */
object GoogleDriveSignIn {

    // Not tied to a screen: the sign-in finishes even if the person leaves the settings meanwhile
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the sign-in. When Google has to show its screen it is handed to [showGoogleScreen], which opens it
     * and passes what it returns to [finish].
     */
    @Suppress("TooGenericExceptionCaught")
    fun start(context: Context, showGoogleScreen: (PendingIntent) -> Unit) {
        val appContext = context.applicationContext
        if (!GoogleDriveService.isAvailable(appContext)) {
            appContext.toast(TLMR.strings.anitenshi_google_drive_needs_play_services, Toast.LENGTH_LONG)
            return
        }

        scope.launch {
            try {
                val result = GoogleDriveService(appContext).authorize()
                val googleScreen = result.pendingIntent
                if (result.hasResolution() && googleScreen != null) {
                    withUIContext { showGoogleScreen(googleScreen) }
                } else {
                    connect(appContext, result)
                }
            } catch (e: Exception) {
                fail(appContext, e)
            }
        }
    }

    /**
     * The answer of the Google screen opened by [start]: [allowed] is false when the person closed it, [data] is
     * what the screen returned.
     */
    @Suppress("TooGenericExceptionCaught")
    fun finish(context: Context, allowed: Boolean, data: Intent?) {
        val appContext = context.applicationContext
        if (!allowed || data == null) {
            appContext.toast(TLMR.strings.anitenshi_google_drive_cancelled, Toast.LENGTH_LONG)
            return
        }

        scope.launch {
            try {
                val result = Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data)
                connect(appContext, result)
            } catch (e: Exception) {
                fail(appContext, e)
            }
        }
    }

    private suspend fun connect(context: Context, result: AuthorizationResult) {
        val token = result.accessToken ?: error("Google Play services gave no access token")
        GoogleDriveService(context).connect(token)

        // Signing in also picks Google Drive as the sync service (the first launch has no menu for it), then the
        // first sync starts right away
        context.appGraph.syncPreferences.syncService().set(SyncManager.SyncService.GOOGLE_DRIVE.value)
        SyncDataJob.startNow(context)
        withUIContext { context.toast(TLMR.strings.google_drive_login_success, Toast.LENGTH_LONG) }
    }

    private suspend fun fail(context: Context, e: Exception) {
        logcat(LogPriority.ERROR, e) { "AniTenshi: Google Drive sign-in" }
        val message = GoogleDriveService.errorMessage(context, e)
        withUIContext { context.toast(message, Toast.LENGTH_LONG) }
    }
}
