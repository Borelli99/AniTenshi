package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AniTenshiPalette

/** AniTenshi: the dots at the top of the first-launch screens (the current one is a gold dash). */
@Composable
internal fun OnboardingDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (index == current) 22.dp else 6.dp)
                    .background(
                        if (index <= current) AniTenshiPalette.Gold else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape,
                    ),
            )
        }
    }
}

/** Picture, title and short text at the top of a first-launch step. */
@Composable
internal fun OnboardingHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        art()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A gold icon in a round night blue tile, for the steps that have no logo. */
@Composable
internal fun OnboardingIcon(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(42.dp),
        )
    }
}
