package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

/**
 * AniTenshi 1.0.1: installs that still use one of the old default themes (Default or Dynamic)
 * switch to the AniTenshi theme once. A theme picked on purpose (Nord, Tako, ...) is kept.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AniTenshiThemeMigration(
    private val uiPreferences: UiPreferences,
) : Migration {
    override val version = 145f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val appTheme = uiPreferences.appTheme
        if (appTheme.get() == AppTheme.DEFAULT || appTheme.get() == AppTheme.MONET) {
            appTheme.set(AppTheme.ANITENSHI)
        }
        return true
    }
}
