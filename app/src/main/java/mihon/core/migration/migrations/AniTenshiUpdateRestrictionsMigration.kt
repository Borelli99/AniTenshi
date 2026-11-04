package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * AniTenshi 1.0.16: library updates skip only completed series by default. Who still has exactly the four old
 * default "skip" rules saved (usually from pressing OK in the dialog) gets the new default once; any other choice
 * is kept.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AniTenshiUpdateRestrictionsMigration(
    private val libraryPreferences: LibraryPreferences,
) : Migration {
    override val version: Float = 160f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        libraryPreferences.replaceOldDefaultUpdateRestrictions()
        return@withIOContext true
    }
}
