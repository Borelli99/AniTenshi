package eu.kanade.presentation.entries.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.entries.MissingExtensionState
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication

/** Key of the missing-extension card in the entry screens' lists. */
const val ENTRY_MISSING_EXTENSION_KEY = "anitenshi-missing-extension"

/**
 * AniTenshi: the extension an anime or manga comes from isn't installed, and what can be done about it from its
 * screen.
 */
class MissingExtensionUi(
    val state: MissingExtensionState,
    /** Opens the extension list, already searching for the extension. */
    val onSearch: () -> Unit,
    /** Installs the extension found in the person's repos. */
    val onInstall: () -> Unit,
    /** Opens the list of repos, to add the one that has the extension. */
    val onOpenRepos: () -> Unit,
) {
    /** What the one button does: install when that can be done from here, else where to go next. */
    val onAction: () -> Unit
        get() = when {
            state.canInstall -> onInstall
            state.notInRepos -> onOpenRepos
            else -> onSearch
        }
}

/**
 * Null when the extension of the anime or manga on screen is installed. Set by the entry screens, so that the
 * header and the list don't each need it passed down.
 */
val LocalMissingExtension = compositionLocalOf<MissingExtensionUi?> { null }

/**
 * The card under the actions of an anime or manga whose extension isn't installed: what is missing, and one gold
 * button to get it.
 */
@Composable
fun MissingExtensionCard(
    ui: MissingExtensionUi,
    isAnime: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val state = ui.state

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(TLMR.strings.anitenshi_extension_missing_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = missingExtensionMessage(state, isAnime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    if (state.installFailed) {
                        Text(
                            text = stringResource(TLMR.strings.anitenshi_extension_install_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                        )
                    }
                }
            }

            Button(
                onClick = ui.onAction,
                enabled = !state.isInstalling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.secondary,
                    contentColor = colors.onSecondary,
                    // Still gold while it installs, only fainter
                    disabledContainerColor = colors.secondary.copy(alpha = 0.5f),
                    disabledContentColor = colors.onSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (state.isInstalling) {
                    CircularProgressIndicator(
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = missingExtensionActionIcon(state),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = missingExtensionActionLabel(state),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Shown instead of opening a chapter or an episode that can't be opened without the extension: the same message
 * and the same action as [MissingExtensionCard].
 */
@Composable
fun MissingExtensionDialog(
    ui: MissingExtensionUi,
    isAnime: Boolean,
    onDismissRequest: () -> Unit,
) {
    val state = ui.state
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        title = { Text(text = stringResource(TLMR.strings.anitenshi_extension_missing_title)) },
        text = { Text(text = missingExtensionMessage(state, isAnime)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    ui.onAction()
                },
                enabled = !state.isInstalling,
            ) {
                Text(text = missingExtensionActionLabel(state))
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
 * The source's line in the header of a title whose extension isn't installed: a warning triangle and the source's
 * name in the error colour, where the bare id of the source used to be. A tap looks for the extension in the
 * extension list.
 *
 * [fallbackName] and [onFallbackClick] are for when nothing provides [LocalMissingExtension].
 */
@Composable
fun MissingExtensionSourceRow(
    fallbackName: String,
    onFallbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = LocalMissingExtension.current
    val name = ui?.state?.sourceName?.takeIf { it.isNotBlank() }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: stringResource(TLMR.strings.anitenshi_extension_missing_short)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickableNoIndication(onClick = ui?.onSearch ?: onFallbackClick),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = stringResource(TLMR.strings.anitenshi_extension_missing_short),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun missingExtensionMessage(state: MissingExtensionState, isAnime: Boolean): String {
    val name = state.displayName
    val what = when {
        name.isBlank() && isAnime -> stringResource(TLMR.strings.anitenshi_extension_missing_anime_unknown)
        name.isBlank() -> stringResource(TLMR.strings.anitenshi_extension_missing_manga_unknown)
        isAnime -> stringResource(TLMR.strings.anitenshi_extension_missing_anime, name)
        else -> stringResource(TLMR.strings.anitenshi_extension_missing_manga, name)
    }
    val more = when {
        state.installedNotLoaded -> stringResource(TLMR.strings.anitenshi_extension_not_loaded)
        state.canInstall -> null
        state.notInRepos -> stringResource(TLMR.strings.anitenshi_extension_not_in_repos)
        else -> null
    }
    return if (more == null) what else "$what $more"
}

@Composable
private fun missingExtensionActionLabel(state: MissingExtensionState): String {
    return when {
        state.isInstalling -> stringResource(TLMR.strings.anitenshi_extension_installing)
        state.canInstall -> stringResource(TLMR.strings.anitenshi_extension_install, state.displayName)
        state.notInRepos -> stringResource(TLMR.strings.anitenshi_extension_open_repos)
        else -> stringResource(TLMR.strings.anitenshi_extension_search)
    }
}

private fun missingExtensionActionIcon(state: MissingExtensionState): ImageVector {
    return when {
        state.canInstall -> Icons.Outlined.Download
        state.notInRepos -> Icons.Outlined.Layers
        else -> Icons.Outlined.Search
    }
}
