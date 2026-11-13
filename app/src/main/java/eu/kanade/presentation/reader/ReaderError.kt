package eu.kanade.presentation.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: what can be done about a page that failed, shown in a card under the message: [title] and [body], and
 * a button when [actionLabel] and [onAction] are given.
 */
@Immutable
class ReaderErrorSuggestion(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/**
 * A page that failed to load or decode: the message, Retry, and Open in WebView when [onOpenInWebView] is given.
 *
 * AniTenshi: [details] (the reason, what the image is) go under the message in smaller type, and a [suggestion]
 * goes in a card below them. When the card has a button, that is the one filled button and the other two are only
 * outlined.
 *
 * [onButtonPressChange] tells a viewer that reads every touch for its own taps when a button is held.
 */
@Composable
fun ReaderError(
    details: String?,
    onRetry: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onButtonPressChange: ((pressed: Boolean) -> Unit)? = null,
    suggestion: ReaderErrorSuggestion? = null,
) {
    val buttonModifier = if (onButtonPressChange != null) Modifier.reportPress(onButtonPressChange) else Modifier
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.widthIn(max = ReaderErrorMaxWidth),
        ) {
            Text(
                text = stringResource(MR.strings.decode_image_error),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (!details.isNullOrBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (suggestion != null) {
            SuggestionCard(suggestion = suggestion, buttonModifier = buttonModifier)
        }

        if (suggestion?.onAction != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                val outlinedColors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(
                    onClick = onRetry,
                    colors = outlinedColors,
                    modifier = buttonModifier.heightIn(min = 44.dp),
                ) {
                    Text(stringResource(MR.strings.action_retry))
                }
                if (onOpenInWebView != null) {
                    OutlinedButton(
                        onClick = onOpenInWebView,
                        colors = outlinedColors,
                        modifier = buttonModifier.heightIn(min = 44.dp),
                    ) {
                        Text(stringResource(MR.strings.action_open_in_web_view))
                    }
                }
            }
        } else {
            Button(onClick = onRetry, modifier = buttonModifier) {
                Text(stringResource(MR.strings.action_retry))
            }
            if (onOpenInWebView != null) {
                Button(onClick = onOpenInWebView, modifier = buttonModifier) {
                    Text(stringResource(MR.strings.action_open_in_web_view))
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: ReaderErrorSuggestion,
    buttonModifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.6f)),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .widthIn(max = SuggestionMaxWidth)
            .fillMaxWidth(),
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
                            imageVector = suggestion.icon,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = suggestion.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (suggestion.actionLabel != null && suggestion.onAction != null) {
                Button(
                    onClick = suggestion.onAction,
                    modifier = buttonModifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = suggestion.actionLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val ReaderErrorMaxWidth = 560.dp
private val SuggestionMaxWidth = 440.dp

internal fun Modifier.reportPress(onPressChange: (Boolean) -> Unit) = pointerInput(onPressChange) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onPressChange(true)
        waitForUpOrCancellation(pass = PointerEventPass.Initial)
        onPressChange(false)
    }
}
