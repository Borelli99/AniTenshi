package eu.kanade.presentation.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.BasePreferenceWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.sync.SyncManager
import mihon.app.di.appGraph
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

/**
 * AniTenshi: what the "Sync" row in More shows. Automatic syncs work quietly, so this is where the person sees
 * that they happen, and when they stopped.
 */
sealed interface SyncStatus {

    /** No sync service chosen. */
    data object Off : SyncStatus

    /**
     * A service is chosen but can't be used: not signed in, or its details are missing. [reconnect] when it
     * worked before on this device (Google Drive access was lost).
     */
    data class NotConnected(val reconnect: Boolean) : SyncStatus

    data object Running : SyncStatus

    /** Nothing running. [lastSync] is 0 when it never synced; [failed] when the latest attempt didn't go through. */
    data class Idle(val lastSync: Long, val failed: Boolean) : SyncStatus
}

@Composable
fun rememberSyncStatus(): SyncStatus {
    val context = LocalContext.current
    val syncPreferences = remember { context.appGraph.syncPreferences }
    val service by syncPreferences.syncService().collectPreferenceAsState()
    val googleDriveConnected by syncPreferences.googleDriveConnected().collectPreferenceAsState()
    val syncYomiKey by syncPreferences.clientAPIKey().collectPreferenceAsState()
    val lastSync by syncPreferences.lastSyncTimestamp().collectPreferenceAsState()
    val lastError by syncPreferences.lastSyncError().collectPreferenceAsState()
    val running by remember { SyncDataJob.isRunningFlow(context) }.collectAsState(initial = false)

    val googleDrive = service == SyncManager.SyncService.GOOGLE_DRIVE.value
    val ready = when (service) {
        SyncManager.SyncService.GOOGLE_DRIVE.value -> googleDriveConnected
        SyncManager.SyncService.SYNCYOMI.value -> syncYomiKey.isNotBlank()
        else -> false
    }
    return when {
        SyncManager.SyncService.fromInt(service) == SyncManager.SyncService.NONE -> SyncStatus.Off
        running -> SyncStatus.Running
        !ready -> SyncStatus.NotConnected(reconnect = googleDrive && lastSync > 0L)
        else -> SyncStatus.Idle(lastSync = lastSync, failed = lastError.isNotBlank())
    }
}

/**
 * The row itself. Tapping it opens the sync settings; while nothing is wrong it also carries a round button to
 * sync right away.
 */
@Composable
fun SyncStatusRow(
    status: SyncStatus,
    onClick: () -> Unit,
    onClickSyncNow: () -> Unit,
) {
    val problem = status is SyncStatus.NotConnected || (status is SyncStatus.Idle && status.failed)
    val iconTint = when {
        problem -> MaterialTheme.colorScheme.error
        status is SyncStatus.Off -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val widget: (@Composable () -> Unit)? = when (status) {
        SyncStatus.Running -> {
            { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
        }
        is SyncStatus.Idle -> {
            {
                OutlinedIconButton(
                    onClick = onClickSyncNow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = stringResource(TLMR.strings.pref_sync_now),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        else -> null
    }

    BasePreferenceWidget(
        title = stringResource(TLMR.strings.anitenshi_sync_status_title),
        subcomponent = {
            Text(
                text = status.summary(),
                modifier = Modifier
                    .padding(horizontal = PrefsHorizontalPadding)
                    .then(if (problem) Modifier else Modifier.secondaryItemAlpha()),
                color = if (problem) MaterialTheme.colorScheme.error else Color.Unspecified,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
            )
        },
        icon = {
            Icon(
                imageVector = status.icon(),
                tint = iconTint,
                contentDescription = null,
            )
        },
        onClick = onClick,
        widget = widget,
    )
}

@Composable
private fun SyncStatus.summary(): String = when (this) {
    SyncStatus.Off -> stringResource(TLMR.strings.anitenshi_sync_status_off)

    is SyncStatus.NotConnected -> stringResource(
        if (reconnect) {
            TLMR.strings.anitenshi_sync_status_reconnect
        } else {
            TLMR.strings.anitenshi_sync_status_connect
        },
    )

    SyncStatus.Running -> stringResource(TLMR.strings.anitenshi_sync_status_running)

    is SyncStatus.Idle -> {
        val updated = if (lastSync > 0L) {
            stringResource(TLMR.strings.anitenshi_sync_status_synced, relativeTimeSpanString(lastSync).midSentence())
        } else {
            stringResource(TLMR.strings.anitenshi_sync_status_never)
        }
        if (failed) stringResource(TLMR.strings.anitenshi_sync_status_failed, updated) else updated
    }
}

private fun SyncStatus.icon(): ImageVector = when (this) {
    SyncStatus.Off -> Icons.Outlined.Cloud
    is SyncStatus.NotConnected -> Icons.Outlined.SyncProblem
    SyncStatus.Running -> Icons.Outlined.CloudUpload
    is SyncStatus.Idle -> if (failed) Icons.Outlined.SyncProblem else Icons.Outlined.CloudDone
}

/** "Yesterday" reads as "yesterday" after "Synced"; dates such as "Nov 3" keep their capital. */
private fun String.midSentence(): String {
    return if (none { it.isDigit() }) replaceFirstChar { it.lowercase() } else this
}
