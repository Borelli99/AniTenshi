package eu.kanade.tachiyomi.data.sync.service

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.ConnectException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleDriveSyncService(context: Context, json: Json, syncPreferences: SyncPreferences) : SyncService(
    context,
    json,
    syncPreferences,
) {
    constructor(context: Context) : this(
        context,
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        },
        Injekt.get<SyncPreferences>(),
    )

    enum class DeleteSyncDataStatus {
        NOT_INITIALIZED,
        NO_FILES,
        SUCCESS,
        ERROR,
    }

    private val appName = context.stringResource(MR.strings.app_name)

    private val remoteFileName = "${appName}_sync.proto.gz"

    private val googleDriveService = GoogleDriveService(context)

    private val protoBuf: ProtoBuf = Injekt.get()

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    override suspend fun doSync(syncData: SyncData): Backup? {
        beforeSync()

        try {
            val remoteSData = pullSyncData()

            if (remoteSData != null) {
                // Get local unique device ID
                val localDeviceId = syncPreferences.uniqueDeviceID()
                val lastSyncDeviceId = remoteSData.deviceId

                // Log the device IDs
                logcat(LogPriority.DEBUG, "SyncService") {
                    "Local device ID: $localDeviceId, Last sync device ID: $lastSyncDeviceId"
                }

                // check if the last sync was done by the same device if so
                // overwrite the remote data with the local data
                return if (lastSyncDeviceId == localDeviceId) {
                    pushSyncData(syncData)
                    syncData.backup
                } else {
                    // Merge the local and remote sync data
                    val mergedSyncData = mergeSyncData(syncData, remoteSData)
                    pushSyncData(mergedSyncData)
                    mergedSyncData.backup
                }
            }

            pushSyncData(syncData)
            return syncData.backup
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, "SyncService") { "Error syncing: ${e.message}" }
            return null
        }
    }

    private suspend fun beforeSync() {
        googleDriveService.refreshToken()
    }

    @Suppress("TooGenericExceptionThrown", "TooGenericExceptionCaught")
    private fun pullSyncData(): SyncData? {
        val drive = googleDriveService.driveService
            ?: throw Exception(context.stringResource(TLMR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        if (fileList.isEmpty()) {
            logcat(LogPriority.INFO) { "No files found in app data" }
            return null
        }

        val gdriveFileId = fileList[0].id
        logcat(LogPriority.DEBUG) { "Google Drive File ID: $gdriveFileId" }

        try {
            drive.files().get(gdriveFileId).executeMediaAsInputStream().use { inputStream ->
                GZIPInputStream(inputStream).use { gzipInputStream ->
                    val byteArray = gzipInputStream.readBytes()
                    val backup = protoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
                    val deviceId = fileList[0].appProperties["deviceId"] ?: ""
                    return SyncData(deviceId = deviceId, backup = backup)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Error downloading file: ${e.message}" }
            // AniTenshi: translated, it ends up in the sync error notification
            throw Exception(context.stringResource(TLMR.strings.anitenshi_sync_download_failed, e.message ?: ""), e)
        }
    }

    @Suppress("TooGenericExceptionThrown")
    private suspend fun pushSyncData(syncData: SyncData) {
        val drive = googleDriveService.driveService
            ?: throw Exception(context.stringResource(TLMR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        val backup = syncData.backup ?: return

        val byteArray = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        if (byteArray.isEmpty()) {
            throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
        }

        PipedOutputStream().use { pos ->
            PipedInputStream(pos).use { pis ->
                withIOContext {
                    launch {
                        GZIPOutputStream(pos).use { gzipOutputStream ->
                            gzipOutputStream.write(byteArray)
                        }
                    }

                    val mediaContent = InputStreamContent("application/octet-stream", pis)

                    if (fileList.isNotEmpty()) {
                        val fileId = fileList[0].id
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = "application/octet-stream"
                            appProperties = mapOf("deviceId" to syncData.deviceId)
                        }
                        drive.files().update(fileId, fileMetadata, mediaContent).execute()
                        logcat(
                            LogPriority.DEBUG,
                        ) { "Updated existing sync data file in Google Drive with file ID: $fileId" }
                    } else {
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = "application/octet-stream"
                            parents = listOf("appDataFolder")
                            appProperties = mapOf("deviceId" to syncData.deviceId)
                        }
                        val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute()
                        logcat(
                            LogPriority.DEBUG,
                        ) { "Created new sync data file in Google Drive with file ID: ${uploadedFile.id}" }
                    }
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getAppDataFileList(drive: Drive): MutableList<File> {
        try {
            // Search for the existing file by name in the appData folder
            val query = "mimeType='application/x-gzip' and name = '$remoteFileName'"
            val fileList = drive.files()
                .list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name, createdTime, appProperties)")
                .execute()
                .files
            logcat { "AppData folder file list: $fileList" }

            return fileList
        } catch (e: HttpResponseException) {
            if (e.statusCode == HTTP_UNAUTHORIZED) {
                // AniTenshi: Drive refused the token (the app's access was removed in the Google account). That is
                // not "no sync file yet": stop here instead of uploading over it, and drop the token so the next
                // sync asks Google again.
                googleDriveService.dropRefusedToken()
                throw e
            }
            logcat(LogPriority.ERROR, throwable = e) { "Error no sync data found in appData folder" }
            return mutableListOf()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Error no sync data found in appData folder" }
            return mutableListOf()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun deleteSyncDataFromGoogleDrive(): DeleteSyncDataStatus {
        // AniTenshi: ask for the access token first: the service has none until then
        val drive = try {
            googleDriveService.refreshToken()
            googleDriveService.driveService
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Google Drive service not initialized" }
            null
        }

        if (drive == null) {
            return DeleteSyncDataStatus.NOT_INITIALIZED
        }

        return withIOContext {
            try {
                val appDataFileList = getAppDataFileList(drive)

                if (appDataFileList.isEmpty()) {
                    this@GoogleDriveSyncService
                        .logcat(LogPriority.DEBUG) { "No sync data file found in appData folder of Google Drive" }
                    DeleteSyncDataStatus.NO_FILES
                } else {
                    for (file in appDataFileList) {
                        drive.files().delete(file.id).execute()
                        this@GoogleDriveSyncService.logcat(
                            LogPriority.DEBUG,
                        ) { "Deleted sync data file in appData folder of Google Drive with file ID: ${file.id}" }
                    }
                    DeleteSyncDataStatus.SUCCESS
                }
            } catch (e: Exception) {
                this@GoogleDriveSyncService.logcat(LogPriority.ERROR, throwable = e) {
                    "Error occurred while interacting with Google Drive"
                }
                DeleteSyncDataStatus.ERROR
            }
        }
    }
}

/**
 * AniTenshi: access to the person's Google Drive through Google Play services, the sign-in built into Android.
 *
 * The app holds no key and no long-lived token. Before each sync it asks Play services for a short-lived access
 * token, which comes without any screen for as long as the person hasn't removed the app's access. Google recognises
 * the app by its package name and signing certificate (the "Android" OAuth client of the Google Cloud project).
 *
 * It replaces the sign-in through the browser with a "Desktop app" client, which needed the client secret inside
 * the APK.
 */
class GoogleDriveService(private val context: Context) {
    var driveService: Drive? = null
        private set

    private var accessToken: String? = null

    private val syncPreferences = Injekt.get<SyncPreferences>()

    /**
     * Asks Play services for access to Drive. The answer either carries an access token (access was given before,
     * nothing to show) or a screen to open ([AuthorizationResult.getPendingIntent]) where the person picks the
     * account and allows it.
     */
    suspend fun authorize(): AuthorizationResult {
        val request = AuthorizationRequest.builder().setRequestedScopes(SCOPES)
        val account = syncPreferences.googleDriveAccount().get()
        if (account.isNotBlank()) {
            // Same account as when connecting, also on phones with several Google accounts
            request.setAccount(Account(account, GOOGLE_ACCOUNT_TYPE))
        }
        return Identity.getAuthorizationClient(context).authorize(request.build()).await()
            ?: error("Google Play services gave no authorization result")
    }

    /**
     * Finishes a sign-in with the access [token] Play services gave: checks it against Drive, notes which account
     * it is (shown in the settings, and asked for again at the next syncs) and marks Drive as connected.
     * Throws when Drive refuses the token.
     */
    suspend fun connect(token: String) = withIOContext {
        setupGoogleDriveService(token)
        val email = driveService?.about()?.get()?.setFields("user(emailAddress)")?.execute()?.user?.emailAddress
        syncPreferences.googleDriveAccount().set(email.orEmpty())
        syncPreferences.googleDriveConnected().set(true)
    }

    /**
     * Disconnects this device from Google Drive: asks Google to drop the app's access (best effort) and forgets the
     * connection. The data already on Drive stays there.
     */
    suspend fun signOut() {
        val account = syncPreferences.googleDriveAccount().get()
        if (account.isNotBlank()) {
            runCatching {
                val request = RevokeAccessRequest.builder()
                    .setAccount(Account(account, GOOGLE_ACCOUNT_TYPE))
                    .setScopes(SCOPES)
                    .build()
                Identity.getAuthorizationClient(context).revokeAccess(request).await()
            }.onFailure { e -> logcat(LogPriority.WARN, throwable = e) { "AniTenshi: Google access revoke" } }
        }
        forgetConnection()
    }

    /**
     * Gets a fresh access token before talking to Drive, without showing anything. Throws with a message for the
     * person when it can't; when the app's access is gone, Drive is also marked as not connected, so automatic
     * syncs stop until the person connects again.
     */
    @Suppress("TooGenericExceptionThrown", "TooGenericExceptionCaught")
    internal suspend fun refreshToken() {
        if (!syncPreferences.googleDriveConnected().get()) {
            throw Exception(context.stringResource(TLMR.strings.google_drive_not_signed_in))
        }
        if (!isAvailable(context)) {
            forgetConnection()
            throw Exception(context.stringResource(TLMR.strings.anitenshi_google_drive_needs_play_services))
        }

        val result = try {
            authorize()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "AniTenshi: Google Drive authorization" }
            throw when ((e as? ApiException)?.statusCode) {
                // Seen as a network problem by the sync job, which doesn't bother the person with those
                CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT ->
                    ConnectException(errorMessage(context, e)).apply { initCause(e) }
                else -> Exception(errorMessage(context, e), e)
            }
        }

        val token = result.accessToken
        if (result.hasResolution() || token == null) {
            // Play services would have to show its screen: the access was removed, or never given on this device
            forgetConnection()
            throw Exception(context.stringResource(TLMR.strings.anitenshi_google_drive_access_expired))
        }
        setupGoogleDriveService(token)
    }

    /**
     * Drive refused the current token: it is dropped from Play services' cache (it keeps tokens for up to an hour),
     * so the next sync asks Google again and finds out whether the access is really gone.
     */
    internal fun dropRefusedToken() {
        val token = accessToken ?: return
        accessToken = null
        driveService = null
        Identity.getAuthorizationClient(context)
            .clearToken(ClearTokenRequest.builder().setToken(token).build())
            .addOnFailureListener { e -> logcat(LogPriority.WARN, throwable = e) { "AniTenshi: Google token cache" } }
    }

    private fun forgetConnection() {
        syncPreferences.googleDriveConnected().set(false)
        syncPreferences.googleDriveAccount().delete()
        accessToken = null
        driveService = null
    }

    /** Builds the Drive client that sends [token] with every request. */
    private fun setupGoogleDriveService(token: String) {
        accessToken = token
        val withToken = HttpRequestInitializer { request -> request.headers.setAuthorization("Bearer $token") }
        driveService = Drive.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), withToken)
            .setApplicationName(context.stringResource(MR.strings.app_name))
            .build()
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"

        private val SCOPES = listOf(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))

        /** Google Play services are on this device and usable; without them there is no Google Drive sign-in. */
        fun isAvailable(context: Context): Boolean =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

        /** What went wrong with the sign-in, in words for the person. */
        fun errorMessage(context: Context, e: Throwable): String {
            return when ((e as? ApiException)?.statusCode) {
                CommonStatusCodes.CANCELED ->
                    context.stringResource(TLMR.strings.anitenshi_google_drive_cancelled)
                // The app (package name + signing certificate) isn't registered in the Google Cloud project
                CommonStatusCodes.DEVELOPER_ERROR ->
                    context.stringResource(TLMR.strings.anitenshi_google_drive_not_configured)
                CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT ->
                    context.stringResource(TLMR.strings.anitenshi_google_drive_offline)
                else ->
                    context.stringResource(TLMR.strings.google_drive_login_failed, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}

/** Waits for a Play services [Task] without blocking; null for tasks that carry no result. */
private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { e -> continuation.resumeWithException(e) }
    addOnCanceledListener { continuation.cancel() }
}
