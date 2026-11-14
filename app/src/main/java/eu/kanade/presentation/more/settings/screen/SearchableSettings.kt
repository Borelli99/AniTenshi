package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.isTabletUi

interface SearchableSettings : Screen {

    @Composable
    @ReadOnlyComposable
    fun getTitleRes(): StringResource

    @Composable
    fun getPreferences(): List<Preference>

    @Composable
    fun RowScope.AppBarAction() {
    }

    @Composable
    override fun Content() {
        val handleBack = LocalBackPress.current
        // AniTenshi: the search is one tap away on every page. Not on tablets, where the search bar of the
        // list of the pages is always on screen beside this page.
        val navigator = LocalNavigator.current
        val onSearchClick: (() -> Unit)? = if (navigator != null && !isTabletUi()) {
            { navigator.push(SettingsSearchScreen()) }
        } else {
            null
        }
        PreferenceScaffold(
            titleRes = getTitleRes(),
            onBackPressed = if (handleBack != null) handleBack::invoke else null,
            onSearchClick = onSearchClick,
            actions = { AppBarAction() },
            itemsProvider = { getPreferences() },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
