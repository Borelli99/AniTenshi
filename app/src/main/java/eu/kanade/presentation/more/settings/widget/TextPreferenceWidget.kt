package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Preview
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    // AniTenshi: true when the subtitle is what the setting is set to now, not a description of it.
    // It is then written in the accent color, so the choice made stands out from the explanations.
    subtitleIsValue: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onPreferenceClick: (() -> Unit)? = null,
) {
    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        subcomponent = if (!subtitle.isNullOrBlank()) {
            {
                if (subtitleIsValue) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 10,
                    )
                } else {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .padding(horizontal = PrefsHorizontalPadding)
                            .secondaryItemAlpha(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                    )
                }
            }
        } else {
            null
        },
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    tint = iconTint,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        onClick = onPreferenceClick,
        widget = widget,
        enabled = enabled,
    )
}

/**
 * AniTenshi: the two small arrows at the end of a setting that opens a list of choices.
 */
@Composable
internal fun ChoiceIndicator() {
    Icon(
        imageVector = Icons.Outlined.UnfoldMore,
        contentDescription = null,
        modifier = Modifier
            .padding(start = TrailingWidgetBuffer)
            .size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun TextPreferenceWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                TextPreferenceWidget(
                    title = "Text preference with icon",
                    subtitle = "Text preference summary",
                    icon = MaterialSymbols.Rounded.Preview,
                    onPreferenceClick = {},
                )
                TextPreferenceWidget(
                    title = "Text preference",
                    subtitle = "Text preference summary",
                    onPreferenceClick = {},
                )
            }
        }
    }
}
