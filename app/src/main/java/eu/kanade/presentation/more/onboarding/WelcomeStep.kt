package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import mihon.app.di.appGraph
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.AniTenshiMark
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** AniTenshi: first step, the logo, a welcome and light / dark / system. */
internal class WelcomeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }
        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        Column {
            OnboardingHeader(
                title = stringResource(TLMR.strings.anitenshi_onboarding_welcome_title),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_welcome_body),
            ) {
                AniTenshiMark(size = 132.dp, letterColor = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = stringResource(TLMR.strings.anitenshi_onboarding_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            )
            AppThemeModePreferenceWidget(
                value = themeMode,
                onItemClick = {
                    themeModePref.set(it)
                    setAppCompatDelegateThemeMode(it)
                },
            )
        }
    }
}
