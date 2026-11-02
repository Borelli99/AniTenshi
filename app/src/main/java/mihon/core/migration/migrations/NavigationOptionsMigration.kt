package mihon.core.migration.migrations

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.model.StartScreen
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

@Inject
@ContributesIntoSet(AppScope::class)
class NavigationOptionsMigration(
    private val context: Context,
    private val preferenceStore: PreferenceStore,
) : Migration {
    override val version = 120f

    // Bring back navigation options
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val isDefaultTabManga = preferenceStore.getBoolean("default_home_tab_library", false)
        prefs.edit {
            remove("bottom_nav_style")
            remove("default_home_tab_library")

            // AniTenshi has a fixed bottom bar (Home, Anime, Manga): the old bottom bar style is dropped.
            val startScreen = if (isDefaultTabManga.get()) StartScreen.MANGA else StartScreen.ANIME
            preferenceStore.getEnum("start_screen", StartScreen.ANIME).set(startScreen)
        }

        return true
    }
}
