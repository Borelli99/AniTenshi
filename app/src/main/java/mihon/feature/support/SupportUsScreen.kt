package mihon.feature.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.core.common.Constants
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.AniTenshiMark
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/** A donation page: the name shown in the app, its link and a generic icon. */
data class DonationLink(val name: String, val url: String, val icon: ImageVector)

/**
 * AniTenshi's donation pages.
 * While the list is empty the "Support AniTenshi" entry stays hidden in More.
 */
object AniTenshiDonations {
    val links: List<DonationLink> = listOf(
        DonationLink("PayPal", "https://paypal.me/Borelli99", Icons.Outlined.Payment),
        DonationLink("Buy Me a Coffee", "https://buymeacoffee.com/anitenshi", Icons.Outlined.LocalCafe),
    )
}

/**
 * AniTenshi: a short thank-you page with the donation links and a GitHub star.
 */
class SupportUsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(TLMR.strings.anitenshi_support_title),
                    navigateUp = navigator::pop,
                )
            },
        ) { paddingValues ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.padding.medium),
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                AniTenshiMark()
                Text(
                    text = stringResource(TLMR.strings.anitenshi_support_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                )

                AniTenshiDonations.links.forEach { link ->
                    SupportItem(
                        icon = link.icon,
                        title = link.name,
                        onClick = { uriHandler.openUri(link.url) },
                    )
                }

                SupportItem(
                    icon = Icons.Outlined.StarBorder,
                    title = stringResource(TLMR.strings.anitenshi_support_star),
                    onClick = { uriHandler.openUri(Constants.URL_GITHUB) },
                )

                Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
            }
        }
    }

    @Composable
    private fun SupportItem(
        icon: ImageVector,
        title: String,
        onClick: () -> Unit,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            TextPreferenceWidget(
                title = title,
                icon = icon,
                widget = {
                    Icon(
                        imageVector = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                onPreferenceClick = onClick,
            )
        }
    }
}
