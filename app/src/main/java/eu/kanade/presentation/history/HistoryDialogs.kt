package eu.kanade.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.random.Random

@Composable
fun HistoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: (Boolean) -> Unit,
    isManga: Boolean,
) {
    var removeEverything by remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.action_remove))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                val subtitle = if (isManga) {
                    MR.strings.dialog_with_checkbox_remove_description
                } else {
                    AYMR.strings.dialog_with_checkbox_remove_description_anime
                }
                Text(text = stringResource(subtitle))

                LabeledCheckbox(
                    label = if (isManga) {
                        stringResource(AYMR.strings.dialog_with_checkbox_reset)
                    } else {
                        stringResource(AYMR.strings.dialog_with_checkbox_reset_anime)
                    },
                    checked = removeEverything,
                    onCheckedChange = { removeEverything = it },
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete(removeEverything)
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

/**
 * AniTenshi: says what clearing the history takes away, the statistics included: their "Last 7 days" chart comes
 * from the history, and for manga the reading time too.
 */
@Composable
fun HistoryDeleteAllDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    isManga: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        title = {
            Text(text = stringResource(TLMR.strings.anitenshi_history_clear_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isManga) {
                        stringResource(TLMR.strings.anitenshi_history_clear_manga)
                    } else {
                        stringResource(TLMR.strings.anitenshi_history_clear_anime)
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceContainer)
                        .border(1.dp, colors.secondary, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.secondary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(TLMR.strings.anitenshi_history_clear_stats_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = if (isManga) {
                                stringResource(TLMR.strings.anitenshi_history_clear_stats_manga)
                            } else {
                                stringResource(TLMR.strings.anitenshi_history_clear_stats_anime)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(
                    text = stringResource(TLMR.strings.anitenshi_history_clear_confirm),
                    color = colors.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun HistoryDeleteDialogPreview() {
    TachiyomiPreviewTheme {
        HistoryDeleteDialog(
            onDismissRequest = {},
            onDelete = {},
            isManga = Random.nextBoolean(),
        )
    }
}
