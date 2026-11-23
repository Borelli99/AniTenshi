package eu.kanade.presentation.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DataSaverOff
import androidx.compose.material.icons.outlined.DataSaverOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AniTenshiSheet
import eu.kanade.presentation.components.AniTenshiSheetRow
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.PushPin
import mihon.icons.materialsymbols.roundedfilled.PushPin
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: what a long press on a source opens, anime or manga: a panel from the bottom with its icon and name,
 * "Browse" and "Latest", then pin or unpin, the data saver (when it is on) and disable, each with a line on what
 * follows.
 *
 * @param isAnime whether the line under the name says Anime or Manga.
 * @param onClickLatest null when the source has no latest titles.
 * @param onClickDisable null for the local source, which can't be disabled.
 * @param isExcludedFromDataSaver null when the data saver is off (no row for it).
 */
@Composable
fun SourceOptionsSheet(
    icon: @Composable () -> Unit,
    name: String,
    lang: String,
    isAnime: Boolean,
    isPinned: Boolean,
    onClickBrowse: () -> Unit,
    onClickLatest: (() -> Unit)?,
    onClickPin: () -> Unit,
    onClickDisable: (() -> Unit)?,
    isExcludedFromDataSaver: Boolean?,
    onClickToggleDataSaver: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val details = listOfNotNull(
        stringResource(if (isAnime) AYMR.strings.label_anime else AYMR.strings.manga),
        LocaleHelper.getSourceDisplayName(lang, context).takeIf { it.isNotBlank() },
        stringResource(TLMR.strings.anitenshi_source_pinned).takeIf { isPinned },
    ).joinToString(" · ")

    AniTenshiSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onClickBrowse,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.secondary,
                    contentColor = colors.onSecondary,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(MR.strings.browse),
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (onClickLatest != null) {
                OutlinedButton(
                    onClick = onClickLatest,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, colors.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.secondary),
                ) {
                    Text(
                        text = stringResource(MR.strings.latest),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (isPinned) {
            AniTenshiSheetRow(
                icon = MaterialSymbols.RoundedFilled.PushPin,
                label = stringResource(TLMR.strings.anitenshi_source_unpin),
                hint = stringResource(TLMR.strings.anitenshi_source_unpin_hint),
                onClick = onClickPin,
            )
        } else {
            AniTenshiSheetRow(
                icon = MaterialSymbols.Rounded.PushPin,
                label = stringResource(TLMR.strings.anitenshi_source_pin),
                hint = stringResource(TLMR.strings.anitenshi_source_pin_hint),
                onClick = onClickPin,
            )
        }
        if (isExcludedFromDataSaver != null) {
            AniTenshiSheetRow(
                icon = if (isExcludedFromDataSaver) Icons.Outlined.DataSaverOn else Icons.Outlined.DataSaverOff,
                label = if (isExcludedFromDataSaver) {
                    stringResource(AYMR.strings.data_saver_stop_exclude)
                } else {
                    stringResource(AYMR.strings.data_saver_exclude)
                },
                hint = if (isExcludedFromDataSaver) {
                    stringResource(TLMR.strings.anitenshi_source_saver_include_hint)
                } else {
                    stringResource(TLMR.strings.anitenshi_source_saver_exclude_hint)
                },
                onClick = onClickToggleDataSaver,
            )
        }
        if (onClickDisable != null) {
            AniTenshiSheetRow(
                icon = Icons.Outlined.Block,
                label = stringResource(MR.strings.action_disable),
                hint = stringResource(TLMR.strings.anitenshi_source_disable_hint),
                onClick = onClickDisable,
                isDestructive = true,
            )
        }
    }
}
