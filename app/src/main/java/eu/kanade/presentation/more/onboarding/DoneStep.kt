package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.AniTenshiMark
import tachiyomi.presentation.core.i18n.stringResource

/** AniTenshi: last step, how to move around with the three round buttons (the backup moved to [SyncStep]). */
internal class DoneStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OnboardingHeader(
                title = stringResource(TLMR.strings.anitenshi_onboarding_done_title),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_done_body),
            ) {
                AniTenshiMark(size = 120.dp, letterColor = MaterialTheme.colorScheme.onBackground)
            }
            NavRow(
                title = stringResource(TLMR.strings.anitenshi_onboarding_nav_anime_title),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_nav_anime_body),
                diameter = 46.dp,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_anime),
                    contentDescription = null,
                    tint = AniTenshiPalette.Lavender,
                    modifier = Modifier.size(22.dp),
                )
            }
            NavRow(
                title = stringResource(MR.strings.label_home),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_nav_home_body),
                diameter = 56.dp,
                selected = true,
            ) {
                AniTenshiMark(size = 34.dp, letterColor = AniTenshiPalette.Ivory)
            }
            NavRow(
                title = stringResource(TLMR.strings.anitenshi_onboarding_nav_manga_title),
                subtitle = stringResource(TLMR.strings.anitenshi_onboarding_nav_manga_body),
                diameter = 46.dp,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_manga),
                    contentDescription = null,
                    tint = AniTenshiPalette.Lavender,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    diameter: Dp,
    selected: Boolean = false,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = AniTenshiPalette.Navy,
                border = BorderStroke(1.dp, AniTenshiPalette.NavyLine),
                modifier = Modifier
                    .size(diameter)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, AniTenshiPalette.Gold, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
