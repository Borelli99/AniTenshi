package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.app.di.appGraph
import mihon.core.metro.metroGraph
import tachiyomi.core.common.util.system.logcat
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class SyncDataJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject private lateinit var syncManager: SyncManager

    @Inject private lateinit var notifier: SyncNotifier

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        graph.inject(this)

        // AniTenshi: automatic syncs run quietly: no "sync complete" every time the app opens
        val silent = inputData.getBoolean(KEY_SILENT, false)

        if (tags.contains(TAG_AUTO)) {
            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(TAG_MANUAL)) {
                return Result.retry()
            }
        }

        // AniTenshi: nothing to sync with yet (not signed in): automatic syncs just skip
        if ((silent || tags.contains(TAG_AUTO)) && !graph.syncPreferences.isSyncReady()) {
            return Result.success()
        }

        if (!silent) setForegroundSafely()

        return try {
            syncManager.syncData(silent = silent)
            // AniTenshi: the result is remembered for the status row in "More"
            graph.syncPreferences.lastSyncError().delete()
            Result.success()
        } catch (e: CancellationException) {
            // Stopped (by hand, or by the system): neither a success nor a failure to report
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            graph.syncPreferences.lastSyncError().set(e.message.orEmpty().ifBlank { e.javaClass.simpleName })
            val notDone = e as? SyncNotDoneException
            // AniTenshi: quiet syncs don't notify about the network or a service that couldn't be reached (the
            // status row shows it); they do when the person has something to do, like connecting again
            val keepQuiet = silent && (e.isNetworkError() || notDone != null)
            if (!keepQuiet && notDone?.alreadyNotified != true) notifier.showSyncError(e.message)
            // AniTenshi: a failed sync before a library update must not stop the update
            if (silent) Result.success() else Result.failure()
        } finally {
            context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_RESTORE_PROGRESS,
            notifier.showSyncProgress().build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private fun Throwable.isNetworkError(): Boolean = generateSequence(this) { it.cause }
        .any { it is UnknownHostException || it is SocketTimeoutException || it is ConnectException }

    companion object {
        private const val TAG_JOB = "SyncDataJob"
        private const val TAG_AUTO = "$TAG_JOB:auto"
        const val TAG_MANUAL = "$TAG_JOB:manual"
        private const val TAG_QUIET = "$TAG_JOB:quiet"
        private const val KEY_SILENT = "silent"

        // AniTenshi: coming back to the app syncs again only after this long
        private const val RESUME_SYNC_INTERVAL_MS = 5 * 60 * 1000L

        // AniTenshi: finishing chapters or episodes syncs again only after this long, to go easy on data and battery
        private const val PROGRESS_SYNC_INTERVAL_MS = 10 * 60 * 1000L

        fun isRunning(context: Context): Boolean {
            return context.workManager.isRunning(TAG_JOB)
        }

        /** AniTenshi: whether a sync is running right now, for the status row in "More". */
        fun isRunningFlow(context: Context): Flow<Boolean> {
            val runningSyncs = WorkQuery.Builder.fromTags(listOf(TAG_JOB))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            return context.workManager.getWorkInfosFlow(runningSyncs)
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
        }

        @Suppress("MagicNumber")
        fun setupTask(context: Context, prefInterval: Int? = null) {
            val syncPreferences = context.appGraph.syncPreferences
            val interval = prefInterval ?: syncPreferences.syncInterval().get()

            if (interval > 0) {
                val request = PeriodicWorkRequestBuilder<SyncDataJob>(
                    interval.toLong(),
                    TimeUnit.MINUTES,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG_JOB)
                    .addTag(TAG_AUTO)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(TAG_AUTO, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                context.workManager.cancelUniqueWork(TAG_AUTO)
            }
        }

        fun startNow(context: Context) {
            val wm = context.workManager
            if (wm.isRunning(TAG_JOB)) {
                // Already running either as a scheduled or manual job
                return
            }
            val request = OneTimeWorkRequestBuilder<SyncDataJob>()
                .addTag(TAG_JOB)
                .addTag(TAG_MANUAL)
                .build()
            context.workManager.enqueueUniqueWork(TAG_MANUAL, ExistingWorkPolicy.KEEP, request)
        }

        /**
         * AniTenshi: a quiet sync for the app's own triggers (opening, closing or coming back to the app).
         * Does nothing until the account is connected; with [throttle] it skips when the last sync was
         * moments ago. It waits for a connection instead of failing offline.
         */
        fun startAutomatic(context: Context, throttle: Boolean = false) {
            startQuiet(context, minIntervalMs = if (throttle) RESUME_SYNC_INTERVAL_MS else 0L)
        }

        /**
         * AniTenshi: a quiet sync after a chapter was read or an episode watched, so the other devices get the
         * progress even during a long session. Not more often than every 10 minutes; leaving the app syncs anyway.
         */
        fun startAfterProgress(context: Context) {
            val syncPreferences = context.appGraph.syncPreferences
            if (!syncPreferences.isSyncReady()) return
            if (!syncPreferences.getSyncTriggerOptions().syncOnProgress) return
            startQuiet(context, minIntervalMs = PROGRESS_SYNC_INTERVAL_MS)
        }

        private fun startQuiet(context: Context, minIntervalMs: Long) {
            val syncPreferences = context.appGraph.syncPreferences
            if (!syncPreferences.isSyncReady()) return
            if (minIntervalMs > 0 &&
                System.currentTimeMillis() - syncPreferences.lastSyncTimestamp().get() < minIntervalMs
            ) {
                return
            }
            val wm = context.workManager
            if (wm.isRunning(TAG_JOB)) return

            val request = OneTimeWorkRequestBuilder<SyncDataJob>()
                .addTag(TAG_JOB)
                .addTag(TAG_QUIET)
                .setInputData(workDataOf(KEY_SILENT to true))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniqueWork(TAG_QUIET, ExistingWorkPolicy.KEEP, request)
        }

        /**
         * AniTenshi: the quiet sync to run before a library update started by hand, or null when it
         * shouldn't: sync off or not connected, the trigger turned off, or a sync already running.
         */
        fun requestBeforeLibraryUpdate(context: Context): OneTimeWorkRequest? {
            val syncPreferences = context.appGraph.syncPreferences
            if (!syncPreferences.isSyncReady()) return null
            if (!syncPreferences.getSyncTriggerOptions().syncOnLibraryUpdate) return null
            if (context.workManager.isRunning(TAG_JOB)) return null

            return OneTimeWorkRequestBuilder<SyncDataJob>()
                .addTag(TAG_JOB)
                .addTag(TAG_MANUAL)
                .setInputData(workDataOf(KEY_SILENT to true))
                .build()
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG_JOB, TAG_AUTO, TAG_MANUAL, TAG_QUIET))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(TAG_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}
