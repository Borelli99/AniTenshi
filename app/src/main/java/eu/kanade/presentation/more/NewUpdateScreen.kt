package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.more.NewUpdateScreenModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.NewReleases
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.InfoScreen

@Composable
fun NewUpdateScreen(
    versionName: String,
    isBeta: Boolean,
    changelogInfo: String,
    stage: NewUpdateScreenModel.Stage,
    downloadProgress: () -> Int,
    onOpenInBrowser: () -> Unit,
    onAcceptUpdate: () -> Unit,
    onRejectUpdate: () -> Unit,
) {
    InfoScreen(
        icon = MaterialSymbols.Rounded.NewReleases,
        headingText = stringResource(MR.strings.update_check_notification_update_available),
        subtitleText = versionName,
        acceptText = when (stage) {
            NewUpdateScreenModel.Stage.Available -> stringResource(MR.strings.update_check_confirm)

            NewUpdateScreenModel.Stage.Downloading -> stringResource(
                MR.strings.downloading_with_progress,
                downloadProgress(),
            )

            NewUpdateScreenModel.Stage.Downloaded -> stringResource(MR.strings.action_install)

            NewUpdateScreenModel.Stage.Failed -> stringResource(MR.strings.action_retry)
        },
        onAcceptClick = onAcceptUpdate,
        canAccept = stage != NewUpdateScreenModel.Stage.Downloading,
        rejectText = stringResource(MR.strings.action_not_now),
        onRejectClick = onRejectUpdate,
        subtitleBadge = { UpdateChannelBadge(isBeta = isBeta) },
    ) {
        RichText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.large),
            style = RichTextStyle(
                stringStyle = RichTextStringStyle(
                    linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
                ),
            ),
        ) {
            // Only the readable notes: everything after the first "---" line is the technical log
            Markdown(content = changelogInfo.split(UpdateNotesSeparator, limit = 2).first().trim())

            TextButton(
                onClick = onOpenInBrowser,
                modifier = Modifier.padding(top = MaterialTheme.padding.small),
            ) {
                Text(text = stringResource(MR.strings.update_check_open))
                Spacer(modifier = Modifier.width(MaterialTheme.padding.extraSmall))
                Icon(imageVector = MaterialSymbols.AutoMirroredRounded.OpenInNew, contentDescription = null)
            }
        }
    }
}

/** AniTenshi: says whether the update is a stable version (filled gold) or a beta (gold outline). */
@Composable
private fun UpdateChannelBadge(isBeta: Boolean) {
    val gold = MaterialTheme.colorScheme.primary
    val contentColor = if (isBeta) gold else MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .height(28.dp)
            .then(if (isBeta) Modifier.border(1.5.dp, gold, shape) else Modifier.background(gold, shape))
            .padding(start = 9.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isBeta) Icons.Outlined.Science else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(
                if (isBeta) TLMR.strings.anitenshi_update_beta else TLMR.strings.anitenshi_update_stable,
            ),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val UpdateNotesSeparator = Regex("\\r?\\n---\\r?\\n")

@PreviewLightDark
@Composable
private fun NewUpdateScreenPreview() {
    TachiyomiPreviewTheme {
        NewUpdateScreen(
            versionName = "v0.99.9",
            isBeta = false,
            changelogInfo = """
                ## Yay
                Foobar

                ### More info
                - Hello
                - World
            """.trimIndent(),
            stage = NewUpdateScreenModel.Stage.Available,
            downloadProgress = { 0 },
            onOpenInBrowser = {},
            onAcceptUpdate = {},
            onRejectUpdate = {},
        )
    }
}
