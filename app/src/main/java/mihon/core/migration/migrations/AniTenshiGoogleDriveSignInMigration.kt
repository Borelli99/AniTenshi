package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.sync.SyncPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

/**
 * AniTenshi 1.0.17: Google Drive is reached through Google Play services instead of the browser sign-in, which
 * kept a long-lived token on the device. Who was connected stays connected (the first sync asks Play services for
 * access, and tells them to connect again only if Google wants to show its screen); the old tokens are deleted.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AniTenshiGoogleDriveSignInMigration(
    private val syncPreferences: SyncPreferences,
) : Migration {
    override val version: Float = 161f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        if (syncPreferences.googleDriveRefreshToken().get().isNotBlank()) {
            syncPreferences.googleDriveConnected().set(true)
        }
        syncPreferences.googleDriveAccessToken().delete()
        syncPreferences.googleDriveRefreshToken().delete()
        return@withIOContext true
    }
}
