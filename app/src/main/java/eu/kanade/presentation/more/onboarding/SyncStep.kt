package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveLoopbackSignIn
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

/**
 * AniTenshi: optional step before the repos: keep devices in sync through Google Drive, or start from a
 * backup. The restore runs while the first launch goes on, so its repos already show in the next step.
 */
internal class SyncStep(
    private val onRestoreBackup: (uri: String) -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val syncPreferences = remember { context.appGraph.syncPreferences }
        val refreshToken by syncPreferences.googleDriveRefreshToken().collectPreferenceAsState()
        val restoreState by remember { BackupRestoreJob.fileRestoreState(context) }.collectAsState(initial = null)

        val chooseBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                logcat(LogPriority.ERROR, e)
            }
            onRestoreBackup(uri.toString())
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OnboardingHeader(
                title = stringResource(TLMR.strings.anitenshi_onboarding_sync_title),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_sync_body),
            ) {
                OnboardingIcon(Icons.Outlined.Sync)
            }

            DriveCard(
                connected = refreshToken.isNotBlank(),
                onConnect = {
                    if (!GoogleDriveService.isConfigured(context)) {
                        context.toast(TLMR.strings.anitenshi_google_drive_not_configured, duration = 5000)
                    } else {
                        GoogleDriveLoopbackSignIn.start(context)
                    }
                },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(TLMR.strings.anitenshi_onboarding_or),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BackupCard(
                state = restoreState,
                onChoose = {
                    try {
                        chooseBackup.launch(arrayOf("*/*"))
                    } catch (e: ActivityNotFoundException) {
                        context.toast(MR.strings.file_picker_error)
                    }
                },
            )

            Text(
                text = stringResource(TLMR.strings.anitenshi_onboarding_sync_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DriveCard(
    connected: Boolean,
    onConnect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Bullet(Icons.Outlined.Sync, stringResource(TLMR.strings.anitenshi_onboarding_sync_auto))
            Bullet(Icons.Outlined.Devices, stringResource(TLMR.strings.anitenshi_onboarding_sync_new_device))
            if (connected) {
                StatusRow(
                    title = stringResource(TLMR.strings.anitenshi_google_drive_connected),
                    subtitle = stringResource(TLMR.strings.anitenshi_onboarding_sync_connected_body),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onConnect,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(TLMR.strings.anitenshi_onboarding_sync_connect),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupCard(
    state: WorkInfo.State?,
    onChoose: () -> Unit,
) {
    val running = state == WorkInfo.State.ENQUEUED ||
        state == WorkInfo.State.RUNNING ||
        state == WorkInfo.State.BLOCKED
    val done = state == WorkInfo.State.SUCCEEDED
    val failed = state == WorkInfo.State.FAILED

    Surface(
        onClick = onChoose,
        enabled = !running && !done,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatusRow(
            title = stringResource(
                when {
                    running -> TLMR.strings.anitenshi_onboarding_backup_running
                    done -> TLMR.strings.anitenshi_onboarding_backup_done
                    failed -> TLMR.strings.anitenshi_onboarding_backup_failed
                    else -> TLMR.strings.anitenshi_onboarding_backup_title
                },
            ),
            subtitle = stringResource(
                when {
                    running -> TLMR.strings.anitenshi_onboarding_backup_running_body
                    done -> TLMR.strings.anitenshi_onboarding_backup_done_body
                    failed -> TLMR.strings.anitenshi_onboarding_backup_failed_body
                    else -> TLMR.strings.anitenshi_onboarding_backup_body
                },
            ),
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            when {
                running -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
                done -> Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                else -> Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        icon()
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
